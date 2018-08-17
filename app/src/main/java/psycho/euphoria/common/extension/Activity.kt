package psycho.euphoria.common.extension

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.abc_tooltip.*
import kotlinx.android.synthetic.main.dialog_title.view.*
import psycho.euphoria.common.*
import psycho.euphoria.tools.BuildConfig
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.CustomTextView
import java.io.File

private const val STRING_NO_APP_FOUND = "No valid app found"
private const val STRING_OPEN_WITH = "Open with"
private const val STRING_AN_ERROR_OCCURRED = "An error occurred: %s"
private const val STRING_MAXIMUM_SHARE_REACHED = "You cannot share this much content at once"
private const val STRING_SHARE_VIA = "Share via"
private const val STRING_UNKNOWN_ERROR_OCCURRED = "An unknown error occurred"
fun Activity.isActivityDestroyed() = isJellyBeanMr1Plus && isDestroyed
fun Activity.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText("commons", text)
    Services.clipboardManager.primaryClip = clip
}

@SuppressLint("NewApi")
fun Activity.getColorCompat(resId: Int): Int {
    return if (isMPlus)
        getColor(resId)
    else
        resources.getColor(resId)
}

fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }
    if (uri == null) {
        toast(STRING_UNKNOWN_ERROR_OCCURRED)
        return null
    }
    return uri
}

fun Activity.isShowingSAFDialog(path: String, treeUri: String, requestCode: Int): Boolean {
    return if (needsStupidWritePermissions(path) && (treeUri.isEmpty() || !hasProperStoredTreeUri())) {
        runOnUiThread {
            WritePermissionDialog(this, false) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra("android.content.extra.SHOW_ADVANCED", true)
                    if (resolveActivity(packageManager) == null) {
                        type = "*/*"
                    }
                    if (resolveActivity(packageManager) != null) {
                        startActivityForResult(this, requestCode)
                    } else {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

@SuppressLint("NewApi")
fun Activity.launchActivity(klass: Class<*>) {
    startActivity(Intent(this, klass))
}

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

fun Activity.openPath(path: String, forceChooser: Boolean, openAsText: Boolean = false) {
    val mimeType = if (openAsText) "text/plain" else ""
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, mimeType)
}

fun Activity.openPathIntent(path: String, forceChooser: Boolean, applicationId: String, forceMimeType: String = "") {
    val newUri = getFinalUriFromPath(path, applicationId) ?: return
    val mimeType = if (forceMimeType.isNotEmpty()) forceMimeType else getUriMimeType(path, newUri)
    Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(newUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (resolveActivity(packageManager) != null) {
            val chooser = Intent.createChooser(this, STRING_OPEN_WITH)
            try {
                startActivity(if (forceChooser) chooser else this)
            } catch (e: NullPointerException) {
                showErrorToast(e)
            }
        } else {
            if (!tryGenericMimeType(this, mimeType, newUri)) {
                toast(STRING_NO_APP_FOUND)
            }
        }
    }
}

fun Activity.setupDialogStuff(view: View, dialog: AlertDialog, titleId: Int = 0, callback: (() -> Unit)? = null) {
    if (isActivityDestroyed()) {
        return
    }
    if (view is ViewGroup)
    else if (view is CustomTextView) {
        view.setColors(Services.textColor, Color.WHITE, Services.backgroundColor)
    }
    var title: TextView? = null
    if (titleId != 0) {
        title = layoutInflater.inflate(R.layout.dialog_title, null) as TextView
        title.dialog_title_textview.apply {
            setText(titleId)
            setTextColor(Services.textColor)
        }
    }
    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(title)
        setCanceledOnTouchOutside(true)
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Services.textColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Services.textColor)
        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Services.textColor)
        window.setBackgroundDrawable(ColorDrawable(Services.backgroundColor))
    }
    callback?.invoke()
}

fun Activity.sharePathIntent(path: String, applicationId: String) {
    Thread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@Thread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                if (resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(this, STRING_SHARE_VIA))
                } else {
                    toast(STRING_NO_APP_FOUND)
                }
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(STRING_MAXIMUM_SHARE_REACHED)
                } else {
                    showErrorToast(e)
                }
            }
        }
    }.start()
}

fun Activity.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(STRING_AN_ERROR_OCCURRED, message))
}

fun Activity.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
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
//    val newUri = getFinalUriFromPath(path, BuildConfig.APPLICATION_ID) ?: return
//    Intent().apply {
//        action = Intent.ACTION_SEND
//        putExtra(Intent.EXTRA_STREAM, newUri)
//        type = getUriMimeType(path, newUri)
//        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        try {
//            if (resolveActivity(packageManager) != null) {
//                startActivity(Intent.createChooser(this, STRING_SHARE_VIA))
//            } else {
//                toast(STRING_NO_APP_FOUND)
//            }
//        } catch (e: RuntimeException) {
//            if (e.cause is TransactionTooLargeException) {
//                toast(STRING_MAXIMUM_SHARE_REACHED)
//            } else {
//                showErrorToast(e)
//            }
//        }
//    }
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = if (isNPlus) {
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.fileprovider", File(path))
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
                toast(STRING_NO_APP_FOUND)
            }
        }
    } else {
        openPath(path, forceChooser, openAsText)
    }
}

