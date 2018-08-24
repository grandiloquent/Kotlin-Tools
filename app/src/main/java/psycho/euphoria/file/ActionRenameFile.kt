package psycho.euphoria.file

import android.content.Context
import android.os.Build
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.buildUniqueFile
import psycho.euphoria.common.extension.needsStupidWritePermissions
import psycho.euphoria.tools.R
import java.io.File
import java.util.*


const val MENU_RENAME_FILE = 2
private val STRING_RENAME_FILE by lazy {
    if (Locale.getDefault() == Locale.CHINA) "重命名"
    else "Rename"
}

fun bindRenameFileMenuItem(context: Context, menu: Menu): MenuItem? {
    return menu.add(0, MENU_RENAME_FILE, 0, STRING_RENAME_FILE).also {
        it.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

    }
}

fun renameFile(context: Context, oldFile: File, newFile: File) {
    val newFile = newFile.buildUniqueFile()
    if (context.needsStupidWritePermissions(oldFile.absolutePath)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.renameDocument(context.contentResolver, generateDocumentUriFast(oldFile, Services.treeUri), newFile.name)
        }
    } else {
        oldFile.renameTo(newFile)
    }
}