package psycho.euphoria.tools.files

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
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
//
//    private val mActionMode = object : ModalMultiSelectorCallback(mMultiSelector) {
//        // Set the menu for pops up when selecting the item
//        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
//            when (item.itemId) {
//                R.id.action_delete -> {
//
//                    // Close the opened top toolbar
//                    mode.finish()
//                    mFileAdapter?.let {
//                        for (i in 0 until it.itemCount) {
//                            if (mMultiSelector.isSelected(i, 0)) {
//                                val fileItem = it.getItem(i)
//                                File(fileItem.path).deletes()
//                            }
//                        }
//                        refreshRecyclerView()
//                    }
//                    // Clear selected state
//                    mMultiSelector.clearSelections()
//
//                }
//                R.id.action_rename_file -> {
//
//                    mode.finish()
//                    mFileAdapter?.let {
//                        for (i in 0 until it.itemCount) {
//                            if (mMultiSelector.isSelected(i, 0)) {
//                                val fileItem = it.getItem(i)
//
//                                dialog(this@FileActivity, fileItem.name, getString(R.string.menu_rename_file), true) {
//                                    if (!it.isNullOrBlank()) {
//                                        renameFile(fileItem.path, fileItem.path.getParentPath() + File.separator + it.toString()) {
//                                            refreshRecyclerView()
//                                        }
//                                    }
//                                }
//
//                                return true
//                            }
//                        }
//
//
//                        mMultiSelector.clearSelections()
//
//                    }
//                }
//                R.id.action_split_video -> {
//                    mode.finish()
//                    mFileAdapter?.let {
//                        for (i in 0 until it.itemCount) {
//                            if (mMultiSelector.isSelected(i, 0)) {
//                                val fileItem = it.getItem(i)
//                                splitVideo(fileItem.path)
//                                return true
//                            }
//                        }
//
//
//                        mMultiSelector.clearSelections()
//
//                    }
//                }
//                R.id.action_scan_file -> {
//                    mode.finish()
//                    mFileAdapter?.let {
//                        for (i in 0 until it.itemCount) {
//                            if (mMultiSelector.isSelected(i, 0)) {
//                                val fileItem = it.getItem(i)
//                                scanFileRecursively(File(fileItem.path)) {
//                                    toast("Recursive scan file end")
//                                }
//                                return true
//                            }
//                        }
//
//
//                        mMultiSelector.clearSelections()
//
//                    }
//
//                }
//            }
//            return true
//        }
//
//
//        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
//            // Setting the menu items for the actionmode
//            menuInflater.inflate(R.menu.menu_file_action_mode, menu)
//
//            if (mMultiSelector.selectedPositions.size > 1) {
//                menu.findItem(R.id.action_rename_file).isVisible = false
//                menu.findItem(R.id.action_split_video).isVisible = false
//            }
//            return true
//        }
//
//
//    }


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
        mRecentDirectory = config.recentDirectory
        refreshRecyclerView(mRecentDirectory)
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
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectAll() {
        mFileAdapter?.apply {
            selectAll()
        }
    }

    private fun deleteFiles() {
        mFileAdapter?.let {
            if (it.selectedItemCount < 1) return
            for (i in it.selectedItemList) {
                toast(it.getItem(i).path)
            }
        }
    }

    private fun renameFile(fileItem: FileItem) {
        dialog(this, fileItem.name, getString(R.string.menu_rename_file), true) {
            if (!it.isNullOrBlank()) {
                renameFile(fileItem.path, fileItem.path.getParentPath() + File.separator + it.toString()) {
                    refreshRecyclerView()
                }
            }
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

    override fun onPause() {
        // After entering the pause state, save the last accessed directory and the sorting method used
        config.recentDirectory = mRecentDirectory
        config.sortOrder = mSortOrder
        super.onPause()
    }

    private fun refreshRecyclerView() {
        refreshRecyclerView(mRecentDirectory)
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
                        .setDefaultColours(R.color.color_primary, R.color.color_primary)

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

    companion object {

    }

}