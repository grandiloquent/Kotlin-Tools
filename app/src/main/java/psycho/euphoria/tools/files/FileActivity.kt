package psycho.euphoria.tools.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_download.*
import psycho.euphoria.tools.music.MediaPlaybackService
import psycho.euphoria.tools.pictures.PictureActivity
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.*
import psycho.euphoria.tools.config
import psycho.euphoria.tools.downloads.DownloadActivity
import psycho.euphoria.tools.videos.SplitVideo
import psycho.euphoria.tools.videos.VideoActivity
import java.io.File


class FileActivity : AppCompatActivity() {


    private lateinit var mRecentDirectory: String
    private var mFileAdapter: FileAdapter? = null
    private var mSortOrder = SORT_BY_NAME


    private fun initializeRecyclerView() {
        recycler_view.run {
            setHasFixedSize(true)
            adapter = mFileAdapter
            registerForContextMenu(this)
        }
    }

    override fun onBackPressed() {
        val parent = File(mRecentDirectory).parentFile
        if (parent == null)
            super.onBackPressed()
        else {
            refreshRecyclerView(parent.absolutePath)
            mRecentDirectory = parent.absolutePath
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo) {

        menu.add(0, MENU_DELELTE, 0, getString(R.string.menu_detelte));
        if (menuInfo is ContextMenuRecyclerView.ContextMenuInfo) {
            val fileItem = mFileAdapter!!.getItem(menuInfo.position)
            when {
                fileItem.path.isVideoFast() -> {
                    menu.add(0, MENU_SPLIT_VIDEO, 0, getString(R.string.menu_split_video))
                }
            }
        }

        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val menuInfo = item.menuInfo
        if (menuInfo is ContextMenuRecyclerView.ContextMenuInfo) {
            mFileAdapter?.let {

                when (item.itemId) {
                    MENU_DELELTE -> {
                        val fileItem = it.getItem(menuInfo.position)
                        File(fileItem.path).deletes()
                        refreshRecyclerView()
                    }
                    MENU_DELELTE -> {
                        val fileItem = it.getItem(menuInfo.position)
                        splitVideo(fileItem.path)

                    }
                }
            }

        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        mSortOrder = config.sortOrder
        initializeRecyclerView()
        mRecentDirectory = config.recentDirectory
        refreshRecyclerView(mRecentDirectory)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        with(menu) {
            add(0, KEY_INTERNAL_STORAGE, 0, getString(R.string.menu_storage))
            add(0, KEY_SDCARD, 0, getString(R.string.menu_sd_card))
            add(0, KEY_DOWNLOAD, 0, getString(R.string.menu_downlaod))
            add(0, KEY_SORT_BY_LAST_MODIFIED,1,getString(R.string.menu_sort_by_last_modified))
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            KEY_INTERNAL_STORAGE -> {
                with(Environment.getExternalStorageDirectory().absolutePath) {
                    refreshRecyclerView(this)
                    mRecentDirectory = this
                }
            }
            KEY_SDCARD -> {
                var sdCardPath = config.sdCardPath
                if (sdCardPath.isBlank()) {
                    sdCardPath = getSDCardPath()
                    config.sdCardPath = sdCardPath
                }
                refreshRecyclerView(sdCardPath)
                mRecentDirectory = sdCardPath
            }
            KEY_DOWNLOAD -> {
                val intent = Intent(this, DownloadActivity::class.java)
                startActivity(intent)
            }
            KEY_SORT_BY_LAST_MODIFIED -> {
                mSortOrder = SORT_BY_DATE_MODIFIED
                refreshRecyclerView()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        config.recentDirectory = mRecentDirectory
        config.sortOrder = mSortOrder
        super.onPause()
    }

    private fun refreshRecyclerView() {
        refreshRecyclerView(mRecentDirectory)
    }

    private fun refreshRecyclerView(path: String) {
        File(path).listFileItems(mSortOrder)?.let {
            if (mFileAdapter == null) {
                mFileAdapter = FileAdapter(this, it) {
                    if (it.isDirectory) {
                        refreshRecyclerView(it.path)
                        mRecentDirectory = it.path
                    } else onClickFile(it.path)
                }
                recycler_view.adapter = mFileAdapter
            } else {
                mFileAdapter?.switchData(it)
            }
        }
    }

    private fun onClickFile(path: String) {
        when {
            path.isVideoFast() -> {
                val intent = Intent(this, VideoActivity::class.java)
                intent.putExtra(KEY_PATH, path)
                startActivity(intent)
            }
            path.isImageFast() -> {
                val intent = Intent(this, PictureActivity::class.java)
                intent.putExtra(KEY_PATH, path)
                startActivity(intent)
            }
            path.isAudioFast() -> {
                val intent = Intent(this, MediaPlaybackService::class.java)
                intent.putExtra(MediaPlaybackService.KEY_PLAY_DIRECTORY, File(path).parentFile.absolutePath)
                intent.putExtra(MediaPlaybackService.KEY_PLAY_POSITION, 0)
                startService(intent)
            }
            else -> {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.fromFile(File(path)))
                    if (intent.resolveActivity(packageManager) != null)
                        startActivity(intent)
                } catch (e: Exception) {
                    showErrorToast(e)
                }

            }
        }

    }

    private fun splitVideo(path: String) {

        dialog(this, "0.00 1.00", getString(R.string.menu_split_video)) {
            if (it != null) {
                try {
                    val parameters = it.toString().trim().split(' ')
                    if (parameters.size > 1) {
                        val start = parameters[0].convertToSeconds()
                        val end = parameters[1].convertToSeconds()
                        if (end > start)
                            SplitVideo(path).execute(start * 1.0, end * 1.0)

                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    companion object {
        private const val KEY_INTERNAL_STORAGE = 0
        private const val KEY_SDCARD = 1
        private const val KEY_DOWNLOAD = 2
        private const val KEY_SORT_BY_LAST_MODIFIED = 3

        private const val MENU_COPY = 11
        private const val MENU_DELELTE = 12
        private const val MENU_SPLIT_VIDEO = 13

    }

}