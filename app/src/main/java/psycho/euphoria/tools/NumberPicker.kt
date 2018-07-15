package psycho.euphoria.tools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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

    private val mPaint: Paint
    private var mItemSize: Float = 0.0f
    private var textSize: Float
    private var selectedTextSize: Float
    private val mSelectedText: EditText
    private var mInitialScrollOffset = Int.MIN_VALUE
    private var mSelectorTextGapHeight = 0
    private var mSelectedTextCenterY = 0.0f
    private var mWheelMiddleItemIndex = 1
    private var mMaxWidth = 0
    private var mMinWidth = 0
    private var mMaxHeight = 0
    private var mMinHeight = 0
    private var mCurrentScrollOffset = 0
    var textColor = 0XFFFF4081
    var selectedTextColor = 0XFFFF4081

    init {
        setWillNotDraw(false)
        textSize = context.dp2px(30.0f)
        selectedTextSize = context.dp2px(35.0f)

        var t = textSize
        mPaint = Paint().apply {
            isAntiAlias = true
            textSize = t
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }

        mMaxWidth = SIZE_UNSPECIFIED
        mMinWidth = context.dp2px(DEFAULT_MIN_WIDTH).toInt()
        mMinHeight = SIZE_UNSPECIFIED
        mMaxHeight = context.dp2px(DEFAULT_MAX_HEIGHT).toInt()

        LayoutInflater.from(context).inflate(R.layout.number_picker_with_selector_wheel, this, true)
        mSelectedText = findViewById(R.id.np__numberpicker_input);
        mSelectedText.run {
            isEnabled = false
            isFocusable = false
            imeOptions = EditorInfo.IME_ACTION_NONE
        }
    }

    private fun getMaxTextSize(): Float {
        return max(textSize, selectedTextSize)
    }

    fun initialize() {
        val textGapCount = 3
        val totalTextSize = ((3 - 1) * textSize + selectedTextSize).toInt()
        val totalTextGapHeight = bottom - top - totalTextSize
        mSelectorTextGapHeight = totalTextGapHeight / textGapCount
        mItemSize = getMaxTextSize() + mSelectorTextGapHeight
        mInitialScrollOffset = (mSelectedTextCenterY - mItemSize * mWheelMiddleItemIndex).toInt()
        mCurrentScrollOffset = mInitialScrollOffset
    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        "onDrawForeground".e(TAG, "")

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        val newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);

        val widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, getMeasuredWidth(), widthMeasureSpec);
        val heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, getMeasuredHeight(), heightMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);
    }

    private fun resolveSizeAndStateRespectingMinSize(minSize: Int, measuredSize: Int, measureSpec: Int): Int {
        if (minSize != SIZE_UNSPECIFIED) {
            val desiredWidth = Math.max(minSize, measuredSize)
            return View.resolveSizeAndState(desiredWidth, measureSpec, 0)
        } else {
            return measuredSize
        }
    }

    private fun makeMeasureSpec(measureSpec: Int, maxSize: Int): Int {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec
        }
        val size = View.MeasureSpec.getSize(measureSpec)
        val mode = View.MeasureSpec.getMode(measureSpec)
        when (mode) {
            View.MeasureSpec.EXACTLY -> return measureSpec
            View.MeasureSpec.AT_MOST -> return View.MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), View.MeasureSpec.EXACTLY)
            View.MeasureSpec.UNSPECIFIED -> return View.MeasureSpec.makeMeasureSpec(maxSize, View.MeasureSpec.EXACTLY)
            else -> throw IllegalArgumentException("Unknown measure mode: $mode")
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val tl = (measuredWidth - mSelectedText.measuredWidth) / 2
        val tt = (measuredHeight - mSelectedText.measuredHeight) / 2
        val tr = tl + mSelectedText.measuredWidth
        val tb = tt + mSelectedText.measuredHeight

        mSelectedText.layout(tl, tt, tr, tb);
        mSelectedTextCenterY = mSelectedText.y + mSelectedText.measuredHeight / 2;

        "onLayout".e(TAG, "$tl $tt $mSelectedTextCenterY $mItemSize")
        if (changed) {
            initialize()
        }
    }

    override fun onDraw(canvas: Canvas?) {


        canvas?.run {
            save()

            var xl = (right - left) / 2.0f
            var yl = mCurrentScrollOffset.toFloat()


            for (i in 0..2) {
                if (i == 1) {
                    mPaint.setTextSize(selectedTextSize);
                    mPaint.setColor(selectedTextColor.toInt());
                } else {
                    mPaint.setTextSize(textSize);
                    mPaint.setColor(textColor.toInt());
                }

                    drawText(i.toString(), xl, yl + getPaintCenterY(mPaint.getFontMetrics()), mPaint);
                yl += mItemSize
            }
            restore()
        }
    }

    private fun getPaintCenterY(fontMetrics: Paint.FontMetrics): Float {
        return abs(fontMetrics.top + fontMetrics.bottom) / 2
    }

    companion object {
        private const val DEFAULT_MAX_HEIGHT = 180.0f
        private const val DEFAULT_MIN_WIDTH = 64.0f
        private const val SIZE_UNSPECIFIED = -1
        private const val DEFAULT_TEXT_COLOR = 0xFF000000
        private const val DEFAULT_TEXT_SIZE = 25f;
        private const val TAG = "NumberPicker"

    }
}