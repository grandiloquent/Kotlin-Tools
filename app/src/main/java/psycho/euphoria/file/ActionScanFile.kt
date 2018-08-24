package psycho.euphoria.file

import android.content.Context
import android.media.MediaScannerConnection


fun triggerScanFile(context: Context, files: Array<String?>, callback: (() -> Unit)?) {
    var cnt = files.size
    MediaScannerConnection.scanFile(context, files, null) { _, _ ->
        if (--cnt == 0) callback?.invoke()
    }
}