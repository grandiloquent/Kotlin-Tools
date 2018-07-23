package psycho.euphoria.tools.files

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_file.*
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.FileItem
import psycho.euphoria.tools.commons.formatSize
import psycho.euphoria.tools.commons.getColoredDrawableWithColor

class FileAdapter(private val context: Context,
                  private val files: ArrayList<FileItem>,
                  private val itemClick: (FileItem) -> Unit) :
        RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    private val mFileDrawble: Drawable
    private val mFolderDrawable: Drawable
    private val mOptions: RequestOptions

    init {


        mFileDrawble = context.resources.getColoredDrawableWithColor(R.mipmap.ic_file, TEXT_COLOR.toInt())
        mFolderDrawable = context.resources.getColoredDrawableWithColor(R.mipmap.ic_folder, TEXT_COLOR.toInt())
        mOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(mFileDrawble)
    }

    fun switchData(list: List<FileItem>) {
        files.clear()
        files.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bindFileItem(files[position])
    }

    fun getItem(position: Int): FileItem {
        return files[position]
    }

    companion object {
        private const val TEXT_COLOR = 0XFF333333
    }

    inner class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            itemView.isLongClickable = true
        }

        fun bindFileItem(fileItem: FileItem) {
            itemView.setOnClickListener { itemClick(fileItem) }
            with(fileItem) {
                item_name.text = name
                if (isDirectory) {
                    item_icon.setImageDrawable(mFolderDrawable)
                    item_details.text = "0"

//                    item_details.post {
//                        item_details.text = "${File(fileItem.path).listFiles().size}"
//                    } // maybe cause some performance problem
                } else {
                    item_details.text = size.formatSize()
                    var itemToLoad = if (fileItem.name.endsWith(".apk", true)) {
                        val packageManager = context.packageManager
                        val packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
                        if (packageInfo != null) {
                            val appInfo = packageInfo.applicationInfo.apply {
                                sourceDir = path
                                publicSourceDir = path

                            }
                            appInfo.loadIcon(packageManager)
                        } else {
                            path
                        }
                    } else {
                        path
                    }
                    Glide.with(context).load(itemToLoad).transition(DrawableTransitionOptions.withCrossFade()).apply(mOptions).into(item_icon)

                }
            }
        }
    }
}

