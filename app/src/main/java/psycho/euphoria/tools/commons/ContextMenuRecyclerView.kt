package psycho.euphoria.tools.commons

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
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