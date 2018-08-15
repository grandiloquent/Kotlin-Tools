package psycho.euphoria.player
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import psycho.euphoria.common.BlobCache
import java.io.IOException
object CacheManager {
    private val CACHE_MAP = HashMap<String, BlobCache>()
    private var OLD_CHECK_DONE = false
    private const val TAG = "CacheManager"
    private const val KEY_CACHE_UP_TO_DATE = "cache-up-to-date"
    fun removeOldFilesIfNecessary(context: Context) {
        val prefer = PreferenceManager.getDefaultSharedPreferences(context)
        var n = 0
        try {
            n = prefer.getInt(KEY_CACHE_UP_TO_DATE, 0)
        } catch (t: Throwable) {
        }
        if (n != 0) return
        prefer.edit().putInt(KEY_CACHE_UP_TO_DATE, 1).apply()
        val cacheDir = context.externalCacheDir
        val prefix = cacheDir.absolutePath
        BlobCache.deleteFiles("$prefix/imgcache")
        BlobCache.deleteFiles("$prefix/rev_geocoding")
        BlobCache.deleteFiles("$prefix/bookmark")
    }
    fun getCahce(context: Context, filename: String, maxEntries: Int, maxBytes: Int, version: Int): BlobCache? {
        synchronized(CACHE_MAP) {
            if (!OLD_CHECK_DONE) {
                removeOldFilesIfNecessary(context)
                OLD_CHECK_DONE = true
            }
            var cache = CACHE_MAP.get(filename)
            if (cache == null) {
                val cacheDir = context.externalCacheDir
                val path = "${cacheDir.absolutePath}/$filename"
                Log.e(TAG, "getCahce: path => $path")
                try {
                    cache = BlobCache(path, maxEntries, maxBytes, false, version)
                    CACHE_MAP.put(filename, cache)
                } catch (e: IOException) {
                    Log.e(TAG, "Cannot instantiate cache!", e)
                }
            }
            return cache
        }
    }
}