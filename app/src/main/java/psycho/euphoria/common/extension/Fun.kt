package psycho.euphoria.common.extension
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Editable
import android.view.WindowManager
import android.widget.EditText
import java.io.File
import java.io.FileFilter
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
fun getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
fun dialog(context: Context, content: String?, title: String?, isForFileName: Boolean = false, positiveListener: (Editable?) -> Unit) {
    val editText = EditText(context)
    editText.maxLines = 1
    editText.setText(content)
    if (isForFileName) {
        content?.let {
            val pos = it.lastIndexOf('.')
            if (pos > -1) {
                editText.setSelection(0, pos)
            }
        }
    }
    val dialog = AlertDialog.Builder(context)
            .setView(editText)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                positiveListener(editText.text)
            }.create()
    //  Show the input keyboard for user
    dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    dialog.show()
}
fun serializeFileName(path: String, context: Context, startValue: Int = 1) {
    val dir = File(path)
    if (!dir.isDirectory) return
    val files = dir.listFiles(FileFilter {
        it.isFile
    })
    val chinese = Regex("[\\u4E00-\\u9FA5]+")
    val map = HashMap<String, Int>()
    for (file in files) {
        val matchValue = chinese.find(file.name)
        if (matchValue != null && !map.containsKey(matchValue.value)) {
            map.put(matchValue.value, startValue)
        }
    }
    if (map.isNotEmpty()) {
        for (file in files) {
            val matchValue = chinese.find(file.name)
            if (matchValue != null && map.containsKey(matchValue.value)) {
                val ext = if (file.extension.isBlank()) "mp4" else file.extension
                val targetFile = File(file.parentFile, matchValue.value + map[matchValue.value] + "." + ext)
                if (context.needsStupidWritePermissions(targetFile.absolutePath)) {
                    val document = context.getDocumentFile(file.absolutePath)
                    if (document != null)
                        DocumentsContract.renameDocument(context.applicationContext.contentResolver, document.uri, targetFile.absolutePath.getFilenameFromPath())
                } else {
                    file.renameTo(targetFile)
                }
                map.set(matchValue.value, map[matchValue.value]?.plus(1) ?: 1)
            }
        }
    }
}