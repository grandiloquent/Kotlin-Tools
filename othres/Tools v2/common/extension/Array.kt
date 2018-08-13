package psycho.euphoria.common.extension

import java.util.*

fun <T> Array<T>?.dump(): String? {
    if (this != null && this.isEmpty()) {
        val sb = StringBuilder()
        for (i in 0 until this.size) {
            sb.append(i.toString()).append(",")
        }
        return sb.toString()
    } else
        return null

}

fun IntArray?.dump(): String? {
    if (this != null && this.isEmpty()) {
        val sb = StringBuilder()
        for (i in 0 until this.size) {
            sb.append(i.toString()).append(",")
        }
        return sb.toString()
    } else
        return null

}
/**
 * Returns the index of the largest element in {@code array} that is less than (or optionally
 * equal to) a specified {@code value}.
 * <p>
 * The search is performed using a binary search algorithm, so the array must be sorted. If the
 * array contains multiple elements equal to {@code value} and {@code inclusive} is true, the
 * index of the first one will be returned.
 *
 * @param array The array to search.
 * @param value The value being searched for.
 * @param inclusive If the value is present in the array, whether to return the corresponding
 *     index. If false then the returned index corresponds to the largest element strictly less
 *     than the value.
 * @param stayInBounds If true, then 0 will be returned in the case that the value is smaller than
 *     the smallest element in the array. If false then -1 will be returned.
 * @return The index of the largest element in {@code array} that is less than (or optionally
 *     equal to) {@code value}.
 */
fun IntArray.binarySearchFloor(value: Int, inclusive: Boolean,
                               stayInBounds: Boolean): Int {
    var index = Arrays.binarySearch(this, value)
    if (index < 0) {
        index = -(index + 2)
    } else {
        while (--index >= 0 && this[index] == value) {
        }
        if (inclusive) {
            index++
        }
    }
    return if (stayInBounds) Math.max(0, index) else index
}
