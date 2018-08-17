package psycho.euphoria.file
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.android.synthetic.main.item_file.*
import psycho.euphoria.common.FileCache
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.isVideoFast
import psycho.euphoria.common.util.FutureListener
import psycho.euphoria.common.util.ThreadPool
import java.io.File
import java.io.FileOutputStream
class ThumbnailManager {
    private val mFileCache = FileCache(Services.context, Services.context.externalCacheDir, DATABASE, 1024 * 10000)
    private val mThreadPool = ThreadPool()
    constructor() {
        Log.e(TAG, "[constructor] ${Services.context.externalCacheDir}")
        //FileCache.deleteFiles(Services.context, Services.context.externalCacheDir, DATABASE)
    }
    fun into(path: String, view: FileAdapter.ViewHolder?, defaultDrawable: Drawable) {
        mThreadPool.submit(ThumbJob(path), FutureListener<Drawable?> {
            //Log.e(TAG, "[into] from threadPool")
            view?.item_icon?.drawble = it?.get() ?: defaultDrawable
        })
    }
    companion object {
        private const val TAG = "ThumbnailManager"
        private const val DATABASE = "thumbnail_cache.db"
        var instance: ThumbnailManager? = null
            get() = field ?: ThumbnailManager().also { instance = it }
    }
    private inner class ThumbJob(private val path: String) : ThreadPool.Job<Drawable?> {
        override fun run(jc: ThreadPool.JobContext?): Drawable? {
            val entry = mFileCache.lookup(path)
            if (entry != null) {
                if (entry.cacheFile != null) {
                    //Log.e(TAG, "[into] loading from cache")
                    return FastBitmapDrawable(BitmapFactory.decodeFile(entry.cacheFile.absolutePath))
                }
            }
            if (path.endsWith(".apk", true)) {
                return path.getApkIconPath()
            }
            if (path.isVideoFast()) {
                try {
                    val bitmap = File(path).getVideoIcon()
                    val outputFile = createTempFile("tmp", null, Services.context.externalCacheDir)
                    //Log.e(TAG, "[run] temporary file: ${outputFile.absolutePath}")
                    FileOutputStream(outputFile).use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    }
                    mFileCache.store(path, outputFile)
                    //Log.e(TAG, "[run] create jpeg")
                    return FastBitmapDrawable(bitmap)
                } catch (e: Exception) {
                    //Log.e(TAG, "[into]", e)
                }
            }
            return null
        }
    }
}