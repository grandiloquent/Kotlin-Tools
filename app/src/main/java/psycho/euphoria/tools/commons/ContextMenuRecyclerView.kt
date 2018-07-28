package psycho.euphoria.tools.commons

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseBooleanArray
import android.view.ContextMenu
import android.view.View

class ContextMenuRecyclerView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mContextMenuInfo: ContextMenuInfo? = null


    override fun getContextMenuInfo(): ContextMenuInfo? {
        return mContextMenuInfo
    }

    private fun saveContextMenuInfo(originalView: View) {
        val position = getChildAdapterPosition(originalView)
        val longId = getChildItemId(originalView)
        mContextMenuInfo = ContextMenuInfo(this, originalView, position, longId)
    }

    override fun showContextMenuForChild(originalView: View): Boolean {
        saveContextMenuInfo(originalView)
        return super.showContextMenuForChild(originalView)
    }

    override fun showContextMenuForChild(originalView: View, x: Float, y: Float): Boolean {
        saveContextMenuInfo(originalView)
        return super.showContextMenuForChild(originalView, x, y)
    }

    class ContextMenuInfo(
            val recyclerView: RecyclerView,
            val itemView: View,
            val position: Int,
            val id: Long
    ) : ContextMenu.ContextMenuInfo

}

class MultiSelector {
    val mSelections = SparseBooleanArray()
    val mTracker = WeakHolderTracker()
    var isSelectable = true
        set(value) {
            field = value
            refreshAllHolders()
        }

    fun refreshAllHolders() {
        for (holder in mTracker.getTrackedHolders()) refreshHolder(holder)
    }

    fun refreshHolder(holder: SelectableHolder) {
        if (holder == null) return
        holder.isSelectable = isSelectable
        holder.isActivated = mSelections.get(holder.getPosition())
    }

    fun isSelected(position: Int, id: Long): Boolean {
        return mSelections.get(position)
    }

    fun setSelected(position: Int, id: Long, isSelected: Boolean) {
        mSelections.put(position, isSelected)
        refreshHolder(mTracker.getHolder(position))
    }

    fun clearSelection() {
        mSelections.clear()
        refreshAllHolders()
    }

    fun getSelectedPosition(): ArrayList<Int> {
        val positions = ArrayList<Int>()
        for (i in 0 until mSelections.size()) {
            if (mSelections.valueAt(i)) {
                positions.add(mSelections.keyAt(i))
            }
        }
        return positions
    }
}

class WeakHolderTracker {
    fun getTrackedHolders(): List<SelectableHolder> {
        return ArrayList<SelectableHolder>()
    }

    fun getHolder(position: Int): SelectableHolder {
        return SelectableHolder()
    }
}

class SelectableHolder {
    var isSelectable = true
    var isActivated = true

    fun getPosition(): Int {
        return 0
    }
}