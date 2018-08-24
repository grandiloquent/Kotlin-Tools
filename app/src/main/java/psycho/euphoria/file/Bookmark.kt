package psycho.euphoria.file

import android.app.AlertDialog
import android.content.Context
import android.os.Environment
import android.widget.ArrayAdapter
import psycho.euphoria.common.extension.getFilenameFromPath
import psycho.euphoria.common.extension.sdCardPath
import psycho.euphoria.tools.R
import java.io.File

class Bookmark(private val context: Context) {
    private val mCacheFile by lazy {
        File(Environment.getExternalStorageDirectory(), Bookmark.BOOKMARK_CACHE_FILE)
    }
    private var mCollection = ArrayList<String>()

    init {
        if (!mCacheFile.exists()) {
            mCollection.add(Environment.getExternalStorageDirectory().absolutePath)
            mCollection.add(context.sdCardPath)
        }
    }

    fun put(dir: File) {
        if (!dir.exists()) return
        val path = dir.absolutePath;
        if (mCollection.any { it.equals(path) }) return
        mCollection.add(path)
        val stringBuilder = StringBuilder()
        mCollection.forEach { stringBuilder.append(it).append('\n') }
        mCacheFile.writeText(stringBuilder.toString())
    }

    private fun list(): List<String>? {
        if (mCollection.size <= 2 && mCacheFile.exists()) {

            mCollection.addAll(mCacheFile.readLines().toMutableList())

        }

        return mCollection.map { it.getFilenameFromPath() };
    }

    fun show(callback: (String) -> Unit) {
        val adapter = ArrayAdapter<String>(context, R.layout.item_sorting, R.id.text_view)
        adapter.addAll(list())
        val dialog = AlertDialog.Builder(context)
                .setAdapter(adapter) { dialog, position ->
                    callback(mCollection[position])
                }.create()
        dialog.show()
    }

    companion object {
        private const val BOOKMARK_CACHE_FILE = "bookmark_file.txt"
    }
}