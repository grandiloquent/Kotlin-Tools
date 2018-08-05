package psycho.euphoria.tools.commons

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.DocumentsContract
import android.support.v4.content.FileProvider
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.dialog_title.view.*
import psycho.euphoria.tools.BuildConfig
import psycho.euphoria.tools.R
import java.io.*


fun Activity.isActivityDestroyed() = isJellyBean1Plus() && isDestroyed


fun <T> Activity.launchActivity(clazz: Class<T>) {
    val intent = Intent(this, clazz)
    startActivity(intent)
}

fun Activity.copyToClipboard(text: String) {
    val clip = ClipData.newPlainText(getString(R.string.simple_commons), text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip = clip
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

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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

fun Activity.requestFullScreen() {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
}

fun Activity.requestKeepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.rescanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

fun Activity.scanFileRecursively(file: File, callback: (() -> Unit)? = null) {
    applicationContext.scanFileRecursively(file, callback)
}

fun Activity.scanFilesRecursively(files: ArrayList<File>, callback: (() -> Unit)? = null) {
    applicationContext.scanFilesRecursively(files, callback)
}

fun Activity.scanPathRecursively(path: String, callback: (() -> Unit)? = null) {
    applicationContext.scanPathRecursively(path, callback)
}

fun Activity.scanPathsRecursively(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    applicationContext.scanPathsRecursively(paths, callback)
}

fun Activity.setupDialogStuff(view: View, dialog: AlertDialog, titleId: Int = 0, callback: (() -> Unit)? = null) {
    if (isActivityDestroyed()) {
        return
    }
    if (view is ViewGroup)
        updateTextColors(view)
    else if (view is CustomTextView) {
        view.setColors(baseConfig.textColor, getAdjustedPrimaryColor(), baseConfig.backgroundColor)
    }
    var title: TextView? = null
    if (titleId != 0) {
        title = layoutInflater.inflate(R.layout.dialog_title, null) as TextView
        title.dialog_title_textview.apply {
            setText(titleId)
            setTextColor(baseConfig.textColor)
        }
    }
    dialog.apply {
        setView(view)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCustomTitle(title)
        setCanceledOnTouchOutside(true)
        show()
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(baseConfig.textColor)
        getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(baseConfig.textColor)
        getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(baseConfig.textColor)
        window.setBackgroundDrawable(ColorDrawable(baseConfig.backgroundColor))
    }
    callback?.invoke()
}

fun Activity.showErrorToast(msg: String, length: Int = Toast.LENGTH_LONG) {
    toast(String.format(getString(R.string.an_error_occurred), msg), length)
}

fun Activity.showErrorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    showErrorToast(exception.toString(), length)
}

fun Activity.getColorCompat(resId: Int): Int {
    return if (Build.VERSION.SDK_INT >= 23)
        getColor(resId)
    else
        resources.getColor(resId)
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

fun CustomActivity.deleteFile(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFileBg(fileDirItem, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFileBg(fileDirItem, allowDeleteFolder, callback)
    }
}

fun CustomActivity.deleteFileBg(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val path = fileDirItem.path
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
                    trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                }
            } else if (path.startsWith(OTG_PATH)) {
                trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
            }
        }
    }
}

fun CustomActivity.deleteFiles(files: ArrayList<FileDirItem>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFilesBg(files, allowDeleteFolder, callback)
        }.start()
    } else {
        deleteFilesBg(files, allowDeleteFolder, callback)
    }
}

fun CustomActivity.deleteFilesBg(files: ArrayList<FileDirItem>, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
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

fun CustomActivity.deleteFolder(folder: FileDirItem, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        Thread {
            deleteFolderBg(folder, deleteMediaOnly, callback)
        }.start()
    } else {
        deleteFolderBg(folder, deleteMediaOnly, callback)
    }
}

fun CustomActivity.deleteFolderBg(fileDirItem: FileDirItem, deleteMediaOnly: Boolean = true, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    val folder = File(fileDirItem.path)
    if (folder.exists()) {
        val filesArr = folder.listFiles()
        if (filesArr == null) {
            runOnUiThread {
                callback?.invoke(true)
            }
            return
        }
        val files = filesArr.toMutableList().filter { !deleteMediaOnly || it.isImageVideoGif() }
        for (file in files) {
            deleteFileBg(file.toFileDirItem(applicationContext), false) { }
        }
        if (folder.listFiles()?.isEmpty() == true) {
            deleteFileBg(fileDirItem, true) { }
        }
    }
    runOnUiThread {
        callback?.invoke(true)
    }
}

fun CustomActivity.getFileInputStreamSync(path: String): InputStream? {
    return if (path.startsWith(OTG_PATH)) {
        val fileDocument = getSomeDocumentFile(path)
        applicationContext.contentResolver.openInputStream(fileDocument?.uri)
    } else {
        FileInputStream(File(path))
    }
}

fun CustomActivity.getFileOutputStream(fileDirItem: FileDirItem, allowCreatingNewFile: Boolean = false, callback: (outputStream: OutputStream?) -> Unit) {
    if (needsStupidWritePermissions(fileDirItem.path)) {
        handleSAFDialog(fileDirItem.path) {
            var document = getDocumentFile(fileDirItem.path)
            if (document == null && allowCreatingNewFile) {
                document = getDocumentFile(fileDirItem.getParentPath())
            }
            if (document == null) {
                val error = String.format(getString(R.string.could_not_create_file), fileDirItem.path)
                showErrorToast(error)
                callback(null)
                return@handleSAFDialog
            }
            if (!File(fileDirItem.path).exists()) {
                document = document.createFile("", fileDirItem.name)
            }
            if (document?.exists() == true) {
                try {
                    callback(applicationContext.contentResolver.openOutputStream(document.uri))
                } catch (e: FileNotFoundException) {
                    showErrorToast(e)
                    callback(null)
                }
            } else {
                val error = String.format(getString(R.string.could_not_create_file), fileDirItem.path)
                showErrorToast(error)
                callback(null)
            }
        }
    } else {
        val file = File(fileDirItem.path)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        try {
            callback(FileOutputStream(file))
        } catch (e: Exception) {
            callback(null)
        }
    }
}

fun CustomActivity.getFileOutputStreamSync(path: String, mimeType: String, parentDocumentFile: DocumentFile? = null): OutputStream? {
    val targetFile = File(path)
    return if (needsStupidWritePermissions(path)) {
        val documentFile = parentDocumentFile ?: getDocumentFile(path.getParentPath())
        if (documentFile == null) {
            val error = String.format(getString(R.string.could_not_create_file), targetFile.parent)
            showErrorToast(error)
            return null
        }
        val newDocument = documentFile.createFile(mimeType, path.getFilenameFromPath())
        applicationContext.contentResolver.openOutputStream(newDocument!!.uri)
    } else {
        try {
            FileOutputStream(targetFile)
        } catch (e: Exception) {
            showErrorToast(e)
            null
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
                        if (!baseConfig.keepLastModified) {
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
            if (!baseConfig.keepLastModified) {
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