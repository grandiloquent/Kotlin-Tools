package psycho.euphoria.common.extension

import android.Manifest.permission
import android.content.pm.PackageManager
import android.app.Activity
import android.net.Uri
import android.os.Build


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