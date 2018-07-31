package psycho.euphoria.tools.files

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.davidecirillo.multichoicerecyclerview.MultiChoiceAdapter
import com.davidecirillo.multichoicerecyclerview.MultiChoiceToolbar
import kotlinx.android.synthetic.main.activity_file.*
import kotlinx.android.synthetic.main.toolbar.*
import psycho.euphoria.tools.R
import psycho.euphoria.tools.TranslatorActivity
import psycho.euphoria.tools.commons.*
import psycho.euphoria.tools.config
import psycho.euphoria.tools.downloads.DownloadActivity
import psycho.euphoria.tools.music.MediaPlaybackService
import psycho.euphoria.tools.pictures.PictureActivity
import psycho.euphoria.tools.videos.SplitVideo
import psycho.euphoria.tools.videos.VideoActivity
import java.io.File


class FileActivity : CustomActivity() {


    private lateinit var mRecentDirectory: String
    private var mFileAdapter: FileAdapter? = null
    private var mSortOrder = SORT_BY_NAME // Sort by file name by default

    private lateinit var mOptionMenu: Menu


    private fun deleteFiles() {
        mFileAdapter?.let {
            if (it.selectedItemCount < 1) return
            for (i in it.selectedItemList) {
                deleteFile(File(it.getItem(i).path).toFileDirItem(this), true) {
                    if (it) {
                        refreshRecyclerView()
                        mFileAdapter?.deselect(i)
                    }
                }
            }
        }
    }

