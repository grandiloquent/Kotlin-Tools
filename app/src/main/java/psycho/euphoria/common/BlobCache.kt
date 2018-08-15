package psycho.euphoria.common
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.Adler32
class BlobCache {
    private val mAdler32 = Adler32()
    private val mBlobHeader = ByteArray(BLOB_HEADER_SIZE)
    private val mDataFile0: RandomAccessFile
    private val mDataFile1: RandomAccessFile
    private val mIndexFile: RandomAccessFile
    private val mIndexHeader = ByteArray(INDEX_HEADER_SIZE)
    private val mLookupRequest = LookupRequest()
    private val mVersion: Int
    private var mActiveBytes = 0
    private var mActiveDataFile: RandomAccessFile? = null
    private var mActiveEntries = 0
    private var mActiveHashStart = 0
    private var mActiveRegion = 0
    private var mFileOffset = 0
    private var mInactiveDataFile: RandomAccessFile? = null
    private var mInactiveHashStart = 0
    private var mIndexBuffer: MappedByteBuffer? = null
    private var mIndexChannel: FileChannel? = null
    private var mMaxBytes = 0
    private var mMaxEntries = 0
    private var mSlotOffset = 0
    constructor(path: String, maxEntries: Int, maxBytes: Int, reset: Boolean) : this(path, maxEntries, maxBytes, reset, 0)
    constructor(path: String, maxEntries: Int, maxBytes: Int, reset: Boolean, version: Int) {
        mIndexFile = RandomAccessFile("$path.idx", "rw")
        mDataFile0 = RandomAccessFile("$path.0", "rw")
        mDataFile1 = RandomAccessFile("$path.1", "rw")
        mVersion = version
        if (!reset && loadIndex()) {
            return
        }
        resetCache(maxEntries, maxBytes)
        if (!loadIndex()) {
            closeAll()
            throw IOException("unable to load index")
        }
    }
    fun checkSum(data: ByteArray): Int {
        mAdler32.reset()
        mAdler32.update(data)
        return mAdler32.value.toInt()
    }
    fun checkSum(data: ByteArray?, offset: Int, nbytes: Int): Int {
        mAdler32.reset()
        mAdler32.update(data, offset, nbytes)
        return mAdler32.value.toInt()
    }
    private fun clearHash(hashStart: Int) {
        val zero = ByteArray(1024)
        mIndexBuffer?.position(hashStart)
        var count = mMaxEntries * 12
        while (count > 0) {
            val todo = Math.min(count, 1024)
            mIndexBuffer?.put(zero, 0, todo)
            count -= todo
        }
    }
    fun close() {
        syncAll()
        closeAll()
    }
    private fun closeAll() {
        closeSilently(mIndexChannel as Closeable)
        closeSilently(mIndexFile)
        closeSilently(mDataFile0)
        closeSilently(mDataFile1)
    }
    private fun flipRegion() {
        mActiveRegion = 1 - mActiveRegion
        mActiveEntries = 0
        mActiveBytes = DATA_HEADER_SIZE
        writeInt(mIndexHeader, IH_ACTIVE_REGION, mActiveRegion)
        writeInt(mIndexHeader, IH_ACTIVE_ENTRIES, mActiveEntries)
        writeInt(mIndexHeader, IH_ACTIVE_BYTES, mActiveBytes)
        updateIndexHeader()
        setActiveVariables()
        clearHash(mActiveHashStart)
        syncIndex()
    }
    fun getActiveCount(): Int {
        var count = 0
        for (i in 0 until mMaxEntries) {
            val offset = mActiveHashStart + i * 12
            val candidateKey = mIndexBuffer?.getLong(offset)
            val candidateOffset = mIndexBuffer?.getInt(offset + 8)
            if (candidateOffset != 0) ++count
        }
        if (count == mActiveEntries) {
            return count
        } else {
            Log.e(TAG, "wrong active count: $mActiveEntries vs $count")
            return -1
        }
    }
    private fun getBlob(file: RandomAccessFile?, offset: Int,
                        req: LookupRequest): Boolean {
        if (file != null) {
            val header = mBlobHeader
            val oldPosition = file.filePointer
            try {
                file.seek(offset.toLong())
                if (file.read(header) != BLOB_HEADER_SIZE) {
                    Log.w(TAG, "cannot read blob header")
                    return false
                }
                val blobKey = readLong(header, BH_KEY)
                if (blobKey != req.key) {
                    Log.w(TAG, "blob key does not match: $blobKey")
                    return false
                }
                val sum = readInt(header, BH_CHECKSUM)
                val blobOffset = readInt(header, BH_OFFSET)
                if (blobOffset != offset) {
                    Log.w(TAG, "blob offset does not match: $blobOffset")
                    return false
                }
                val length = readInt(header, BH_LENGTH)
                if (length < 0 || length > mMaxBytes - offset - BLOB_HEADER_SIZE) {
                    Log.w(TAG, "invalid blob length: $length")
                    return false
                }
                if (req.buffer == null || req.buffer!!.size < length) {
                    req.buffer = ByteArray(length)
                }
                val blob = req.buffer
                req.length = length
                if (file.read(blob, 0, length) != length) {
                    Log.w(TAG, "cannot read blob data")
                    return false
                }
                if (checkSum(blob, 0, length) != sum) {
                    Log.w(TAG, "blob checksum does not match: $sum")
                    return false
                }
                return true
            } catch (t: Throwable) {
                Log.e(TAG, "getBlob failed.", t)
                return false
            } finally {
                file.seek(oldPosition)
            }
        }
        return false
    }
    fun insert(key: Long, data: ByteArray) {
        if (DATA_HEADER_SIZE + BLOB_HEADER_SIZE + data.size > mMaxBytes) {
            throw RuntimeException("blob is too large!")
        }
        if (mActiveBytes + BLOB_HEADER_SIZE + data.size > mMaxBytes || mActiveEntries * 2 >= mMaxEntries) {
            flipRegion()
        }
        if (!lookupInternal(key, mActiveHashStart)) {
            mActiveEntries++
            writeInt(mIndexHeader, IH_ACTIVE_ENTRIES, mActiveEntries)
        }
        insertInternal(key, data, data.size)
        updateIndexHeader()
    }
    private fun insertInternal(key: Long, data: ByteArray?, length: Int) {
        if (data != null) {
            val header = mBlobHeader
            val sum = checkSum(data)
            writeLong(header, BH_KEY, key)
            writeInt(header, BH_CHECKSUM, sum)
            writeInt(header, BH_OFFSET, mActiveBytes)
            writeInt(header, BH_LENGTH, length)
            mActiveDataFile?.apply {
                write(header)
                write(data, 0, length)
            }
            mIndexBuffer?.apply {
                putLong(mSlotOffset, key)
                putInt(mSlotOffset + 8, mActiveBytes)
            }
            mActiveBytes += BLOB_HEADER_SIZE + length
            writeInt(mIndexHeader, IH_ACTIVE_BYTES, mActiveBytes)
        }
    }
    private fun loadIndex(): Boolean {
        try {
            mIndexFile.seek(0)
            mDataFile0.seek(0)
            mDataFile1.seek(0)
            val buf = mIndexHeader
            if (mIndexFile.read(buf) != INDEX_HEADER_SIZE) {
                Log.w(TAG, "cannot read header")
                return false
            }
            if (readInt(buf, IH_MAGIC) != MAGIC_INDEX_FILE.toInt()) {
                Log.w(TAG, "cannot read header magic")
                return false
            }
            if (readInt(buf, IH_VERSION) != mVersion) {
                Log.w(TAG, "version mismatch")
                return false
            }
            mMaxEntries = readInt(buf, IH_MAX_ENTRIES)
            mMaxBytes = readInt(buf, IH_MAX_BYTES)
            mActiveRegion = readInt(buf, IH_ACTIVE_REGION)
            mActiveEntries = readInt(buf, IH_ACTIVE_ENTRIES)
            mActiveBytes = readInt(buf, IH_ACTIVE_BYTES)
            val sum = readInt(buf, IH_CHECKSUM)
            if (checkSum(buf, 0, IH_CHECKSUM) != sum) {
                Log.w(TAG, "header checksum does not match")
                return false
            }
            if (mMaxEntries <= 0) {
                Log.w(TAG, "invalid max entries")
                return false
            }
            if (mMaxBytes <= 0) {
                Log.w(TAG, "invalid max bytes")
                return false
            }
            if (mActiveRegion != 0 && mActiveRegion != 1) {
                Log.w(TAG, "invalid active region")
                return false
            }
            if (mActiveEntries < 0 || mActiveEntries > mMaxEntries) {
                Log.w(TAG, "invalid active entries")
                return false
            }
            if (mActiveBytes < DATA_HEADER_SIZE || mActiveBytes > mMaxBytes) {
                Log.w(TAG, "invalid active bytes")
                return false
            }
            if (mIndexFile.length() != (INDEX_HEADER_SIZE + mMaxEntries * 12 * 2).toLong()) {
                Log.w(TAG, "invalid index file length")
                return false
            }
            val magic = ByteArray(4)
            if (mDataFile0.read(magic) != 4) {
                Log.w(TAG, "cannot read data file magic")
                return false
            }
            if (readInt(magic, 0) != MAGIC_DATA_FILE.toInt()) {
                Log.w(TAG, "invalid data file magic")
                return false
            }
            if (mDataFile1.read(magic) != 4) {
                Log.w(TAG, "cannot read data file magic")
                return false
            }
            if (readInt(magic, 0) != MAGIC_DATA_FILE.toInt()) {
                Log.w(TAG, "invalid data file magic")
                return false
            }
            mIndexChannel = mIndexFile.channel
            mIndexBuffer = mIndexChannel?.map(FileChannel.MapMode.READ_WRITE,
                    0, mIndexFile.length())
            mIndexBuffer?.order(ByteOrder.LITTLE_ENDIAN)
            setActiveVariables()
            return true
        } catch (ex: IOException) {
            Log.e(TAG, "loadIndex failed.", ex)
            return false
        }
    }
    fun lookup(key: Long): ByteArray? {
        mLookupRequest.key = key
        mLookupRequest.buffer = null
        return if (lookup(mLookupRequest)) {
            mLookupRequest.buffer
        } else {
            null
        }
    }
    fun lookup(req: LookupRequest): Boolean {
        if (lookupInternal(req.key, mActiveHashStart)) {
            if (getBlob(mActiveDataFile, mFileOffset, req)) {
                return true
            }
        }
        val insertOffset = mSlotOffset
        if (lookupInternal(req.key, mInactiveHashStart)) {
            if (getBlob(mInactiveDataFile, mFileOffset, req)) {
                if (mActiveBytes + BLOB_HEADER_SIZE + req.length > mMaxBytes || mActiveEntries * 2 >= mMaxEntries) {
                    return true
                }
                mSlotOffset = insertOffset
                try {
                    insertInternal(req.key, req.buffer, req.length)
                    mActiveEntries++
                    writeInt(mIndexHeader, IH_ACTIVE_ENTRIES, mActiveEntries)
                    updateIndexHeader()
                } catch (t: Throwable) {
                    Log.e(TAG, "cannot copy over")
                }
                return true
            }
        }
        return false
    }
    private fun lookupInternal(key: Long, hashStart: Int): Boolean {
        var slot = (key % mMaxEntries).toInt()
        if (slot < 0) slot += mMaxEntries
        val slotBegin = slot
        while (true) {
            val offset = hashStart + slot * 12
            val candidateKey = mIndexBuffer?.getLong(offset)
            val candidateOffset = mIndexBuffer?.getInt(offset + 8)
            if (candidateOffset == 0) {
                mSlotOffset = offset
                return false
            } else if (candidateKey == key) {
                mSlotOffset = offset
                mFileOffset = candidateOffset ?: 0
                return true
            } else {
                if (++slot >= mMaxEntries) {
                    slot = 0
                }
                if (slot == slotBegin) {
                    Log.w(TAG, "corrupted index: clear the slot.")
                    mIndexBuffer?.putInt(hashStart + slot * 12 + 8, 0)
                }
            }
        }
    }
    private fun resetCache(maxEntries: Int, maxBytes: Int) {
        mIndexFile.setLength(0)
        mIndexFile.setLength((INDEX_HEADER_SIZE + maxEntries * 12 * 2).toLong())
        mIndexFile.seek(0)
        val buf = mIndexHeader
        writeInt(buf, IH_MAGIC, MAGIC_INDEX_FILE.toInt())
        writeInt(buf, IH_MAX_ENTRIES, maxEntries)
        writeInt(buf, IH_MAX_BYTES, maxBytes)
        writeInt(buf, IH_ACTIVE_REGION, 0)
        writeInt(buf, IH_ACTIVE_ENTRIES, 0)
        writeInt(buf, IH_ACTIVE_BYTES, DATA_HEADER_SIZE)
        writeInt(buf, IH_VERSION, mVersion)
        writeInt(buf, IH_CHECKSUM, checkSum(buf, 0, IH_CHECKSUM))
        mIndexFile.write(buf)
        mDataFile0.setLength(0)
        mDataFile1.setLength(0)
        mDataFile0.seek(0)
        mDataFile1.seek(0)
        writeInt(buf, 0, MAGIC_DATA_FILE.toInt())
        mDataFile0.write(buf, 0, 4)
        mDataFile1.write(buf, 0, 4)
    }
    private fun setActiveVariables() {
        mActiveDataFile = if (mActiveRegion == 0) mDataFile0 else mDataFile1
        mInactiveDataFile = if (mActiveRegion == 1) mDataFile0 else mDataFile1
        mActiveDataFile?.apply {
            val activeBytes = mActiveBytes.toLong()
            setLength(activeBytes)
            seek(activeBytes)
        }
        mActiveHashStart = INDEX_HEADER_SIZE
        mInactiveHashStart = INDEX_HEADER_SIZE
        if (mActiveRegion == 0) {
            mInactiveHashStart += mMaxEntries * 12
        } else {
            mActiveHashStart += mMaxEntries * 12
        }
    }
    fun syncAll() {
        syncIndex()
        try {
            mDataFile0.fd.sync()
        } catch (t: Throwable) {
            Log.w(TAG, "sync data file 0 failed", t)
        }
        try {
            mDataFile1.fd.sync()
        } catch (t: Throwable) {
            Log.w(TAG, "sync data file 1 failed", t)
        }
    }
    fun syncIndex() {
        try {
            mIndexBuffer?.force()
        } catch (t: Throwable) {
            Log.w(TAG, "sync index failed", t)
        }
    }
    private fun updateIndexHeader() {
        writeInt(mIndexHeader, IH_CHECKSUM,
                checkSum(mIndexHeader, 0, IH_CHECKSUM))
        mIndexBuffer?.position(0)
        mIndexBuffer?.put(mIndexHeader)
    }
    companion object {
        private const val BH_CHECKSUM = 8
        private const val BH_KEY = 0
        private const val BH_LENGTH = 16
        private const val BH_OFFSET = 12
        private const val BLOB_HEADER_SIZE = 20
        private const val DATA_HEADER_SIZE = 4
        private const val IH_ACTIVE_BYTES = 20
        private const val IH_ACTIVE_ENTRIES = 16
        private const val IH_ACTIVE_REGION = 12
        private const val IH_CHECKSUM = 28
        private const val IH_MAGIC = 0
        private const val IH_MAX_BYTES = 8
        private const val IH_MAX_ENTRIES = 4
        private const val IH_VERSION = 24
        private const val INDEX_HEADER_SIZE = 32
        private const val MAGIC_DATA_FILE = 0xBD248510
        private const val MAGIC_INDEX_FILE = 0xB3273030
        private const val TAG = "BlobCache"
        fun readInt(buf: ByteArray, offset: Int): Int {
            return (buf[offset].toInt() and 0xff
                    or (buf[offset + 1].toInt() and 0xff shl 8)
                    or (buf[offset + 2].toInt() and 0xff shl 16)
                    or (buf[offset + 3].toInt() and 0xff shl 24))
        }
        fun readLong(buf: ByteArray, offset: Int): Long {
            var result = (buf[offset + 7].toInt() and 0xff).toLong()
            for (i in 6 downTo 0) {
                result = result shl 8 or ((buf[offset + i].toInt() and 0xff).toLong())
            }
            return result
        }
        fun writeInt(buf: ByteArray, offset: Int, value: Int) {
            var value = value
            for (i in 0..3) {
                buf[offset + i] = (value and 0xff).toByte()
                value = value shr 8
            }
        }
        fun writeLong(buf: ByteArray, offset: Int, value: Long) {
            var value = value
            for (i in 0..7) {
                buf[offset + i] = (value and 0xff).toByte()
                value = value shr 8
            }
        }
        fun deleteFiles(path: String) {
            deleteFileSilently("$path.idx")
            deleteFileSilently("$path.0")
            deleteFileSilently("$path.1")
        }
        private fun deleteFileSilently(path: String) {
            try {
                File(path).delete()
            } catch (t: Throwable) {
            }
        }
        fun closeSilently(c: Closeable?) {
            try {
                c?.let {
                    it.close()
                }
            } catch (t: Throwable) {
            }
        }
    }
    class LookupRequest {
        var key: Long = 0
        var buffer: ByteArray? = null
        var length: Int = 0
    }
}