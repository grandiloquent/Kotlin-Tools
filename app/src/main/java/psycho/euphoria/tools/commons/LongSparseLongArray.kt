package psycho.euphoria.tools.commons
class LongSparseLongArray(initialCapacity: Int) : Cloneable {
    private var mKeys: LongArray
    private var mValues: LongArray
    private var mSize: Int = 0
    constructor() : this(10)
    init {
        if (initialCapacity == 0) {
            mKeys = LongArray(0)
            mValues = LongArray(0)
        } else {
            mKeys = LongArray(initialCapacity)
            mValues = LongArray(initialCapacity)
        }
        mSize = 0
    }
    public override fun clone(): LongSparseLongArray {
        var clone: LongSparseLongArray? = null
        try {
            clone = super.clone() as LongSparseLongArray
            clone.mKeys = mKeys.clone()
            clone.mValues = mValues.clone()
        } catch (cnse: CloneNotSupportedException) {
        }
        return clone!!
    }
    operator fun get(key: Long): Long {
        return get(key, 0)
    }
    operator fun get(key: Long, valueIfKeyNotFound: Long): Long {
        val i = binarySearch(mKeys, mSize, key)
        return if (i < 0) {
            valueIfKeyNotFound
        } else {
            mValues[i]
        }
    }
    fun delete(key: Long) {
        val i = binarySearch(mKeys, mSize, key)
        if (i >= 0) {
            removeAt(i)
        }
    }
    fun removeAt(index: Int) {
        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1))
        System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1))
        mSize--
    }
    fun put(key: Long, value: Long) {
        var i = binarySearch(mKeys, mSize, key)
        if (i >= 0) {
            mValues[i] = value
        } else {
            i = i.inv()
            mKeys = insert(mKeys, mSize, i, key)
            mValues = insert(mValues, mSize, i, value)
            mSize++
        }
    }
    fun size(): Int {
        return mSize
    }
    fun keyAt(index: Int): Long {
        return mKeys[index]
    }
    fun valueAt(index: Int): Long {
        return mValues[index]
    }
    fun indexOfKey(key: Long): Int {
        return binarySearch(mKeys, mSize, key)
    }
    fun indexOfValue(value: Long): Int {
        for (i in 0 until mSize)
            if (mValues[i] == value)
                return i
        return -1
    }
    fun clear() {
        mSize = 0
    }
    fun append(key: Long, value: Long) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value)
            return
        }
        mKeys = append(mKeys, mSize, key)
        mValues = append(mValues, mSize, value)
        mSize++
    }
    override fun toString(): String {
        if (size() <= 0) {
            return "{}"
        }
        val buffer = StringBuilder(mSize * 28)
        buffer.append('{')
        for (i in 0 until mSize) {
            if (i > 0) {
                buffer.append(", ")
            }
            val key = keyAt(i)
            buffer.append(key)
            buffer.append('=')
            val value = valueAt(i)
            buffer.append(value)
        }
        buffer.append('}')
        return buffer.toString()
    }
}