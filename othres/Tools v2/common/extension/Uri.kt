package psycho.euphoria.common.extension

import android.net.Uri
import android.text.TextUtils


fun Uri.isLocalFileUri(): Boolean {
    val scheme = getScheme()
    return TextUtils.isEmpty(scheme) || "file" == scheme
}