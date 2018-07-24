package psycho.euphoria.tools.commons

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import psycho.euphoria.tools.BuildConfig
import psycho.euphoria.tools.R
import java.io.File

fun Activity.requestFullScreen() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
}

fun Activity.requestKeepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.openPath(path: String, forceChooser: Boolean, openAsText: Boolean = false) {
    val mimeType = if (openAsText) "text/plain" else ""
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, mimeType)
}

fun Activity.openPathIntent(path: String, forceChooser: Boolean, applicationId: String, forceMimeType: String = "") {
    Thread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@Thread
        val mimeType = if (forceMimeType.isNotEmpty()) forceMimeType else getUriMimeType(path, newUri)
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(newUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)



            putExtra(REAL_FILE_PATH, path)

            if (resolveActivity(packageManager) != null) {
                val chooser = Intent.createChooser(this, getString(R.string.open_with))
                try {
                    startActivity(if (forceChooser) chooser else this)
                } catch (e: NullPointerException) {
                    showErrorToast(e)
                }
            } else {
                if (!tryGenericMimeType(this, mimeType, newUri)) {
                    toast(R.string.no_app_found)
                }
            }
        }
    }.start()
}
fun Activity.tryGenericMimeType(intent: Intent, mimeType: String, uri: Uri): Boolean {
    var genericMimeType = mimeType.getGenericMimeType()
    if (genericMimeType.isEmpty()) {
        genericMimeType = "*/*"
    }

    intent.setDataAndType(uri, genericMimeType)
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}
fun Activity.tryOpenPathIntent(path: String, forceChooser: Boolean, openAsText: Boolean = false) {
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = if (isNougatPlus()) {
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", File(path))
        } else {
            Uri.fromFile(File(path))
        }

        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    } else {
        openPath(path, forceChooser, openAsText)
    }
}
fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}
fun AppCompatActivity.hideSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.hide()
    }
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE
}


fun AppCompatActivity.showSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.show()
    }
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun Activity.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Activity.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}