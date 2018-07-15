package psycho.euphoria.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NumberPicker(context: Context, attributeSet: AttributeSet) :
        LinearLayout(context, attributeSet) {

    private val mSelectedText: EditText
    private val mSelectorWheelPaint: Paint
    private var mBottomSelectionDividerBottom = 0
    private var mCurrentScrollOffset = 0.0f
    private var mInitializeScrollOffset = Integer.MIN_VALUE
    private var mLeftOfSelectionDividerLeft = 0
    private var mMaxHeight = 0
    private var mMaxWidth = 0
    private var mMinHeight = 0
    private var mMinWidth = 0
    private var mRealWheelItemCount = DEFAULT_WHEEL_ITEM_COUNT
    private var mRightOfSelectionDividerRight = 0
    private var mSelectedTextSize = 0.0f
    private var mSelectionDivider: Drawable? = null
    var selectionDividerThickness = 0
        set(value) {
            field = context.dp2px(value.toFloat()).toInt()
        }
    private var mSelectorElementSize = 0.0f
    private var mSelectorIndices = IntArray(DEFAULT_WHEEL_ITEM_COUNT)
    private var mTopSelectionDividerTop = 0
    private var mWheelItemCount = DEFAULT_WHEEL_ITEM_COUNT
    private var mWheelMiddleItemIndex = 0
    val mSelectorIndexToStringCache = SparseArray<String>()
    var order = ASCENDING
    var selectedTextColor = 0xFF000000
    var textColor = 0xFF000000
    var textSize = 25f
    private var mSelectedTextCenterX = 0.0f
    private var mSelectedTextCenterY = 0.0f
    private var mSelectionDividersDistance = 0


    init {

        LayoutInflater.from(context).inflate(R.layout.number_picker_with_selector_wheel, this, true)

        mSelectionDivider = context.getDrawableCompat(R.mipmap.np_numberpicker_selection_divider)

        mSelectedText = findViewById<EditText>(R.id.np__numberpicker_input).apply {
            isEnabled = false
            isFocusable = false
            imeOptions = EditorInfo.IME_ACTION_NONE
            textColor = selectedTextColor

        }


        mSelectorWheelPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER

        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        val mw = measuredWidth
        val mh = measuredHeight
        val smw = mSelectedText.measuredWidth
        val smh = mSelectedText.measuredHeight
        val tl = (mw - smw) / 2
        val tt = (mh - smh) / 2
        val tr = tl + smw
        var tb = tt + smh
        mSelectedText.layout(tl, tt, tr, tb)
        mSelectedTextCenterX = mSelectedText.x + mSelectedText.measuredWidth / 2
        mSelectedTextCenterY = mSelectedText.y + mSelectedText.measuredHeight / 2

        "onLayout".e(TAG, "$tl,$tt,$tr,$tb")
        if (changed) {

            if (isHorizontalMode()) {
                mLeftOfSelectionDividerLeft = (width - mSelectionDividersDistance) / 2 - selectionDividerThickness
                mRightOfSelectionDividerRight = mLeftOfSelectionDividerLeft + 2 * selectionDividerThickness + mSelectionDividersDistance
            } else {
                mTopSelectionDividerTop = (height - mSelectionDividersDistance) / 2 - selectionDividerThickness
                mBottomSelectionDividerBottom = mTopSelectionDividerTop + 2 * selectionDividerThickness + mSelectionDividersDistance
            }
        }

    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            var xl: Float
            var yl: Float
            if (isHorizontalMode()) {
                xl = mCurrentScrollOffset
                yl = (mSelectedText.baseline + mSelectedText.top).toFloat()
                if (mRealWheelItemCount < DEFAULT_WHEEL_ITEM_COUNT) {
                    it.clipRect(mLeftOfSelectionDividerLeft, 0, mRightOfSelectionDividerRight, bottom)
                }
            } else {
                xl = (right - left) / 2.0f
                yl = mCurrentScrollOffset
                if (mRealWheelItemCount < DEFAULT_WHEEL_ITEM_COUNT)
                    it.clipRect(0, mTopSelectionDividerTop, right, mBottomSelectionDividerBottom)
            }
            "onDraw".e(TAG, "$xl,$yl ${isHorizontalMode()}")
            val selectorIndices = getSelectorIndices()
            for (i in selectorIndices.indices) {
                if (i == mWheelMiddleItemIndex) {
                    mSelectorWheelPaint.textSize = mSelectedTextSize
                    mSelectorWheelPaint.color = selectedTextColor.toInt()
                } else {
                    mSelectorWheelPaint.textSize = textSize
                    mSelectorWheelPaint.color = textColor.toInt()
                }
                val selectorIndex = selectorIndices[if (isAscendingOrder()) i else selectorIndices.size - i - 1]
                val scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex)
                if (mWheelMiddleItemIndex != i || mSelectedText.visibility != View.VISIBLE) {
                    if (isHorizontalMode()) {
                        it.drawText(scrollSelectorValue, xl, yl, mSelectorWheelPaint)
                    } else {
                        it.drawText(scrollSelectorValue, xl, yl + getPaintCenterY(mSelectorWheelPaint.fontMetrics), mSelectorWheelPaint)
                    }
                }
                if (isHorizontalMode()) {
                    xl += mSelectorElementSize
                } else {
                    yl += mSelectorElementSize
                }
            }
            it.restore()
            if (mSelectionDivider != null) {
                if (isHorizontalMode()) {
                    "onDraw".e(TAG, "isHorizontalMode")
                    val leftOfLeftDivider = mLeftOfSelectionDividerLeft
                    val rightOfLeftDivider = leftOfLeftDivider + selectionDividerThickness
                    mSelectionDivider?.setBounds(leftOfLeftDivider, 0, rightOfLeftDivider, bottom)
                    mSelectionDivider?.draw(it)

                    val rightOfRightDivider = mRightOfSelectionDividerRight
                    val leftOfRightDivider = rightOfLeftDivider -selectionDividerThickness
                    mSelectionDivider?.setBounds(leftOfRightDivider, 0, rightOfRightDivider, bottom)
                    mSelectionDivider?.draw(canvas)
                } else {
                    val tt = mTopSelectionDividerTop
                    val bt = tt + selectionDividerThickness
                    mSelectionDivider?.setBounds(0, tt, right, bt)
                    mSelectionDivider?.draw(it)

                    val bb = mBottomSelectionDividerBottom
                    val tb = bb -selectionDividerThickness
                    mSelectionDivider?.setBounds(0, tb, right, bb)
                    mSelectionDivider?.draw(it)
                }
            }

        }
    }

    fun getPaintCenterY(fontMetrics: Paint.FontMetrics): Float {
        return abs(fontMetrics.top + fontMetrics.bottom) / 2
    }

    fun getSelectorIndices(): IntArray {
        return mSelectorIndices;
    }

    fun isAscendingOrder(): Boolean {
        return order == ASCENDING;
    }

    fun isHorizontalMode(): Boolean {
        return orientation == HORIZONTAL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mw = makeMeasureSpec(widthMeasureSpec, mMaxWidth)
        val mh = makeMeasureSpec(heightMeasureSpec, mMaxHeight)
        super.onMeasure(mw, mh)
        val w = resolveSizeAndStateRespectingMinSize(mMinWidth, measuredWidth, widthMeasureSpec)
        val h = resolveSizeAndStateRespectingMinSize(mMinHeight, measuredHeight, heightMeasureSpec)
        setMeasuredDimension(w, h)

        "onMeasure".e(TAG, "$w x $h")
    }

    fun resolveSizeAndStateRespectingMinSize(minSize: Int, measuredSize: Int, measureSpec: Int): Int {
        if (minSize != SIZE_UNSPECIFIED) {
            val desiredWidth = max(minSize, measuredSize)
            return View.resolveSizeAndState(desiredWidth, measureSpec, 0)
        } else return measuredSize
    }


    fun makeMeasureSpec(measureSpec: Int, maxSize: Int): Int {
        if (maxSize == SIZE_UNSPECIFIED) return measureSpec

        val size = MeasureSpec.getSize(measureSpec)
        val mode = MeasureSpec.getMode(measureSpec)

        when (mode) {
            MeasureSpec.EXACTLY -> return measureSpec
            MeasureSpec.AT_MOST -> return MeasureSpec.makeMeasureSpec(min(size, maxSize), MeasureSpec.EXACTLY)
            MeasureSpec.UNSPECIFIED -> return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY)
            else -> throw IllegalArgumentException("Unknown measure mode: $mode")
        }
    }

    companion object {
        private const val SIZE_UNSPECIFIED = -1
        private const val DEFAULT_WHEEL_ITEM_COUNT = 3
        private const val ASCENDING = 0
        private const val DESCENDING = 1
        private const val HORIZONTAL = LinearLayout.HORIZONTAL
        private const val VERTICAL = LinearLayout.VERTICAL
        private const val TAG = "NumberPicker"

    }
}