    private fun initialize() {
        setContentView(R.layout.activity_file)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            // setShowHideAnimationEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24px)
        }
        toolbar.setNavigationOnClickListener { onBackPressed() }
        mSortOrder = config.sortOrder
        initializeRecyclerView()
        refreshRecyclerView(mRecentDirectory)
    }

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
                    tryOpenPathIntent(path, false)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRecentDirectory = config.recentDirectory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.WAKE_LOCK
            ), REQUEST_PERMISSION_CODE)
        } else
            initialize()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOptionMenu = menu
        menuInflater.inflate(R.menu.menu_file, menu)
        updateOptionMenuVisible()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_storage -> {
                with(Environment.getExternalStorageDirectory().absolutePath) {
                    refreshRecyclerView(this)
                    mRecentDirectory = this
                }
            }
            R.id.action_sdcard -> {
                var sdCardPath = config.sdCardPath
                if (sdCardPath.isBlank()) {
                    sdCardPath = getSDCardPath()
                    config.sdCardPath = sdCardPath
                }
                refreshRecyclerView(sdCardPath)
                mRecentDirectory = sdCardPath
            }
            R.id.action_download -> launchActivity(DownloadActivity::class.java)
            R.id.action_sorting -> showSortingDialog()
            R.id.action_translator -> launchActivity(TranslatorActivity::class.java)
            R.id.action_refresh -> refreshRecyclerView()
            R.id.action_delete -> deleteFiles()
            R.id.action_select_all -> selectAll()
            R.id.action_split_video -> {
                mFileAdapter?.let {
                    splitVideo(it.getItem(it.selectedItemList[0]).path)
                }
            }
            R.id.action_rename_file -> {
                mFileAdapter?.let {
                    renameFile(it.getItem(it.selectedItemList[0]))
                }
            }
            R.id.action_scan_file -> scanFile()
            R.id.action_serialize_file_name -> {
                serializeFileName(mRecentDirectory, this)
            }
            R.id.action_serialize_file_name100 -> {
                serializeFileName(mRecentDirectory, this,100)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        // After entering the pause state, save the last accessed directory and the sorting method used
        config.recentDirectory = mRecentDirectory
        config.sortOrder = mSortOrder
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initialize()
    }

    private fun refreshRecyclerView() {
        refreshRecyclerView(mRecentDirectory)
    }

    private fun refreshRecyclerView(path: String) {
        File(path).listFileItems(mSortOrder)?.let {
            if (mFileAdapter == null) {
                // Initialize FileAdapter
                mFileAdapter = FileAdapter(this, it) {
                    if (it != null && it.isDirectory) {
                        refreshRecyclerView(it.path)
                        mRecentDirectory = it.path
                    } else if (it != null) onClickFile(it.path)
                }
                recycler_view.adapter = mFileAdapter
                val builder = MultiChoiceToolbar.Builder(this, toolbar)
                        .setMultiChoiceColours(R.color.colorPrimaryMulti, R.color.colorPrimaryMulti)
                        .setDefaultIcon(R.drawable.ic_arrow_back_white_24px) { onBackPressed() }
                        .setTitles(getString(R.string.app_name), "item selected")
                        .setDefaultColours(getColor(R.color.color_primary), getColor(R.color.color_primary))
                mFileAdapter?.apply {
                    setMultiChoiceToolbar(builder.build())
                    setMultiChoiceSelectionListener(object : MultiChoiceAdapter.Listener {
                        override fun OnItemSelected(selectedPosition: Int, itemSelectedCount: Int, allItemCount: Int) {
                            updateOptionMenuVisible()
                        }

                        override fun OnItemDeselected(deselectedPosition: Int, itemSelectedCount: Int, allItemCount: Int) {
                            updateOptionMenuVisible()
                        }

                        override fun OnSelectAll(itemSelectedCount: Int, allItemCount: Int) {
                            updateOptionMenuVisible()
                        }

                        override fun OnDeselectAll(itemSelectedCount: Int, allItemCount: Int) {
                            updateOptionMenuVisible()
                        }
                    })
                }
            } else {
                mFileAdapter?.switchData(it)
            }
        }
    }

    private fun renameFile(fileItem: FileItem) {
        dialog(this, fileItem.name, getString(R.string.menu_rename_file), true) {
            if (!it.isNullOrBlank()) {
                renameFile(fileItem.path, fileItem.path.getParentPath() + File.separator + it.toString()) {

                    if (it) {
                        refreshRecyclerView()
                        mFileAdapter?.deselectAll()
                    }

                }
            }
        }
    }

    private fun scanFile() {
        mFileAdapter?.let {
            if (it.selectedItemCount < 1) return
            for (i in it.selectedItemList) {
                scanFileRecursively(File(it.getItem(i).path))
            }
        }
    }

    private fun selectAll() {
        mFileAdapter?.apply {
            selectAll()
        }
    }

    private fun showSortingDialog() {
        val adapter = ArrayAdapter<String>(this, R.layout.item_sorting, R.id.text_view)
        adapter.add(getString(R.string.menu_sort_by_name))
        adapter.add(getString(R.string.menu_sort_by_last_modified))
        adapter.add(getString(R.string.menu_sort_by_size))
        val dialog = AlertDialog.Builder(this)
                .setAdapter(adapter) { dialog, position ->
                    when (position) {
                        0 -> sortBy(0)
                        1 -> sortBy(SORT_BY_DATE_MODIFIED)
                        2 -> sortBy(SORT_BY_SIZE)
                    }
                }.create()
        dialog.show()
    }

    private fun sortBy(sortOrder: Int) {
        // Sort the file list in the specified way
        mSortOrder = sortOrder
        // Refresh display after sorting
        refreshRecyclerView()
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

    private fun updateOptionMenuVisible() {
        val count = mFileAdapter?.selectedItemCount ?: 0
        if (count > 1) {
            mOptionMenu.apply {
                // When the number of selected items is greater than 1.
                // Just hide the menu items that only support single item
                // The status of other menu items is the same as when it is equal to 1.
                findItem(R.id.action_rename_file).isVisible = false
                findItem(R.id.action_split_video).isVisible = false
            }
        } else if (count == 1) {
            mOptionMenu.apply {
                findItem(R.id.action_delete).isVisible = true
                findItem(R.id.action_download).isVisible = false
                findItem(R.id.action_refresh).isVisible = false
                findItem(R.id.action_rename_file).isVisible = true
                findItem(R.id.action_scan_file).isVisible = true
                findItem(R.id.action_sdcard).isVisible = false
                findItem(R.id.action_select_all).isVisible = true
                findItem(R.id.action_sorting).isVisible = false
                findItem(R.id.action_split_video).isVisible = true
                findItem(R.id.action_storage).isVisible = false
                findItem(R.id.action_translator).isVisible = true
            }
        } else if (count == 0) {
            mOptionMenu.apply {
                findItem(R.id.action_delete).isVisible = false
                findItem(R.id.action_download).isVisible = true
                findItem(R.id.action_refresh).isVisible = true
                findItem(R.id.action_rename_file).isVisible = false
                findItem(R.id.action_scan_file).isVisible = false
                findItem(R.id.action_sdcard).isVisible = true
                findItem(R.id.action_select_all).isVisible = false
                findItem(R.id.action_sorting).isVisible = true
                findItem(R.id.action_split_video).isVisible = false
                findItem(R.id.action_storage).isVisible = true
                findItem(R.id.action_translator).isVisible = true
            }
        }
        toolbar.postInvalidate()
    }


    companion object {
        private const val REQUEST_PERMISSION_CODE = 100
    }

}