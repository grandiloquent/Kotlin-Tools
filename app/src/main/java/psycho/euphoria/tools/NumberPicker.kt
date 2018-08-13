package psycho.euphoria.tools

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Scroller
import psycho.euphoria.common.extension.dp2px
import psycho.euphoria.common.extension.sp2px
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class NumberPicker(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val mAdjustScroller: Scroller
    private val mFlingScroller: Scroller
    private val mMaximumFlingVelocity: Int
    private val mMinimumFlingVelocity: Int
    private val mTextPaint: Paint
    private var mCurrentScrollOffset: Int = 0
    private var mCurrentValueOffset: Int = 0
    private var mLastDownEventY: Float = 0.0f
    private var mLastDownOrMoveEventY: Float = 0.0f
    private var mScrollerLastY = 0
    private var mVelocityTracker: VelocityTracker? = null
    private var textHeight: Int = 0
    var minHeight = 0
        set(value) {
            field = value
            requestLayout()
        }
    var minWidth = 0
        set(value) {
            field = value
            requestLayout()
        }
    private var maxValue = 0
        set(v) {
            if (v < 0) {
                throw IllegalArgumentException("maxValue must be >= 0")
            }
            field = v
            if (value > v) {
                value = v
            }
            invalidate()
        }
    private var minValue = 0
        set(v) {
            if (v < 0) {
                throw IllegalArgumentException("minValue must be >= 0")
            }
            field = v
            if (value < v) {
                value = v
            }
            invalidate()
        }
    var paddingHorizontal = 0
        set(value) {
            field = value
            requestLayout()
        }
    var paddingVertical = 0
        set(value) {
            field = value
            requestLayout()
        }
    var textColor: Int = DEFAULT_TEXT_COLOR
        set(value) {
            field = value
            mTextPaint.color = value
            invalidate()
        }
    var textSize = 0.0f
        set(value) {
            field = value
            mTextPaint.textSize = value
            invalidate()
        }
      var value = 0
        set(v) {
            if (v < minValue) {
                throw IllegalArgumentException("value must be >= minValue")
            }
            if (v > maxValue) {
                throw IllegalArgumentException("value must be <= maxValue")
            }
            field = v
            invalidate()
        }

    init {
        mTextPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        mFlingScroller = Scroller(context, null, true)
        mAdjustScroller = Scroller(context, DecelerateInterpolator(2.5f))
        val configuration = ViewConfiguration.get(context)
        mMinimumFlingVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumFlingVelocity = configuration.scaledMaximumFlingVelocity / MAX_FLING_VELOCITY_ADJUSTMENT
        value = DEFAULT_VALUE
        textColor = DEFAULT_TEXT_COLOR
        textSize = context.sp2px(DEFAULT_TEXT_SIZE_SP)
        paddingVertical = context.dp2px(DEFAULT_PADDING).toInt()
        paddingHorizontal = paddingVertical
        maxValue = DEFAULT_MAX_VALUE
        minValue = DEFAULT_MIN_VALUE
        minHeight = context.dp2px(DEFAULT_MIN_HEIGHT_DP).toInt()
        minWidth = context.dp2px(DEFAULT_MIN_WIDTH_DP).toInt()

        // Log.e(TAG, "configuration => ${configuration} \nmTextPaint => ${mTextPaint} \nmFlingScroller => ${mFlingScroller} \nmAdjustScroller => ${mAdjustScroller} \nmMinimumFlingVelocity => ${mMinimumFlingVelocity} \nmMaximumFlingVelocity => ${mMaximumFlingVelocity} \nvalue => ${value} \ntextColor => ${textColor} \ntextSize => ${textSize} \npaddingVertical => ${paddingVertical} \npaddingHorizontal => ${paddingHorizontal} \nmaxValue => ${maxValue} \nminValue => ${minValue} \n")
    }

    private fun adjust(adjustedValueOffset: Int) {
        if (adjustedValueOffset != mCurrentValueOffset) {
            if (mCurrentScrollOffset < 0) {
                mCurrentScrollOffset += measuredHeight
            } else {
                mCurrentScrollOffset -= measuredHeight
            }
        }
        mScrollerLastY = mCurrentScrollOffset
        mCurrentValueOffset = 0
        mAdjustScroller.startScroll(0, mCurrentScrollOffset, 0, -mCurrentScrollOffset, ADJUSTMENT_DURATION_MILLIS)
    }

    private fun calculateAdjustedValueOffset(rawScrollOffset: Int): Int {
        val mCurrentValueOffset = rawScrollOffset / measuredHeight
        return (mCurrentValueOffset + 0.5 * if (mCurrentValueOffset < 0) -1.0 else 1.0).toInt()
    }

    private fun calculateAdjustedValueOffset(): Int {
        return if (abs(mCurrentScrollOffset) < measuredHeight / 2)
            mCurrentValueOffset
        else
            mCurrentValueOffset + if (mCurrentScrollOffset < 0) -1 else 1
    }

    private fun calculateCurrentOffsets(rawScrollOffset: Int) {
        mCurrentValueOffset = rawScrollOffset / measuredHeight
        mCurrentScrollOffset = abs(rawScrollOffset) - abs(mCurrentValueOffset) * measuredHeight
        mCurrentScrollOffset *= if (rawScrollOffset < 0) -1 else 1
    }

    private fun calculateTextHeight(): Int {
        val bounds = Rect()
        mTextPaint.getTextBounds("0", 0, 1, bounds)
        textHeight = bounds.height()
        return textHeight
    }

    private fun calculateTextHeightWithInternalPadding(): Int {
        return calculateTextHeight() + paddingVertical * 2
    }

    private fun calculateTextWidth(): Int {
        var maxDigitWidth = 0f
        for (i in 0..9) {
            val digitWidth = mTextPaint.measureText(formatNumberWithLocale(i))
            if (digitWidth > maxDigitWidth) {
                maxDigitWidth = digitWidth
            }
        }
        var numberOfDigits = 0
        var current = maxValue
        while (current > 0) {
            numberOfDigits++
            current /= 10
        }
        return (numberOfDigits * maxDigitWidth).toInt()
    }

    private fun calculateTextWidthWithInternalPadding(): Int {
        return calculateTextWidth() + paddingHorizontal * 2
    }

    override fun computeScroll() {
        var scroller = mFlingScroller
        if (scroller.isFinished) {
            scroller = mAdjustScroller
            if (scroller.isFinished) {
                return
            }
        }
        scroller.computeScrollOffset()
        val currentScrollerY = scroller.currY
        val diffScrollY = mScrollerLastY - currentScrollerY
        mCurrentScrollOffset -= diffScrollY
        mScrollerLastY = currentScrollerY
        if (mAdjustScroller.isFinished) {
            if (mFlingScroller.isFinished) {
                if (mCurrentScrollOffset != 0) {
                    val adjustedValueOffset = calculateAdjustedValueOffset(measuredHeight)
                    value = getValue(adjustedValueOffset)
                    adjust(adjustedValueOffset)
                }
            } else {
                val newScrollOffset = mCurrentScrollOffset % measuredHeight
                if (newScrollOffset != mCurrentScrollOffset) {
                    val numberOfValuesScrolled = (mCurrentScrollOffset - newScrollOffset) / measuredHeight
                    mCurrentValueOffset += numberOfValuesScrolled
                    mCurrentScrollOffset = newScrollOffset
                }
            }
        }
        invalidate()
    }

    private fun fling(velocity: Int) {
        if (velocity > 0) {
            mScrollerLastY = 0
            mFlingScroller.fling(0, mScrollerLastY, 0, velocity, 0, 0, 0, Integer.MAX_VALUE)
        } else {
            mScrollerLastY = Integer.MAX_VALUE
            mFlingScroller.fling(0, mScrollerLastY, 0, velocity, 0, 0, 0, Integer.MAX_VALUE)
        }
    }

    private fun formatNumberWithLocale(value: Int): String {
        return String.format(Locale.getDefault(), "%d", value)
    }

    fun getMaxValue(): Int {
        return maxValue
    }

    fun getMinValue(): Int {
        return minValue
    }

    private fun getValue(offset: Int): Int {
        var offset = offset
        offset %= maxValue - minValue
        if (value + offset < minValue) {
            return maxValue - (abs(offset) - (value - minValue)) + 1
        } else if (value + offset > maxValue) {
            return minValue + offset - (maxValue - value) - 1
        }
        return value + offset
    }


    private fun measureHeight(heightMeasureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val specSize = View.MeasureSpec.getSize(heightMeasureSpec)
        val result: Int
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = max(minHeight.toInt(), calculateTextHeightWithInternalPadding()) + paddingTop + paddingBottom
        }
        return result
    }

    private fun measureWidth(widthMeasureSpec: Int): Int {
        val specMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val specSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val result: Int
        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = max(minWidth.toInt(), calculateTextWidthWithInternalPadding()) + paddingLeft + paddingRight
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = ((right - left) / 2).toFloat()
        val y = ((bottom - top) / 2 + textHeight / 2).toFloat()
        val currentValueStart = (y + mCurrentScrollOffset).toInt()
        val prevValueStart = currentValueStart - measuredHeight
        val nextValueStart = currentValueStart + measuredHeight
        canvas.drawText(getValue(mCurrentValueOffset + 1).toString() + "", x, prevValueStart.toFloat(), mTextPaint)
        canvas.drawText(getValue(mCurrentValueOffset).toString() + "", x, currentValueStart.toFloat(), mTextPaint)
        canvas.drawText(getValue(mCurrentValueOffset - 1).toString() + "", x, nextValueStart.toFloat(), mTextPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = measureWidth(widthMeasureSpec)
        val heightSize = measureHeight(heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(event)
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (!mFlingScroller.isFinished) {
                    mFlingScroller.forceFinished(true)
                }
                if (!mAdjustScroller.isFinished) {
                    mAdjustScroller.forceFinished(true)
                }
                mLastDownEventY = event.y
                this.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                mLastDownOrMoveEventY = event.y
                val rawScrollOffset = (mLastDownOrMoveEventY - mLastDownEventY).toInt()
                calculateCurrentOffsets(rawScrollOffset)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                mVelocityTracker?.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                val initialVelocity = mVelocityTracker?.yVelocity ?: 0.0f
                if (abs(initialVelocity) > mMinimumFlingVelocity) {
                    fling(initialVelocity.toInt())
                } else {
                    val rawScrollOffset = (mLastDownOrMoveEventY - mLastDownEventY).toInt()
                    val adjustedValueOffset = calculateAdjustedValueOffset(rawScrollOffset)
                    calculateCurrentOffsets(rawScrollOffset)
                    value = getValue(adjustedValueOffset)
                    adjust(adjustedValueOffset)
                }
                invalidate()
                mVelocityTracker?.recycle()
                mVelocityTracker = null
                this.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    companion object {
        private const val DEFAULT_MIN_HEIGHT_DP = 20.0f
        private const val DEFAULT_MIN_WIDTH_DP = 14.0f
        private const val DEFAULT_MAX_VALUE = 9
        private const val DEFAULT_MIN_VALUE = 0
        private const val DEFAULT_VALUE = 0
        private const val DEFAULT_TEXT_COLOR = Color.BLACK
        private const val DEFAULT_TEXT_SIZE_SP = 25.0f
        private const val DEFAULT_PADDING = 12.0f
        private const val ADJUSTMENT_DURATION_MILLIS = 800
        private const val MAX_FLING_VELOCITY_ADJUSTMENT = 6
        private const val TAG = "NumberPicker"
    }
}
class MeterView : LinearLayout {
    private var numberOfFirst = DEFAULT_NUMBER_OF_BLACK
    private var numberOfSecond = DEFAULT_NUMBER_OF_RED
    private var firstColor = DEFAULT_BLACK_COLOR
    private var secondColor = DEFAULT_RED_COLOR
    private var enabled = DEFAULT_ENABLED
    private var pickerStyleId = -1


    var value: Int
        get() {
            var result = 0
            var koeff = childCount
            for (i in 0 until childCount) {
                val picker = getChildAt(i) as NumberPicker
                result += (picker.value * Math.pow(10.0, (--koeff).toDouble())).toInt()
            }
            return result
        }
        set(value) {
            var value = value
            var koeff = childCount
            for (i in 0 until childCount) {
                val picker = getChildAt(i) as NumberPicker
                val number = (value / Math.pow(10.0, (--koeff).toDouble())).toInt()
                if (i == 0 && number > 9) {
                    throw IllegalArgumentException("Number of digits cannot be greater then pickers number")
                }
                value -= (number * Math.pow(10.0, koeff.toDouble())).toInt()
                picker.value = number
            }
        }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        orientation = LinearLayout.HORIZONTAL
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MeterView, 0, 0)
            numberOfFirst = typedArray.getInt(R.styleable.MeterView_mv_numberOfFirst, numberOfFirst)
            numberOfSecond = typedArray.getInt(R.styleable.MeterView_mv_numberOfSecond, numberOfSecond)
            firstColor = typedArray.getColor(R.styleable.MeterView_mv_firstColor, firstColor)
            secondColor = typedArray.getColor(R.styleable.MeterView_mv_secondColor, secondColor)
            pickerStyleId = typedArray.getResourceId(R.styleable.MeterView_mv_pickerStyle, pickerStyleId)
            enabled = typedArray.getBoolean(R.styleable.MeterView_mv_enabled, enabled)
            typedArray.recycle()
        }
        populate(context)
    }

    private fun populate(context: Context) {
        for (i in 0 until numberOfFirst + numberOfSecond) {
            val meterNumberPicker = createPicker(context)
            meterNumberPicker.setBackgroundColor(if (i < numberOfFirst) firstColor else secondColor)
            meterNumberPicker.isEnabled = isEnabled
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.weight = 1f
            addView(meterNumberPicker, lp)
        }
    }

    private fun createPicker(context: Context): NumberPicker {
        return NumberPicker(context,null)
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return !enabled || super.onInterceptTouchEvent(ev)
    }

    fun setNumbersOf(numberOfFirst: Int, numberOfSecond: Int) {
        this.numberOfFirst = numberOfFirst
        this.numberOfSecond = numberOfSecond
        removeAllViews()
        init(context, null)
    }

    companion object {
        private val DEFAULT_NUMBER_OF_BLACK = 5
        private val DEFAULT_NUMBER_OF_RED = 0
        private val DEFAULT_BLACK_COLOR = Color.WHITE
        private val DEFAULT_RED_COLOR = -0x340000
        private val DEFAULT_ENABLED = true
    }
}