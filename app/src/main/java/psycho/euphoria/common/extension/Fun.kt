package psycho.euphoria.common.extension

import android.os.Build
import android.os.Environment

fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
fun getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
