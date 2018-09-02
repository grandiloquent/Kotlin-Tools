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


const val MENU_RENAME_MP3_FILE = 5
private val STRING_RENAME_MP3_FILE by lazy {
    if (Locale.getDefault() == Locale.CHINA) "重命名 Mp3 文件"
    else "Rename MP3 File"
}

fun bindRenameMp3FileMenuItem(context: Context, menu: Menu): MenuItem? {
    return menu.add(0, MENU_RENAME_MP3_FILE, 0, STRING_RENAME_MP3_FILE).also {
        it.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    }
}

fun renameMp3File(context: Context, oldFile: String) {
    Native.getInstacne().RenameMp3File(oldFile);
}