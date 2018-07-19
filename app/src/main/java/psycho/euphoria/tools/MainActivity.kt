package psycho.euphoria.tools

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import psycho.euphoria.tools.downloads.DownloadDatabase
import psycho.euphoria.tools.downloads.DownloadService

class MainActivity : Activity() {
    private lateinit var mButtonPicture: Button
    private fun initialize() {
        setContentView(R.layout.activity_main)
        mButtonPicture = findViewById(R.id.buttonPicture)
        mButtonPicture.setOnClickListener {
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
            downloadFile()
        }
    }

    fun downloadFile() {


        val editText = EditText(this)
        AlertDialog.Builder(this)
                .setView(editText)
                .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("确定") { dialog, _ ->

                    if (editText.text.toString().isNotBlank()) {
                        DownloadDatabase.getInstance(MainActivity@ this).insert(editText.text.toString().trim())
                        val intent = Intent(MainActivity@ this, DownloadService::class.java)
                        startService(intent)
                    }

                }.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), REQUEST_PERMISSIONS_CODE);
        } else initialize()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        initialize()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 1;
        const val TYPE_PICTURE = "picture"
        const val TYPE_VIDEO = "video"
        const val TYPE_MUSIC = "music"
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}