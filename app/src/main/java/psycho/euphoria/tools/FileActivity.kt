package psycho.euphoria.tools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import psycho.euphoria.tools.commons.KEY_PATH
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import psycho.euphoria.tools.MainActivity.Companion.TYPE_MUSIC
import psycho.euphoria.tools.MainActivity.Companion.TYPE_PICTURE
import psycho.euphoria.tools.videos.VideoActivity
import java.io.File

class FileActivity : AppCompatActivity() {
    private lateinit var mListView: ListView
    private lateinit var mFileListAdapter: FileListAdapter
    private var mCurrentDirectory: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file)
        mCurrentDirectory = baseConfig.accessedDirectory

        var files = File(mCurrentDirectory).listFilesOrderly()
        if (files.isEmpty()) {
            mCurrentDirectory = Environment.getExternalStorageDirectory().absolutePath
            files = File(mCurrentDirectory).listFilesOrderly()
        }
        mFileListAdapter = FileListAdapter(this, ArrayList<File>().also {
            it.addAll(files)
        })
        mListView = findViewById(R.id.listView)
        mListView.adapter = mFileListAdapter
        mListView.setOnItemClickListener { _, _, p, _ ->
            val file = mFileListAdapter.getItem(p)
            if (file.isDirectory) {
                mCurrentDirectory = file.absolutePath
                mFileListAdapter.switchData(file.listFilesOrderly())
            } else {
                if (intent.getBooleanExtra(TYPE_PICTURE, false) && file.isImage()) {
                    val intent = Intent(this, PictureActivity::class.java)
                    intent.putExtra(KEY_DIRECTORY, file.absolutePath)
                    startActivity(intent)
                } else if (file.name.endsWith(".mp4")) {
                    val intent = Intent(this, VideoActivity::class.java)
                    intent.putExtra(KEY_PATH, file.absolutePath)
                    startActivity(intent)
                }
            }
        }
        registerForContextMenu(mListView)
    }

    override fun onBackPressed() {
        val parent = File(mCurrentDirectory).parentFile
        if (parent != null) {
            val files = parent.listFilesOrderly()
            if (files != null && files.isNotEmpty()) {
                mFileListAdapter.switchData(files)
                mCurrentDirectory = parent.absolutePath
                return
            }
        }
        super.onBackPressed()
    }

    override fun onPause() {
        mCurrentDirectory?.let {
            baseConfig.accessedDirectory = it
        }
        super.onPause()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (intent.getBooleanExtra(MainActivity.TYPE_PICTURE, false)) {
            menu?.add(0, MENU_BROWSER_PICTURE, 0, "浏览图片")
        } else if (intent.getBooleanExtra(TYPE_MUSIC, false)) {
            menu?.add(0, MENU_PLAY_MUSIC, 0, "播放音乐")
        }
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val menuInfo = item?.menuInfo as AdapterView.AdapterContextMenuInfo
        val file = mFileListAdapter.getItem(menuInfo.position)
        when (item?.itemId) {
            MENU_BROWSER_PICTURE -> {
                if (file.isDirectory) {
                    val intent = Intent(this, PictureActivity::class.java)
                    intent.putExtra(KEY_DIRECTORY, file.absolutePath)
                    startActivity(intent)
                }
            }
            MENU_PLAY_MUSIC -> {
                val intent = Intent(this, MediaPlaybackService::class.java)
                if (file.isDirectory) {
                    intent.putExtra(MediaPlaybackService.KEY_PLAY_DIRECTORY, file.absolutePath)
                    intent.putExtra(MediaPlaybackService.KEY_PLAY_POSITION, 0)
                } else {
                    intent.putExtra(MediaPlaybackService.KEY_PLAY_DIRECTORY, file.parentFile.absolutePath)
                    intent.putExtra(MediaPlaybackService.KEY_PLAY_POSITION, 0)
                }
                startService(intent)
            }
        }
        return true
    }

    companion object {
        const val KEY_DIRECTORY = "directory"
        const val MENU_BROWSER_PICTURE = 1
        const val MENU_SPLIT_VIEDO = 2
        const val MENU_SPLIT_VIEDO_EVEN = 3
        const val MENU_PLAY_MUSIC = 5
    }

    class FileListAdapter(private val context: Context,
                          val files: ArrayList<File>) : BaseAdapter() {
        fun switchData(list: List<File>) {
            files.clear()
            files.addAll(list)
            notifyDataSetChanged()
        }

        override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
            val viewHolder: ViewHolder
            var view = v;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
                viewHolder = ViewHolder(view.findViewById(R.id.textView))
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            viewHolder.textView.text = files[p].name
            return view!!
        }

        override fun getItem(p0: Int): File = files[p0]
        override fun getItemId(p0: Int): Long = p0.toLong()
        override fun getCount(): Int = files.size
    }

    data class ViewHolder(val textView: TextView)
}