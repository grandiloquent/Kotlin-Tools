package psycho.euphoria.tools
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.github.chrisbanes.photoview.PhotoView
import java.io.File
class PictureActivity : AppCompatActivity() {
    private var mFileAdapter: FileAdapter? = null
    private lateinit var mViewPager: ViewPager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        val filePath = intent.getStringExtra(FileActivity.KEY_DIRECTORY)
        if (filePath == null) finish()
        val f = File(filePath)
        val files = f.listImagesRecursively()
        mFileAdapter = FileAdapter(this, files)
        mViewPager = findViewById<ViewPager>(R.id.viewPager)
        mViewPager.adapter = mFileAdapter
        if (f.isFile && files.size > 0) {
            mViewPager.currentItem = files.indexOf(f)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            add(0, MENU_SHARE, 0, "分享图片")
        }
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            MENU_SHARE -> mFileAdapter?.getItem(mViewPager.currentItem)?.share(this)
        }
        return true
    }
    companion object {
        private const val MENU_SHARE = 1
    }
    class FileAdapter(private val context: Context, val files: List<File>) : PagerAdapter() {
        override fun getCount(): Int = files.size
        fun getItem(position: Int): File {
            return files[position]
        }
        override fun instantiateItem(container: ViewGroup, position: Int): View {
            val photoView = PhotoView(context)
            try {
                photoView.setImageDrawable(Drawable.createFromPath(files[position].absolutePath))
            } catch (ex: Exception) {
            }
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            return photoView
        }
        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }
        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }
    }
}