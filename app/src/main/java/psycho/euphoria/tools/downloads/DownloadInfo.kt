package psycho.euphoria.tools.downloads
import java.net.Proxy

data class DownloadInfo(
        var url: String,
        val fileName: String,
        var currentBytes: Long = 0L,
        val proxy: Proxy? = null,
        var etag: String? = null,
        var userAgent: String? = null,
        var mimeType: String? = null,
        var totalBytes: Long = 0L
) {
    fun getHeaders(): List<Pair<String, String>> {
        val headers = ArrayList<Pair<String, String>>()
        return headers
    }
}
