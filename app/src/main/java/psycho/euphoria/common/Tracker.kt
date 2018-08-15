package psycho.euphoria.common
import android.os.Handler
import android.util.Log
class Tracker(private val tag: String) {
    private val mHandler = Handler()
    private val mCollection = ArrayList<String>()
    private val mDumpAction = Runnable {
        Log.e(tag, this.toString())
        dump()
    }
    var enable = true
        set(value) {
            if (!value) {
                mHandler.removeCallbacks(mDumpAction)
                mCollection.clear()
            } else {
                mHandler.removeCallbacks(mDumpAction)
                mHandler.postDelayed(mDumpAction, 5000L)
            }
            field = value
        }
    init {
        dump()
    }
    fun e(message: String) {
        if(enable)
        mCollection.add(message)
    }
    private fun dump() {
        mHandler.postDelayed(mDumpAction, 10000L)
    }
    override fun toString(): String {
        val sb = StringBuilder()
        var index = 0
        for (c in mCollection) {
            sb.append("${++index} $c \n")
        }
        return sb.toString()
    }
}