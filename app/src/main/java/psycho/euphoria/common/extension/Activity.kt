package psycho.euphoria.common.extension

import android.Manifest.permission
import android.content.pm.PackageManager
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.view.View


fun Activity.maybeRequestReadExternalStoragePermission(vararg uris: Uri): Boolean {
    if (Build.VERSION.SDK_INT < 23) {
        return false
    }
    for (uri in uris) {
        if (uri.isLocalFileUri()) {
            if (checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission.READ_EXTERNAL_STORAGE), 0)
                return true
            }
            break
        }
    }
    return false
}

fun Activity.hideSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        actionBar?.hide()
    }
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE
}

fun Activity.showSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        actionBar?.show()
    }
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}
