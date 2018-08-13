package psycho.euphoria.download

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_download.*
import psycho.euphoria.common.Services.clipboardManager
import psycho.euphoria.common.extension.isValidURL
import psycho.euphoria.common.extension.toast
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.*
import java.io.File

class DownloadActivity: AppCompatActivity() {

    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private lateinit var mAdapter: DownloadListAdapter

    private lateinit var mOnPrimaryClipChangedListener: ClipboardManager.OnPrimaryClipChangedListener

    private lateinit var mClipboardManager: ClipboardManager

    private lateinit var mItemTouchHelper: ItemTouchHelper

    private fun addDownloadTask() {
        dialog(this, "", resources.getString(R.string.menu_add_download_task)) { value ->
            value?.let {
                insertDownloadTask(it.toString())
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    private fun initialize() {
        setContentView(R.layout.activity_download)
        mLayoutManager = LinearLayoutManager(this)



        mAdapter = DownloadListAdapter(DownloadTaskProvider.getInstance().listTasks(), fun(v) {
            Tracker.e("DownloadListAdapter", "downloadInfo => ${v.id}")
        }, fun(v) {
            //mItemTouchHelper.startDrag(v)
        })

        recycler_view.apply {
            setHasFixedSize(true)
            adapter = mAdapter
            layoutManager = mLayoutManager
        }
        mOnPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            val primaryClip = clipboardManager.primaryClip
            if (primaryClip.itemCount > 0) {
                val text = primaryClip.getItemAt(0).text
                text?.let {
                    if (it.toString().isValidURL()) {
                        insertDownloadTask(it.toString())
                        toast("Insert Download Task from Clipboard.")
                    }
                }
            }
        }




        mItemTouchHelper = ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT
                        or ItemTouchHelper.RIGHT) {
                    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {

                        return true
                    }

                    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
                        return super.getMovementFlags(recyclerView, viewHolder)
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                        Tracker.e("onSwiped", "direction => $direction")
                        viewHolder?.let {
                            val downloadInfo = mAdapter.getItem(it.adapterPosition)
                            downloadInfo.finish = 1
                            DownloadTaskProvider.getInstance().update(downloadInfo.id)
                            mAdapter.removeAt(it)
                        }
                    }

                })
        Tracker.e("onCreate", "attach ItemTouchHelper to recyclerView")
        mItemTouchHelper.attachToRecyclerView(recycler_view)
    }

    private fun insertDownloadTask(url: String) {
        if (!url.isBlank() && url.isValidURL()) {
            DownloadTaskProvider.getInstance().insert(url, generateFileNameFromURL(url, Environment.getExternalStorageDirectory()))
            refreshRecyclerView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
        // Ensure that the same object registers and releases the event listener for the clipboard change, otherwise the memory will leak
        mClipboardManager = clipboardManager
        mClipboardManager.addPrimaryClipChangedListener(mOnPrimaryClipChangedListener)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        with(menu) {
            add(0, MENU_ADD_DOWNLOAD, 0, resources.getString(R.string.menu_add_download_task))
            add(0, MENU_START_DOWNLOAD, 0, resources.getString(R.string.menu_strat_download_service))
        }
        return super.onCreateOptionsMenu(menu)
    }


    override fun onDestroy() {
        mClipboardManager.removePrimaryClipChangedListener(mOnPrimaryClipChangedListener)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ADD_DOWNLOAD -> addDownloadTask()
            MENU_START_DOWNLOAD -> startDownload()
            else -> return true
        }
        return true
    }

    private fun refreshRecyclerView() {
        mAdapter.switchData(DownloadTaskProvider.getInstance().listTasks())
    }

    private fun startDownload() {
        val intent = Intent(this, DownloadService::class.java)
        startService(intent)
    }

    companion object {
        private const val MENU_ADD_DOWNLOAD = 1
        private const val MENU_START_DOWNLOAD = 2

        fun generateFileNameFromURL(url: String, directory: File): String {
            if (url.isBlank()) {
                var file = File(directory, ('a'..'z').randomString(6))
                while (file.exists()) {
                    file = File(directory, ('a'..'z').randomString(6))
                }
                return file.absolutePath
            } else {
                var fileName = url.substringBefore('?')
                var invalidFileNameChars = arrayOf('\"', '<', '>', '|', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, ':', '*', '?', '\\', '/')
                fileName = fileName.substringAfterLast('/').filter {
                    !invalidFileNameChars.contains(it)
                }
                return File(directory, fileName).absolutePath
            }
        }

    }
}