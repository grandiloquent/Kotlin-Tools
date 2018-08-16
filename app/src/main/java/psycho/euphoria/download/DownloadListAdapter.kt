package psycho.euphoria.download

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_download.*
import psycho.euphoria.common.extension.getFilenameFromPath
import psycho.euphoria.tools.R
import java.util.*

class DownloadListAdapter(private val downloads: MutableList<Request>,
                          private val itemClick: (Request) -> Unit,
                          val menuItemListener: OnMenuItemClickListener) :
        RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view, this, itemClick)
    }

    fun switchData(downloadList: List<Request>) {
        downloads.clear()
        downloads.addAll(downloadList)
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        downloads.removeAt(position)
        notifyDataSetChanged()
    }

    fun removeAt(viewHolder: RecyclerView.ViewHolder) {
        downloads.removeAt(viewHolder.adapterPosition)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bindDownloadInfo(downloads[position])
    }

    fun getItem(position: Int): Request {
        return downloads.get(position)
    }

    override fun getItemCount(): Int {
        return downloads.size
    }

    interface OnMenuItemClickListener {
        fun onMenuItemClick(position: Int, item: MenuItem)
    }

    companion object {
        const val MENU_STOP = 0
    }

    class ViewHolder(override val containerView: View,
                     private val adapter: DownloadListAdapter,
                     private val itemClick: (Request) -> Unit) :
            RecyclerView.ViewHolder(containerView), LayoutContainer {
        init {
            file_menu.setOnClickListener { it.post { showPopMenu(it) } }
        }

        fun bindDownloadInfo(downloadInfo: Request) {
            with(downloadInfo) {
                file_name.text = downloadInfo.fileName.getFilenameFromPath()
                itemView.setOnClickListener { itemClick(this) }
            }
        }

        private fun showPopMenu(view: View) {
            val item = adapter.getItem(adapterPosition) ?: return

            val popupMenu = PopupMenu(view.context, view)

            val locale = Locale.getDefault()

            val stopMenuTitle = if (locale == Locale.CHINA) "停止" else "Stop"

            popupMenu.menu.add(0, MENU_STOP, 0, stopMenuTitle)

            popupMenu.setOnMenuItemClickListener {
                adapter.menuItemListener.onMenuItemClick(adapterPosition, it)
                true
            }
            popupMenu.show()
        }
    }
}