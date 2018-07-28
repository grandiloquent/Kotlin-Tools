package psycho.euphoria.tools.files

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
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
import psycho.euphoria.tools.commons.*
import psycho.euphoria.tools.commons.ui.MultiSelector
import psycho.euphoria.tools.commons.ui.SwappingHolder

class FileAdapter(private val activity: AppCompatActivity,
                  private val files: ArrayList<FileItem>,
                  private val multiSelector: MultiSelector,
                  private val actionMode: ActionMode.Callback,
                  private val itemClick: (FileItem?) -> Unit
) :
        RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    private val mFileDrawble: Drawable = activity.resources.getDrawable(R.drawable.ic_insert_drive_file_48px)
    private val mFolderDrawable: Drawable = activity.resources.getDrawable(R.drawable.ic_folder_48px)
    private val mPdfDrawble: Drawable = activity.resources.getDrawable(R.drawable.ic_file_pdf)
    private val mAudioDrawable: Drawable = activity.resources.getDrawable(R.drawable.ic_file_audio)
    private val mArchiveDrawable: Drawable = activity.resources.getDrawable(R.drawable.ic_file_archive)
    private val mOptions: RequestOptions // Glide option


    init {


        //context.resources.getColoredDrawableWithColor(R.mipmap.ic_file, TEXT_COLOR.toInt())
        //context.resources.getColoredDrawableWithColor(R.mipmap.ic_folder, TEXT_COLOR.toInt())
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

        val view = LayoutInflater.from(activity).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view, multiSelector, activity, itemClick, actionMode)
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

    inner class ViewHolder(override val containerView: View,
                           val multiSelector: MultiSelector,
                           val activity: AppCompatActivity,
                           val selectFileItem: (FileItem?) -> Unit,
                           val actionMode: ActionMode.Callback) : SwappingHolder(containerView, multiSelector),
            View.OnClickListener,
            View.OnLongClickListener,
            LayoutContainer {

        private var mFileItem: FileItem? = null
        override fun onLongClick(p0: View?): Boolean {
            activity.startSupportActionMode(actionMode)
            multiSelector.setSelected(this, true)
            return true
        }

        override fun onClick(view: View?) {
            mFileItem?.let {
                if (!multiSelector.tapSelection(this)) {
                    selectFileItem(mFileItem)
                }
            }
        }

        init {
            itemView.isLongClickable = true
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bindFileItem(fileItem: FileItem) {
            mFileItem = fileItem
            with(fileItem) {
                item_name.text = name
                if (isDirectory) {
                    item_icon.setImageDrawable(mFolderDrawable)
                    item_details.text = "$count item"
                } else {
                    item_details.text = size.formatSize()

                    when {
                        name.endsWith(".pdf", true) -> item_icon.setImageDrawable(mPdfDrawble)
                        name.isArchiveFast() -> item_icon.setImageDrawable(mArchiveDrawable)
                        name.isAudioFast() -> item_icon.setImageDrawable(mAudioDrawable)
                        name.isImageFast() or name.isVideoFast() or name.endsWith(".apk", true) -> {
                            // If the file type is a picture, video or Android app installation package, try to get the thumbnail
                            // The way to determine the file type is to compare the extension of the file, so it is not very reliable
                            var itemToLoad = if (fileItem.name.endsWith(".apk", true)) {
                                val packageManager = activity.packageManager
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
                            Glide.with(activity).load(itemToLoad).transition(DrawableTransitionOptions.withCrossFade()).apply(mOptions).into(item_icon)
                        }
                        else -> {
                            item_icon.setImageDrawable(mFileDrawble)
                        }
                    }


                    // Load images with Glide


                }
            }
        }
    }
}