fun Context.updateLastModified(path: String, lastModified: Long) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DATE_MODIFIED, lastModified / 1000)
    }
    File(path).setLastModified(lastModified)
    val uri = getFileUri(path)
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)
    try {
        contentResolver.update(uri, values, selection, selectionArgs)
    } catch (ignored: Exception) {
    }
}

fun CustomActivity.createDirectorySync(directory: String): Boolean {
    if (getDoesFilePathExist(directory)) {
        return true
    }
    if (needsStupidWritePermissions(directory)) {
        val documentFile = getDocumentFile(directory.getParentPath()) ?: return false
        val newDir = documentFile.createDirectory(directory.getFilenameFromPath())
        return newDir != null
    }
    return File(directory).mkdirs()
}

fun CustomActivity.deleteFile(file: File, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFileBg(file, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFileBg(file, allowDeleteFolder, callback)
    }
}

fun CustomActivity.deleteFileBg(file: File, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val path = file.path
    val file = File(path)
    var fileDeleted = !path.startsWith(OTG_PATH) && ((!file.exists() && file.length() == 0L) || file.delete())
    if (fileDeleted) {
        rescanDeletedPath(path) {
            runOnUiThread {
                callback?.invoke(true)
            }
        }
    } else {
        if (file.isDirectory && allowDeleteFolder) {
            fileDeleted = deleteRecursively(file)
        }
        if (!fileDeleted) {
            if (isPathOnSD(path)) {
                handleSAFDialog(path) {
                    trySAFFileDelete(file, allowDeleteFolder, callback)
                }
            } else if (path.startsWith(OTG_PATH)) {
                trySAFFileDelete(file, allowDeleteFolder, callback)
            }
        }
    }
}

fun CustomActivity.deleteFiles(files: ArrayList<File>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFilesBg(files, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFilesBg(files, allowDeleteFolder, callback)
    }
}

fun CustomActivity.deleteFilesBg(files: ArrayList<File>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (files.isEmpty()) {
        runOnUiThread {
            callback?.invoke(true)
        }
        return
    }
    var wasSuccess = false
    handleSAFDialog(files[0].path) {
        files.forEachIndexed { index, file ->
            deleteFileBg(file, allowDeleteFolder) {
                if (it) {
                    wasSuccess = true
                }
                if (index == files.size - 1) {
                    runOnUiThread {
                        callback?.invoke(wasSuccess)
                    }
                }
            }
        }
    }
}

fun CustomActivity.renameFile(oldPath: String, newPath: String, callback: ((success: Boolean) -> Unit)? = null) {
    if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            val document = getDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    callback?.invoke(false)
                }
                return@handleSAFDialog
            }
            try {
                /**
                 * Change the display name of an existing document.
                 * <p>
                 * If the underlying provider needs to create a new
                 * {@link Document#COLUMN_DOCUMENT_ID} to represent the updated display
                 * name, that new document is returned and the original document is no
                 * longer valid. Otherwise, the original document is returned.
                 *
                 * @param documentUri document with {@link Document#FLAG_SUPPORTS_RENAME}
                 * @param displayName updated name for document
                 * @return the existing or new document after the rename, or {@code null} if
                 *         failed.
                 */
                // com.android.internal.content.FileSystemProvider
                val uri = DocumentsContract.renameDocument(applicationContext.contentResolver, document.uri, newPath.getFilenameFromPath())
                if (document.uri != uri) {
                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!Services.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        callback?.invoke(false)
                    }
                }
            } catch (e: SecurityException) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false)
                }
            }
        }
    } else if (File(oldPath).renameTo(File(newPath).buildUniqueFile())) {
        if (File(newPath).isDirectory) {
            deleteFromMediaStore(oldPath)
            rescanPaths(arrayListOf(newPath)) {
                runOnUiThread {
                    callback?.invoke(true)
                }
                scanPathRecursively(newPath)
            }
        } else {
            if (!Services.keepLastModified) {
                File(newPath).setLastModified(System.currentTimeMillis())
            }
            scanPathsRecursively(arrayListOf(newPath)) {
                runOnUiThread {
                    callback?.invoke(true)
                }
            }
        }
    } else {
        runOnUiThread {
            callback?.invoke(false)
        }
    }
}

private fun deleteRecursively(file: File): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child)
        }
    }
    return file.delete()
}