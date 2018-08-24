package psycho.euphoria.file

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.needsStupidWritePermissions
import psycho.euphoria.tools.R
import java.io.File
import java.util.*

const val MENU_DELETE_FILE = 1
private val STRING_DELETE_FILE by lazy {
    if (Locale.getDefault() == Locale.CHINA) "删除"
    else "Delete File"
}

fun bindDeleteFileMenuItem(context: Context, menu: Menu): MenuItem? {
    return menu.add(0, MENU_DELETE_FILE, 0, STRING_DELETE_FILE).also {
        it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        it.icon = context.resources.getDrawable(R.drawable.ic_delete_forever_24px)
    }
}

fun doDeleteFileAction(context: Context, files: Array<File?>, callback: (() -> Unit)?) {

    files.forEach { deleteFile(context, it) }
    callback?.invoke()

}

private fun deleteFile(context: Context, file: File?) {
    if (file == null) return
    if (context.needsStupidWritePermissions(file.absolutePath)) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            DocumentsContract.deleteDocument(context.contentResolver, generateDocumentUriFast(file, Services.treeUri))
        }
    } else {
        if (file.isDirectory) file.walkBottomUp().map { it.delete() }
        else file.delete()
    }
}

fun generateDocumentUriFast(file: File, treeUri: String): Uri {

    val path = file.absolutePath.substringAfter(treeUri.substringAfterLast('/').substringBefore(Uri.encode(":")) + '/');

    // DocumentsContract.buildDocumentUriUsingTree(treeUri,
//                DocumentsContract.getTreeDocumentId(treeUri))
    var fileUri = Uri.encode(path);
    fileUri = "$treeUri/document/${treeUri.substringAfterLast('/')}${fileUri}"
    return Uri.parse(fileUri)

}