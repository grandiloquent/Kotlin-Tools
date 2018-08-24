package psycho.euphoria.file

import android.content.Context
import android.os.Build
import android.provider.DocumentsContract
import psycho.euphoria.common.Services
import psycho.euphoria.common.extension.needsStupidWritePermissions
import java.io.File

private fun renameFile(context: Context, oldFile: File, newFile: File) {
    if (context.needsStupidWritePermissions(oldFile.absolutePath)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.renameDocument(context.contentResolver, generateDocumentUriFast(oldFile, Services.treeUri), newFile.name)
        }
    } else {
        oldFile.renameTo(newFile)
    }
}