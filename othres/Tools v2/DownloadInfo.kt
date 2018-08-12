package psycho.euphoria.tools.downloads

data class DownloadInfo(
        val id: Long,
        var uri: String,
        val fileName: String,
        var etag: String?,
        var currentBytes: Long,
        var totalBytes: Long,
        var failedCount: Int,
        var finish: Int) {
    override fun toString(): String {
        return "uri => ${uri} \nfileName => ${fileName} \netag => ${etag} \ncurrentBytes => ${currentBytes} \ntotalBytes => ${totalBytes} \nfailedCount => ${failedCount} \nfinish => ${finish} \n"
    }

    fun writeDatabase() {
        DownloadTaskProvider.getInstance().update(this)
    }
}