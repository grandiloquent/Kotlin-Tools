package psycho.euphoria.file

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.davidecirillo.multichoicerecyclerview.MultiChoiceAdapter
import com.davidecirillo.multichoicerecyclerview.MultiChoiceToolbar
import kotlinx.android.synthetic.main.activity_file.*
import kotlinx.android.synthetic.main.toolbar.*
import psycho.euphoria.common.*
import psycho.euphoria.common.extension.*
import psycho.euphoria.common.ui.SwipeRefreshLayout
import psycho.euphoria.download.DownloadActivity
import psycho.euphoria.player.PlayerActivity
import psycho.euphoria.tools.R
import psycho.euphoria.tools.TranslatorActivity
import psycho.euphoria.tools.music.MediaPlaybackService
import psycho.euphoria.tools.pictures.PictureActivity
import psycho.euphoria.player.SplitVideo
import java.io.File

class FileActivity : CustomActivity() {
    private var mFileAdapter: FileAdapter? = null
    private var mRecentDirectory = Environment.getExternalStorageDirectory().absolutePath
    private var mSortOrder = SORT_BY_NAME
    private lateinit var mOptionMenu: Menu
    private var mBookmark = Bookmark(this)
    private val mHandler = Handler()


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
        initializeRecyclerView()
        refreshRecyclerView(mRecentDirectory)
        pull_refresh_view.listener = object : SwipeRefreshLayout.OnRefreshListener {
            override fun onRefresh() {
                refreshRecyclerView()
                pull_refresh_view.setRefreshing(false)
            }
        }
    }

    private fun initializeRecyclerView() {
        recycler_view.run {
            setHasFixedSize(true)
            adapter = mFileAdapter
            registerForContextMenu(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_VIDEO_CODE && resultCode == Activity.RESULT_OK) {
            refreshRecyclerView()
        }
    }

    override fun onBackPressed() {
        mFileAdapter?.let {
            if (it.selectedItemCount > 0) {
                it.deselectAll()
                return
            }
        }
        val parent = File(mRecentDirectory).parentFile
        if (parent == null)
            super.onBackPressed()
        else {
            refreshRecyclerView(parent.absolutePath)
            mRecentDirectory = parent.absolutePath
        }
    }

    private fun onClickFile(path: String) {
        //if ((mFileAdapter?.selectedItemCount ?: 0) > 0) return
        when {
            path.isVideoFast() -> {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.data = File(path).toUri()
                startActivityForResult(intent, REQUEST_VIDEO_CODE)
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
        mSortOrder = Services.prefer.int(STATE_SORT_ORDER)
        mRecentDirectory = Services.prefer.getString(STATE_RECENT_DIRECTORY, Environment.getExternalStorageDirectory().absolutePath)
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
        bindDeleteFileMenuItem(this, menu)
        bindRenameFileMenuItem(this,menu)

        menuInflater.inflate(R.menu.menu_file, menu)
        updateOptionMenuVisible()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_storage -> {
                mBookmark.show { it ->
                    refreshRecyclerView(it)
                    mRecentDirectory = it
                }
            }
            R.id.action_download -> launchActivity(DownloadActivity::class.java)
            R.id.action_sorting -> showSortingDialog()
            R.id.action_translator -> launchActivity(TranslatorActivity::class.java)
            MENU_DELETE_FILE -> {
                mFileAdapter?.let { it ->
                    val files = arrayOfNulls<File>(it.selectedItemCount)
                    for (i in 0 until files.size) {
                        files[i] = File(it.getItem(it.selectedItemList[i]).path)
                    }
                    doDeleteFileAction(this, files) {
                        refreshRecyclerView()
                        it.deselectAll()
                        mHandler.post { triggerScanFile(this, files.map { it?.absolutePath }.toTypedArray(), null) }
                    }
                }
            }
            MENU_RENAME_FILE -> {
                mFileAdapter?.let {
                    val cur = it.getItem(it.selectedItemList[0]);

                    dialog(this, cur.name, getString(R.string.menu_rename_file), true) {
                        if (!it.isNullOrBlank()) {
                            renameFile(this, File(cur.path), File(cur.path.getParentPath(), it.toString()))
                            refreshRecyclerView()
                        }
                    }

                }
            }
            R.id.action_select_all -> selectAll()
            R.id.action_split_video -> {
                mFileAdapter?.let {
                    splitVideo(it.getItem(it.selectedItemList[0]).path)
                }
            }

            R.id.action_scan_file -> scanFile()
            R.id.action_serialize_file_name -> {
                serializeFileName(mRecentDirectory, this)
            }
            R.id.action_serialize_file_name100 -> {
                serializeFileName(mRecentDirectory)
            }
            R.id.action_combine_htmls -> combineSafari(File(mRecentDirectory))
            R.id.action_copy_filename -> {
                mFileAdapter?.let {
                    copyToClipboard(it.getItem(it.selectedItemList[0]).name)
                }
            }
            R.id.action_gbk_utf -> {
                mFileAdapter?.let {
                    gbk2utf(it.getItem(it.selectedItemList[0]).path)
                }
            }
            R.id.action_add_bookmark -> {
                mFileAdapter?.let {
                    var f = it.getItem(it.selectedItemList[0])
                    mBookmark.put(if (f.isDirectory) File(f.path) else File(f.path).parentFile)
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        Services.prefer.putInt(STATE_SORT_ORDER, mSortOrder)
        Services.prefer.putString(STATE_RECENT_DIRECTORY, mRecentDirectory)
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initialize()
    }

    private fun refreshRecyclerView() {
        refreshRecyclerView(mRecentDirectory, true)
    }

    private fun refreshRecyclerView(path: String, isRefresh: Boolean = false) {
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
                        .setDefaultColours(Services.getColor(R.color.color_primary), Services.getColor(R.color.color_primary))
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
//                if (isRefresh) {
//                    mFileAdapter?.refreshData(it)
//                } else
//                    mFileAdapter?.switchData(it)
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
        this.mSortOrder = sortOrder
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
                findItem(MENU_RENAME_FILE).isVisible = false
                findItem(R.id.action_split_video).isVisible = false
                findItem(R.id.action_copy_filename).isVisible = false
            }
        } else if (count == 1) {
            mOptionMenu.apply {
                findItem(MENU_DELETE_FILE).isVisible = true
                findItem(R.id.action_download).isVisible = false
                findItem(MENU_RENAME_FILE).isVisible = true
                findItem(R.id.action_scan_file).isVisible = true
                findItem(R.id.action_select_all).isVisible = true
                findItem(R.id.action_sorting).isVisible = false
                findItem(R.id.action_split_video).isVisible = true
                findItem(R.id.action_storage).isVisible = false
                findItem(R.id.action_translator).isVisible = true
                findItem(R.id.action_copy_filename).isVisible = true
                findItem(R.id.action_gbk_utf).isVisible = true
                findItem(R.id.action_add_bookmark).isVisible = true
            }
        } else if (count == 0) {
            mOptionMenu.apply {
                findItem(R.id.action_add_bookmark).isVisible = false
                findItem(R.id.action_download).isVisible = true
                findItem(MENU_RENAME_FILE).isVisible = false
                findItem(R.id.action_scan_file).isVisible = false
                findItem(R.id.action_select_all).isVisible = false
                findItem(R.id.action_sorting).isVisible = true
                findItem(R.id.action_split_video).isVisible = false
                findItem(R.id.action_storage).isVisible = true
                findItem(R.id.action_translator).isVisible = true
                findItem(R.id.action_gbk_utf).isVisible = false
                findItem(R.id.action_add_bookmark).isVisible = false
            }
        }
        toolbar.postInvalidate()
    }

    companion object {
        private const val REQUEST_PERMISSION_CODE = 100
        private const val REQUEST_VIDEO_CODE = 1
        private const val STATE_SORT_ORDER = "sort_order"
        private const val STATE_RECENT_DIRECTORY = "recent_directory"
        const val PREFER_SD_CARD_PATH = "sd_card_path"
    }
}