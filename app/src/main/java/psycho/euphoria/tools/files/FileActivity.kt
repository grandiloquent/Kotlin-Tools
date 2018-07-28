package psycho.euphoria.tools.files

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
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

        val builder = MultiChoiceToolbar.Builder(this, toolbar)
                .setMultiChoiceColours(R.color.colorPrimaryMulti, R.color.colorPrimaryMulti)
                .setDefaultIcon(R.drawable.ic_arrow_back_white_24px) { onBackPressed() }
                .setTitles(getString(R.string.toolbar_controls), "item selected")


        mFileAdapter?.apply {
            setMultiChoiceToolbar(builder.build())
            setMultiChoiceSelectionListener(object : MultiChoiceAdapter.Listener {
                override fun OnItemSelected(selectedPosition: Int, itemSelectedCount: Int, allItemCount: Int) {

                    if (itemSelectedCount > 1) {

                    }
                    mOptionMenu.findItem(R.id.action_select_all).isVisible = true;
                    invalidateOptionsMenu()
                }

                override fun OnItemDeselected(deselectedPosition: Int, itemSelectedCount: Int, allItemCount: Int) {

                }

                override fun OnSelectAll(itemSelectedCount: Int, allItemCount: Int) {

                }

                override fun OnDeselectAll(itemSelectedCount: Int, allItemCount: Int) {

                }

            })
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
        with(menu) {

            add(0, KEY_SDCARD, 0, getString(R.string.menu_sd_card))

            findItem(R.id.action_select_all).isVisible = false
        }

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
            KEY_SDCARD -> {
                var sdCardPath = config.sdCardPath
                if (sdCardPath.isBlank()) {
                    sdCardPath = getSDCardPath()
                    config.sdCardPath = sdCardPath
                }
                refreshRecyclerView(sdCardPath)
                mRecentDirectory = sdCardPath
            }
            R.id.action_download -> launchActivity(DownloadActivity::class.java)
            R.id.action_sort_alpha -> sortBy(0)
            R.id.action_sort_last_modified -> sortBy(SORT_BY_DATE_MODIFIED)
            R.id.action_sort_size -> sortBy(SORT_BY_SIZE)
            R.id.action_translator -> launchActivity(TranslatorActivity::class.java)
            R.id.action_refresh -> refreshRecyclerView()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sortBy(sortOrder: Int) {
        // Sort the file list in the specified way
        mSortOrder = sortOrder
        // Refresh display after sorting
        refreshRecyclerView()
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

                    tryOpenPathIntent(path, false)

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
        private const val KEY_SDCARD = 1

        private const val MENU_COPY = 11
        private const val MENU_DELELTE = 12
        private const val MENU_SPLIT_VIDEO = 13

    }

}