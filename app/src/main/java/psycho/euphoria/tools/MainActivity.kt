package psycho.euphoria.tools

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import psycho.euphoria.tools.commons.App
import psycho.euphoria.tools.downloads.DownloadActivity
import psycho.euphoria.tools.files.FileActivity
import psycho.euphoria.tools.videos.VideoActivity

class MainActivity : Activity() {
    private lateinit var mButtonPicture: Button
    private fun initialize() {
        setContentView(R.layout.activity_main)
        mButtonPicture = findViewById(R.id.buttonPicture)
        mButtonPicture.setOnClickListener {
//            val i=Intent(App.instance,VideoActivity::class.java)
//            i.putExtra("path","/storage/emulated/0/xvideos.com_fb8f508653cb923e569700c1944944a0.mp4")
//            startActivity(i)
            val i = Intent(MainActivity@ this, FileActivity::class.java)
            i.putExtra(TYPE_PICTURE, true)
            startActivity(i)
        }
        findViewById<Button>(R.id.buttonMusic).setOnClickListener {
            val i = Intent(MainActivity@ this, FileActivity::class.java)
            i.putExtra(TYPE_MUSIC, true)
            startActivity(i)
        }
        findViewById<Button>(R.id.buttonDownload).setOnClickListener {
            launchDownloadActivity()
        }
    }

    fun launchDownloadActivity() {
        val intent = Intent(this, DownloadActivity::class.java)
        startActivity(intent)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_PERMISSIONS_CODE)
        } else initialize()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        initialize()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 1
        const val TYPE_PICTURE = "picture"
        const val TYPE_VIDEO = "video"
        const val TYPE_MUSIC = "music"
    }

}