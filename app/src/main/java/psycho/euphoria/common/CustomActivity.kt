package psycho.euphoria.common
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewConfiguration
import psycho.euphoria.common.extension.*
import psycho.euphoria.tools.R
open class CustomActivity : AppCompatActivity() {
    protected var mIsHasBar = false
    fun handleSAFDialog(path: String, callback: () -> Unit): Boolean {
        return if (!path.startsWith(OTG_PATH) && isShowingSAFDialog(path, Services.treeUri, REQUEST_OPEN_DOCUMENT_TREE)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback()
            false
        }
    }
    fun hasNavBar(): Boolean {
        // Log.e(TAG, "hasNavBar")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val display = windowManager.defaultDisplay
            val realDisplayMetrics = DisplayMetrics()
            display.getRealMetrics(realDisplayMetrics)
            val rh = realDisplayMetrics.heightPixels
            var rw = realDisplayMetrics.widthPixels
            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)
            val h = displayMetrics.heightPixels
            val w = displayMetrics.widthPixels
            rw - w > 0 || rh - h > 0
        } else {
            val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
            val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
            !hasMenuKey && !hasBackKey
        }
    }
    fun hideSystemUI(toggleActionBarVisibility: Boolean) {
        if (toggleActionBarVisibility) {
            supportActionBar?.hide()
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE
    }
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    private fun isInternalStorage(uri: Uri): Boolean {
        if (isLollipopPlus) {
            return isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")
        }
        return false
    }
    private fun isProperSDFolder(uri: Uri): Boolean {
        if (isLollipopPlus) {
            return isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
        }
        return false
    }
    private fun isRootUri(uri: Uri): Boolean {
        if (isLollipopPlus) {
            DocumentsContract.getTreeDocumentId(uri).endsWith(":")
        }
        return false
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            if (isProperSDFolder(resultData.data)) {
                if (resultData.dataString == Services.OTGTreeUri) {
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIsHasBar = hasNavBar()
    }
    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
    }
    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        Services.treeUri = treeUri.toString()
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (isKitkatPlus) {
            applicationContext.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        }
    }
    fun showSystemUI(toggleActionBarVisibility: Boolean) {
        if (toggleActionBarVisibility) {
            supportActionBar?.show()
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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