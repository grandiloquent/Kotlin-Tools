package psycho.euphoria.file

import android.support.v7.util.DiffUtil

class FileItemDifferenceCallback(private val oldItems: ArrayList<FileItem>,
                                 private val newItems: ArrayList<FileItem>) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldItems[oldItemPosition].equals(newItems[newItemPosition])
    override fun getOldListSize() = oldItems.size
    override fun getNewListSize() = newItems.size
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldItems[oldItemPosition].equals(newItems[newItemPosition])
}