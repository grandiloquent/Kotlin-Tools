package psycho.euphoria.file

import android.text.TextUtils
import android.util.Log
import psycho.euphoria.common.Services

data class FileItem(val path: String,
                    val name: String,
                    val size: Long,
                    val count: Int,
                    val isDirectory: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is FileItem) {
            Log.i(Services.TAG, "[equals]:${TextUtils.equals(path, other.path) && size == other.size} ")
            return TextUtils.equals(path, other.path) && size == other.size
        } else return false
    }

    override fun hashCode(): Int {
        /*
        According to Joshua Bloch's Effective Java (a book that can't be recommended enough, and which I bought thanks to continual mentions on stackoverflow):

The value 31 was chosen because it is an odd prime. If it were even and the multiplication overflowed, information would be lost, as multiplication by 2 is equivalent to shifting. The advantage of using a prime is less clear, but it is traditional. A nice property of 31 is that the multiplication can be replaced by a shift and a subtraction for better performance: 31 * i == (i << 5) - i. Modern VMs do this sort of optimization automatically.
         */

        var result = 61
        result = 31 * result + path.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}