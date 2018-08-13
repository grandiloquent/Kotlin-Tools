package psycho.euphoria.tools.commons

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import psycho.euphoria.tools.R

open class CustomActivity : AppCompatActivity() {
    private fun isProperSDFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
    private fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority
    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")
    private fun isInternalStorage(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")

    protected lateinit var mPrefer: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPrefer = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            if (isProperSDFolder(resultData.data)) {
                if (resultData.dataString == mPrefer.getString(PREFER_OTG_TREE_URI, "")) {
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

        return if (!path.startsWith(OTG_PATH) && isShowingSAFDialog(path, mPrefer.getString(PREFER_TREE_URI, ""), REQUEST_OPEN_DOCUMENT_TREE)) {
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
        mPrefer.edit().putString(PREFER_TREE_URI, treeUri.toString()).apply()
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    }

    /*
     if (requestCode == MIC_PERMISSION_REQUEST_CODE && permissions.length > 0) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(coordinatorLayout,
                            "Microphone permissions needed. Please allow in your application settings.",
                            SNACKBAR_DURATION).show();
                } else {
                    retrieveAccessToken();
                }
            }
     */
    companion object {
        const val PREFER_TREE_URI = "tree_uri"
        const val PREFER_OTG_TREE_URI = "otg_tree_uri"
        var funAfterSAFPermission: (() -> Unit)? = null

        fun checkPermission(activity: Activity, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        fun requestPermisson(activity: Activity, permission: String, shouldManual: () -> Unit?, requestPermissionCode: Int) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                // Require users to set up on the phone Manual
                shouldManual?.invoke()
            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(permission), requestPermissionCode)
            }
        }
    }
}