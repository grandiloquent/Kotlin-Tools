package psycho.euphoria.file

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.provider.MediaStore
import psycho.euphoria.common.Services
import java.io.File

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
            it.publicSourceDir =this
            it.loadIcon(Services.packageManager)
        }
    }


}

fun File.getVideoIcon(): Bitmap {
    return ThumbnailUtils.createVideoThumbnail(absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
}