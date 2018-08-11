package psycho.euphoria.player

import android.content.Context
import android.util.Log
import psycho.euphoria.tools.CacheManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream


class Bookmarker(private val context: Context) {

    fun setBookmark(path: String, bookmark: Long) {
        try {
            val cache = CacheManager.getCahce(context,
                    BOOKMARK_CACHE_FILE,
                    BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES,
                    BOOKMARK_CACHE_VERSION)
            val bos = ByteArrayOutputStream()
            DataOutputStream(bos).apply {
                writeUTF(path)
                writeLong(bookmark)
                //writeInt(duration) , duration: Int
                flush()
                cache?.insert(path.hashCode().toLong(), bos.toByteArray())
            }


        } catch (t: Throwable) {
            Log.e(TAG, "setBookmark failed", t)
        }
    }

    fun getBookmark(path: String): Long? {
        try {
            val cache = CacheManager.getCahce(context,
                    BOOKMARK_CACHE_FILE,
                    BOOKMARK_CACHE_MAX_ENTRIES,
                    BOOKMARK_CACHE_MAX_BYTES,
                    BOOKMARK_CACHE_VERSION)
            val data = cache?.lookup(path.hashCode().toLong()) ?: return null
            val dis = DataInputStream(ByteArrayInputStream(data))
            val uriString = DataInputStream.readUTF(dis)
            val bookmark = dis.readLong()
            //val duration = dis.readInt()
            if (!uriString.equals(path)) {
                return null
            }
//        if (bookmark < HALF_MINUTE || duration < TWO_MINUTES
//                || bookmark > duration - HALF_MINUTE) {
//            return null
//        }
            return bookmark

        } catch (t: Throwable) {
            Log.e(TAG, "getBookmark failed", t)
        }
        return null
    }

    companion object {
        private const val BOOKMARK_CACHE_FILE = "bookmark"
        private const val BOOKMARK_CACHE_MAX_BYTES = 10 * 1024
        private const val BOOKMARK_CACHE_MAX_ENTRIES = 100
        private const val BOOKMARK_CACHE_VERSION = 1
        private const val HALF_MINUTE = 30 * 1000
        private const val TAG = "Bookmarker"
        private const val TWO_MINUTES = 4 * HALF_MINUTE
    }
}