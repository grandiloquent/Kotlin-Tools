package psycho.euphoria.tools.commons

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.support.v7.app.AppCompatActivity
import psycho.euphoria.tools.OTG_PATH
import psycho.euphoria.tools.R

class CustomActivity : AppCompatActivity() {
    private fun isProperSDFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
    private fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority
    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")
    private fun isInternalStorage(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            if (isProperSDFolder(resultData.data)) {
                if (resultData.dataString == baseConfig.OTGTreeUri) {
                    toast(R.string.sd_card_otg_same)
                    return
                }
                saveTreeUri(resultData)
                funAfterSAFPermission?.invoke()
                funAfterSAFPermission = null
            } else {
                toast(R.string.wrong_root_selected)
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, requestCode)
            }
        }
    }
    fun handleSAFDialog(path: String, callback: () -> Unit): Boolean {
        return if (!path.startsWith(OTG_PATH) && isShowingSAFDialog(path, baseConfig.treeUri, REQUEST_OPEN_DOCUMENT_TREE)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback()
            false
        }
    }

    override fun onDestroy() {

        super.onDestroy()
        funAfterSAFPermission = null
    }
    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }
    companion object {
        var funAfterSAFPermission: (() -> Unit)? = null
    }
}