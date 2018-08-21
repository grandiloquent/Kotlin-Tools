package psycho.euphoria.file

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.provider.MediaStore
import org.jsoup.Jsoup
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.getFileNameWithoutExtension
import psycho.euphoria.common.extension.getFilenameExtension
import psycho.euphoria.common.extension.getFilenameFromPath
import psycho.euphoria.common.extension.getParentPath
import java.io.File
import java.nio.charset.Charset


fun gbk2utf(path: String) {
    val targetFile = File(path.getParentPath(), path.getFileNameWithoutExtension() + "-utf8" + path.getFilenameExtension())
    targetFile.writeText(File(path).readText(Charset.forName("gbk")), Charset.forName("utf8"))
}

fun combineSafari(dir: File) {
    val tocFile = File(dir, "目录.html")
    if (!tocFile.exists()) return
    var document = Jsoup.parse(tocFile.readText())
    val nodes = document.select("a")
    val strinBuilder = StringBuilder()
    strinBuilder.append("<!DOCTYPE html> <html lang=en> <head> <meta charset=utf-8> <meta content=\"IE=edge\" http-equiv=X-UA-Compatible> <meta content=\"width=device-width,initial-scale=1\" name=viewport><link href=\"style.css\" rel=\"stylesheet\"></head><body>");


    if (nodes.isNotEmpty()) {
        val hrefs = nodes.map { it.attr("href").split('#').first() }.distinct()
        for (h in hrefs) {
            var hf = File(dir, h)
            if (hf.exists()) {
                strinBuilder.append(Jsoup.parse(hf.readText()).body().html())
            }
        }
    }
    strinBuilder.append("</body></html>");

    File(dir, dir.name + ".html").writeText(strinBuilder.toString())
}

fun File.getApkIconPath(): Drawable? {
    return Services.packageManager.getPackageArchiveInfo(absolutePath, PackageManager.GET_ACTIVITIES)?.let {
        it.applicationInfo?.let {
            it.sourceDir = absolutePath
            it.publicSourceDir = absolutePath
            it.loadIcon(Services.packageManager)
        }
    }
}

fun String.getApkIconPath(): Drawable? {
    return Services.packageManager.getPackageArchiveInfo(this, PackageManager.GET_ACTIVITIES)?.let {
        it.applicationInfo?.let {
            it.sourceDir = this
            it.publicSourceDir = this
            it.loadIcon(Services.packageManager)
        }
    }
}

fun File.getVideoIcon(): Bitmap {
    return ThumbnailUtils.createVideoThumbnail(absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
}