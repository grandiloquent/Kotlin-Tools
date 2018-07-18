package psycho.euphoria.tools

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.NumberPicker
import android.widget.Toast
import psycho.euphoria.tools.downloads.*
import java.io.File

class PreViewActivity : Activity() {


    private fun initialize() {
        val that = this
        val executor = getExecutor()
        val uri = "https://alissl.ucdl.pp.uc.cn/fs08/2018/04/24/2/110_de0a68c8da6cd772725f496f285e7936.apk?appid=1507879&packageid=800615771&md5=3beb2d10a4caedaac0cdf4f8aa12af5f&apprd=1507879&pkg=mixiaba.com.Browser&vcode=94&fname=%E7%B1%B3%E4%BE%A0%E6%B5%8F%E8%A7%88%E5%99%A8&iconUrl=http%3A%2F%2Fandroid%2Dartworks%2E25pp%2Ecom%2Ffs08%2F2018%2F04%2F24%2F0%2F110%5F61b560c83365378b9b789490694a57d9%5Fcon%2Epng"
        DownloadDatabase.getInstance(this).insert(uri)

        val fileName = generateFileNameFromURL(uri, Environment.getExternalStorageDirectory())
        executor.execute(Downloader(
                DownloadInfo(
                        uri,
                        fileName,
                        if (File(fileName).isFile) File(fileName).length() else 0L
                )).apply {
            notifyDownloadSpeed = fun(id, speed) {
                runOnUiThread {
                    Toast.makeText(that, "${speed.formatSize()}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.INTERNET
            ), REQUEST_PERMISSIONS_CODE);
        } else initialize()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        initialize()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 1;
    }
}