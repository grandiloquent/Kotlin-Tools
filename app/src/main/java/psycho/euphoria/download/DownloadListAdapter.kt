package psycho.euphoria.download
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_download.*
import psycho.euphoria.tools.R
class DownloadListAdapter(private val downloads: MutableList<Request>,
                          private val itemClick: (Request) -> Unit,
                          private val itemDrag: (ViewHolder) -> Unit) :
        RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view, itemClick)
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
    class ViewHolder(override val containerView: View,
                     private val itemClick: (Request) -> Unit) :
            RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindDownloadInfo(downloadInfo: Request) {
            with(downloadInfo) {
                file_name.text = downloadInfo.fileName
                itemView.setOnClickListener { itemClick(this) }
            }
        }
    }
}