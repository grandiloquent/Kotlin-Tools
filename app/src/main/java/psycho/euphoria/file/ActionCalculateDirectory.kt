package psycho.euphoria.file

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.buildUniqueFile
import psycho.euphoria.common.extension.needsStupidWritePermissions
import psycho.euphoria.tools.R
import java.io.File
import java.util.*


const val MENU_CALCULATE_DIRECTORY = 3
private val STRING_CALCULATE_DIRECTORY by lazy {
    if (Locale.getDefault() == Locale.CHINA) "计算目录大小"
    else "Calculate Directory"
}

fun bindCalculateDirectoryMenuItem(context: Context, menu: Menu): MenuItem? {
    return menu.add(0, MENU_CALCULATE_DIRECTORY, 0, STRING_CALCULATE_DIRECTORY).also {
        it.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)

    }
}

fun calculateDirectory(context: Context, dir: String) {
    val str = Native.getInstacne().calculateDirectory(dir);
    File(Environment.getExternalStorageDirectory(), "dirSize.txt").writeText(str);
}