package psycho.euphoria.tools.downloads

import psycho.euphoria.tools.commons.App
import java.net.Proxy

data class DownloadInfo(
        var id: Long,
        var url: String,
        var fileName: String,
        var currentBytes: Long = 0L,
        val proxy: Proxy? = null,
        var etag: String? = null,
        var userAgent: String? = null,
        var mimeType: String? = null,
        var totalBytes: Long = 0L,
        var finish: Boolean = false
) {
    fun getHeaders(): List<Pair<String, String>> {
        val headers = ArrayList<Pair<String, String>>()
        return headers
    }

    fun maybeDownloaded(): Boolean {
        return totalBytes == -1L || (totalBytes == currentBytes)
    }

    fun writeToDatabase() {
        DownloadDatabase.getInstance(App.instance).update(this)
    }
}
