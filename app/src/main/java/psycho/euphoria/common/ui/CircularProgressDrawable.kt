package psycho.euphoria.common.ui
import android.graphics.*
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.animation.Animator
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.graphics.ColorFilter
import android.animation.ValueAnimator
class CircularProgressDrawable(context: Context) : Drawable() {
    private val LINEAR_INTERPOLATOR = LinearInterpolator()
    private val MATERIAL_INTERPOLATOR = FastOutSlowInInterpolator()
    private val mRing: Ring
    private var mRotation = 0.0f
    private val mResources: Resources
    private var mAnimator: Animator? = null
    private var mRotationCount = 0f
    private var mFinishing = false
    init {
        mResources = context.getResources()
        mRing = Ring()
        mRing.setColors(COLORS)
        setStrokeWidth(STROKE_WIDTH)
        setupAnimators()
    }
    private fun applyFinishTranslation(interpolatedTime: Float, ring: Ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        updateRingColor(interpolatedTime, ring)
        val targetRotation = (Math.floor((ring.getStartingRotation() / MAX_PROGRESS_ARC).toDouble()) + 1f).toFloat()
        val startTrim = ring.getStartingStartTrim() + (ring.getStartingEndTrim() - MIN_PROGRESS_ARC - ring.getStartingStartTrim()) * interpolatedTime
        ring.setStartTrim(startTrim)
        ring.setEndTrim(ring.getStartingEndTrim())
        val rotation = ring.getStartingRotation() + (targetRotation - ring.getStartingRotation()) * interpolatedTime
        ring.setRotation(rotation)
    }
    fun applyTransformation(interpolatedTime: Float, ring: Ring, lastFrame: Boolean) {
        if (mFinishing) {
            applyFinishTranslation(interpolatedTime, ring)
            // Below condition is to work around a ValueAnimator issue where onAnimationRepeat is
            // called before last frame (1f).
        } else if (interpolatedTime != 1f || lastFrame) {
            val startingRotation = ring.getStartingRotation()
            val startTrim: Float
            val endTrim: Float
            if (interpolatedTime < SHRINK_OFFSET) { // Expansion occurs on first half of animation
                val scaledTime = interpolatedTime / SHRINK_OFFSET
                startTrim = ring.getStartingStartTrim()
                endTrim = startTrim + ((MAX_PROGRESS_ARC - MIN_PROGRESS_ARC) * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime) + MIN_PROGRESS_ARC)
            } else { // Shrinking occurs on second half of animation
                val scaledTime = (interpolatedTime - SHRINK_OFFSET) / (1f - SHRINK_OFFSET)
                endTrim = ring.getStartingStartTrim() + (MAX_PROGRESS_ARC - MIN_PROGRESS_ARC)
                startTrim = endTrim - ((MAX_PROGRESS_ARC - MIN_PROGRESS_ARC) * (1f - MATERIAL_INTERPOLATOR.getInterpolation(scaledTime)) + MIN_PROGRESS_ARC)
            }
            val rotation = startingRotation + RING_ROTATION * interpolatedTime
            val groupRotation = GROUP_FULL_ROTATION * (interpolatedTime + mRotationCount)
            ring.setStartTrim(startTrim)
            ring.setEndTrim(endTrim)
            ring.setRotation(rotation)
            setRotation(groupRotation)
        }
    }
    override fun draw(canvas: Canvas) {
        val bounds = bounds
        canvas.save()
        canvas.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY())
        mRing.draw(canvas, bounds)
        canvas.restore()
    }
    private fun evaluateColorChange(fraction: Float, startValue: Int, endValue: Int): Int {
        val startA = startValue shr 24 and 0xff
        val startR = startValue shr 16 and 0xff
        val startG = startValue shr 8 and 0xff
        val startB = startValue and 0xff
        val endA = endValue shr 24 and 0xff
        val endR = endValue shr 16 and 0xff
        val endG = endValue shr 8 and 0xff
        val endB = endValue and 0xff
        return (startA + (fraction * (endA - startA)).toInt() shl 24
                or (startR + (fraction * (endR - startR)).toInt() shl 16)
                or (startG + (fraction * (endG - startG)).toInt() shl 8)
                or startB + (fraction * (endB - startB)).toInt())
    }
    override fun getAlpha(): Int {
        return mRing.getAlpha()
    }
    fun getArrowEnabled(): Boolean {
        return mRing.getShowArrow()
    }
    fun getArrowHeight(): Float {
        return mRing.getArrowHeight()
    }
    fun getArrowScale(): Float {
        return mRing.getArrowScale()
    }
    fun getArrowWidth(): Float {
        return mRing.getArrowWidth()
    }
    fun getBackgroundColor(): Int {
        return mRing.getBackgroundColor()
    }
    fun getCenterRadius(): Float {
        return mRing.getCenterRadius()
    }
    fun getColorSchemeColors(): IntArray? {
        return mRing.getColors()
    }
    fun getEndTrim(): Float {
        return mRing.getEndTrim()
    }
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
    fun getProgressRotation(): Float {
        return mRing.getRotation()
    }
    private fun getRotation(): Float {
        return mRotation
    }
    fun getStartTrim(): Float {
        return mRing.getStartTrim()
    }
    fun getStrokeCap(): Paint.Cap {
        return mRing.getStrokeCap()
    }
    fun getStrokeWidth(): Float {
        return mRing.getStrokeWidth()
    }
    fun isRunning(): Boolean? {
        return mAnimator?.isRunning()
    }
    override fun setAlpha(alpha: Int) {
        mRing.setAlpha(alpha)
        invalidateSelf()
    }
    fun setArrowDimensions(width: Float, height: Float) {
        mRing.setArrowDimensions(width, height)
        invalidateSelf()
    }
    fun setArrowEnabled(show: Boolean) {
        mRing.setShowArrow(show)
        invalidateSelf()
    }
    fun setArrowScale(scale: Float) {
        mRing.setArrowScale(scale)
        invalidateSelf()
    }
    fun setBackgroundColor(color: Int) {
        mRing.setBackgroundColor(color)
        invalidateSelf()
    }
    fun setCenterRadius(centerRadius: Float) {
        mRing.setCenterRadius(centerRadius)
        invalidateSelf()
    }
    fun setColorSchemeColors(vararg colors: Int) {
        mRing.setColors(colors)
        mRing.setColorIndex(0)
        invalidateSelf()
    }
    fun setProgressRotation(rotation: Float) {
        mRing.setRotation(rotation)
        invalidateSelf()
    }
    private fun setRotation(rotation: Float) {
        mRotation = rotation
    }
    private fun setSizeParameters(centerRadius: Float, strokeWidth: Float, arrowWidth: Float,
                                  arrowHeight: Float) {
        val ring = mRing
        val metrics = mResources.displayMetrics
        val screenDensity = metrics.density
        ring.setStrokeWidth(strokeWidth * screenDensity)
        ring.setCenterRadius(centerRadius * screenDensity)
        ring.setColorIndex(0)
        ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity)
    }
    fun setStartEndTrim(start: Float, end: Float) {
        mRing.setStartTrim(start)
        mRing.setEndTrim(end)
        invalidateSelf()
    }
    fun setStrokeCap(strokeCap: Paint.Cap) {
        mRing.setStrokeCap(strokeCap)
        invalidateSelf()
    }
    fun setStrokeWidth(strokeWidth: Float) {
        mRing.setStrokeWidth(strokeWidth)
        invalidateSelf()
    }
    fun setStyle(size: Int) {
        if (size == LARGE) {
            setSizeParameters(CENTER_RADIUS_LARGE, STROKE_WIDTH_LARGE, ARROW_WIDTH_LARGE,
                    ARROW_HEIGHT_LARGE)
        } else {
            setSizeParameters(CENTER_RADIUS, STROKE_WIDTH, ARROW_WIDTH, ARROW_HEIGHT)
        }
        invalidateSelf()
    }
    private fun setupAnimators() {
        val ring = mRing
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val interpolatedTime = animation.animatedValue as Float
            updateRingColor(interpolatedTime, ring)
            applyTransformation(interpolatedTime, ring, false)
            invalidateSelf()
        }
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.RESTART
        animator.interpolator = LINEAR_INTERPOLATOR
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                mRotationCount = 0f
            }
            override fun onAnimationEnd(animator: Animator) {
                // do nothing
            }
            override fun onAnimationCancel(animation: Animator) {
                // do nothing
            }
            override fun onAnimationRepeat(animator: Animator) {
                applyTransformation(1f, ring, true)
                ring.storeOriginals()
                ring.goToNextColor()
                if (mFinishing) {
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false
                    animator.cancel()
                    animator.duration = ANIMATION_DURATION.toLong()
                    animator.start()
                    ring.setShowArrow(false)
                } else {
                    mRotationCount = mRotationCount + 1
                }
            }
        })
        mAnimator = animator
    }
    fun start() {
        mAnimator?.cancel()
        mRing.storeOriginals()
        // Already showing some part of the ring
        if (mRing.getEndTrim() != mRing.getStartTrim()) {
            mFinishing = true
            mAnimator?.let {
                it.duration = (ANIMATION_DURATION / 2).toLong()
                it.start()
            }
        } else {
            mRing.setColorIndex(0)
            mRing.resetOriginals()
            mAnimator?.let {
                it.duration = (ANIMATION_DURATION).toLong()
                it.start()
            }
        }
    }
    fun stop() {
        mAnimator?.cancel()
        setRotation(0f)
        mRing.setShowArrow(false)
        mRing.setColorIndex(0)
        mRing.resetOriginals()
        invalidateSelf()
    }
    fun updateRingColor(interpolatedTime: Float, ring: Ring) {
        if (interpolatedTime > COLOR_CHANGE_OFFSET) {
            ring.setColor(evaluateColorChange((interpolatedTime - COLOR_CHANGE_OFFSET) / (1f - COLOR_CHANGE_OFFSET), ring.getStartingColor(),
                    ring.getNextColor()))
        } else {
            ring.setColor(ring.getStartingColor())
        }
    }
    override fun setColorFilter(colorFilter: ColorFilter) {
        mRing.setColorFilter(colorFilter)
        invalidateSelf()
    }
    companion object {
        private const val ARROW_HEIGHT_LARGE = 6f
        private const val ARROW_HEIGHT = 5f
        private const val ARROW_WIDTH_LARGE = 12f
        private const val ARROW_WIDTH = 10f
        private const val CENTER_RADIUS_LARGE = 11f
        private const val CENTER_RADIUS = 7.5f
        const val DEFAULT = 1
        const val LARGE = 0
        private const val STROKE_WIDTH_LARGE = 3f
        private const val STROKE_WIDTH = 2.5f
        private val COLORS = intArrayOf(Color.BLACK)
        private const val ANIMATION_DURATION = 1332
        private const val COLOR_CHANGE_OFFSET = 0.75f
        private const val SHRINK_OFFSET = 0.5f
        private val GROUP_FULL_ROTATION = 1080f / 5f
        private const val MAX_PROGRESS_ARC = .8f
        private const val MIN_PROGRESS_ARC = .01f
        private val RING_ROTATION = 1f - (MAX_PROGRESS_ARC - MIN_PROGRESS_ARC)
    }
    class Ring {
        val mTempBounds = RectF()
        val mPaint = Paint()
        val mArrowPaint = Paint()
        val mCirclePaint = Paint()
        var mStartTrim = 0f
        var mEndTrim = 0f
        var mRotation = 0f
        var mStrokeWidth = 5f
        var mColors: IntArray? = null
        var mColorIndex: Int = 0
        var mStartingStartTrim: Float = 0f
        var mStartingEndTrim: Float = 0f
        var mStartingRotation: Float = 0f
        var mShowArrow: Boolean = false
        var mArrow: Path? = null
        var mArrowScale = 1f
        var mRingCenterRadius: Float = 0f
        var mArrowWidth = 0f
        var mArrowHeight = 0f
        var mAlpha = 255
        var mCurrentColor: Int = 0
        init {
            mPaint.strokeCap = Paint.Cap.SQUARE;
            mPaint.isAntiAlias = true;
            mPaint.style = Paint.Style.STROKE;
            mArrowPaint.style = Paint.Style.FILL;
            mArrowPaint.isAntiAlias = true;
            mCirclePaint.color = Color.TRANSPARENT;
        }
        fun draw(c: Canvas, bounds: Rect) {
            val arcBounds = mTempBounds
            var arcRadius = mRingCenterRadius + mStrokeWidth / 2f
            if (mRingCenterRadius <= 0) {
                arcRadius = Math.min(bounds.width(), bounds.height()) / 2f - Math.max(
                        mArrowWidth * mArrowScale / 2f, mStrokeWidth / 2f)
            }
            arcBounds.set(bounds.centerX() - arcRadius,
                    bounds.centerY() - arcRadius,
                    bounds.centerX() + arcRadius,
                    bounds.centerY() + arcRadius)
            val startAngle = (mStartTrim + mRotation) * 360
            val endAngle = (mEndTrim + mRotation) * 360
            val sweepAngle = endAngle - startAngle
            mPaint.color = mCurrentColor
            mPaint.alpha = mAlpha
            val inset = mStrokeWidth / 2f
            arcBounds.inset(inset, inset)
            c.drawCircle(arcBounds.centerX(), arcBounds.centerY(), arcBounds.width() / 2f,
                    mCirclePaint)
            arcBounds.inset(-inset, -inset)
            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint)
            drawTriangle(c, startAngle, sweepAngle, arcBounds)
        }
        fun drawTriangle(c: Canvas, startAngle: Float, sweepAngle: Float, bounds: RectF) {
            if (mShowArrow) {
                if (mArrow == null) {
                    mArrow = Path()
                    mArrow?.setFillType(android.graphics.Path.FillType.EVEN_ODD)
                } else {
                    mArrow?.reset()
                }
                val centerRadius = Math.min(bounds.width(), bounds.height()) / 2f
                val inset = mArrowWidth * mArrowScale / 2f
                mArrow?.apply {
                    moveTo(0f, 0f)
                    lineTo(mArrowWidth * mArrowScale, 0f)
                    lineTo(mArrowWidth * mArrowScale / 2, mArrowHeight * mArrowScale)
                    offset(centerRadius + bounds.centerX() - inset,
                            bounds.centerY() + mStrokeWidth / 2f)
                    close()
                }
                mArrowPaint.color = mCurrentColor
                mArrowPaint.alpha = mAlpha
                c.save()
                c.rotate(startAngle + sweepAngle, bounds.centerX(),
                        bounds.centerY())
                c.drawPath(mArrow, mArrowPaint)
                c.restore()
            }
        }
        fun getAlpha(): Int {
            return mAlpha
        }
        fun getArrowHeight(): Float {
            return mArrowHeight
        }
        fun getArrowScale(): Float {
            return mArrowScale
        }
        fun getArrowWidth(): Float {
            return mArrowWidth
        }
        fun getBackgroundColor(): Int {
            return mCirclePaint.color
        }
        fun getCenterRadius(): Float {
            return mRingCenterRadius
        }
        fun getColors(): IntArray? {
            return mColors
        }
        fun getEndTrim(): Float {
            return mEndTrim
        }
        fun getNextColor(): Int {
            return mColors?.let { it[getNextColorIndex()] } ?: run { -1 }
        }
        fun getNextColorIndex(): Int {
            return (mColorIndex + 1) % (mColors?.size ?: -1)
        }
        fun getRotation(): Float {
            return mRotation
        }
        fun getShowArrow(): Boolean {
            return mShowArrow
        }
        fun getStartingColor(): Int {
            return mColors?.let { it[mColorIndex] } ?: run { -1 }
        }
        fun getStartingEndTrim(): Float {
            return mStartingEndTrim
        }
        fun getStartingRotation(): Float {
            return mStartingRotation
        }
        fun getStartingStartTrim(): Float {
            return mStartingStartTrim
        }
        fun getStartTrim(): Float {
            return mStartTrim
        }
        fun getStrokeCap(): Paint.Cap {
            return mPaint.strokeCap
        }
        fun getStrokeWidth(): Float {
            return mStrokeWidth
        }
        fun goToNextColor() {
            setColorIndex(getNextColorIndex())
        }
        fun resetOriginals() {
            mStartingStartTrim = 0f
            mStartingEndTrim = 0f
            mStartingRotation = 0f
            setStartTrim(0f)
            setEndTrim(0f)
            setRotation(0f)
        }
        fun setAlpha(alpha: Int) {
            mAlpha = alpha
        }
        fun setArrowDimensions(width: Float, height: Float) {
            mArrowWidth = width
            mArrowHeight = height
        }
        fun setArrowScale(scale: Float) {
            if (scale != mArrowScale) {
                mArrowScale = scale
            }
        }
        fun setBackgroundColor(color: Int) {
            mCirclePaint.color = color
        }
        fun setCenterRadius(centerRadius: Float) {
            mRingCenterRadius = centerRadius
        }
        fun setColor(color: Int) {
            mCurrentColor = color
        }
        fun setColorFilter(filter: ColorFilter) {
            mPaint.colorFilter = filter
        }
        fun setColorIndex(index: Int) {
            mColorIndex = index
            mCurrentColor = mColors?.let { it[mColorIndex] } ?: run { -1 }
        }
        fun setColors(colors: IntArray) {
            mColors = colors
            setColorIndex(0)
        }
        fun setEndTrim(endTrim: Float) {
            mEndTrim = endTrim
        }
        fun setRotation(rotation: Float) {
            mRotation = rotation
        }
        fun setShowArrow(show: Boolean) {
            if (mShowArrow != show) {
                mShowArrow = show
            }
        }
        fun setStartTrim(startTrim: Float) {
            mStartTrim = startTrim
        }
        fun setStrokeCap(strokeCap: Paint.Cap) {
            mPaint.strokeCap = strokeCap
        }
        fun setStrokeWidth(strokeWidth: Float) {
            mStrokeWidth = strokeWidth
            mPaint.strokeWidth = strokeWidth
        }
        fun storeOriginals() {
            mStartingStartTrim = mStartTrim
            mStartingEndTrim = mEndTrim
            mStartingRotation = mRotation
        }
    }
}
abstract class LookupTableInterpolator protected constructor(private val mValues: FloatArray) : Interpolator {
    private val mStepSize: Float
    init {
        mStepSize = 1f / (mValues.size - 1)
    }
    override fun getInterpolation(input: Float): Float {
        if (input >= 1.0f) {
            return 1.0f
        }
        if (input <= 0f) {
            return 0f
        }
        // Calculate index - We use min with length - 2 to avoid IndexOutOfBoundsException when
        // we lerp (linearly interpolate) in the return statement
        val position = Math.min((input * (mValues.size - 1)).toInt(), mValues.size - 2)
        // Calculate values to account for small offsets as the lookup table has discrete values
        val quantized = position * mStepSize
        val diff = input - quantized
        val weight = diff / mStepSize
        // Linearly interpolate between the table values
        return mValues[position] + weight * (mValues[position + 1] - mValues[position])
    }
}
/**
 * Interpolator corresponding to [android.R.interpolator.fast_out_slow_in].
 *
 * Uses a lookup table for the Bezier curve from (0,0) to (1,1) with control points:
 * P0 (0, 0)
 * P1 (0.4, 0)
 * P2 (0.2, 1.0)
 * P3 (1.0, 1.0)
 */
class FastOutSlowInInterpolator : LookupTableInterpolator(VALUES) {
    companion object {
        /**
         * Lookup table values sampled with x at regular intervals between 0 and 1 for a total of
         * 201 points.
         */
        private val VALUES = floatArrayOf(0.0000f, 0.0001f, 0.0002f, 0.0005f, 0.0009f, 0.0014f, 0.0020f, 0.0027f, 0.0036f, 0.0046f, 0.0058f, 0.0071f, 0.0085f, 0.0101f, 0.0118f, 0.0137f, 0.0158f, 0.0180f, 0.0205f, 0.0231f, 0.0259f, 0.0289f, 0.0321f, 0.0355f, 0.0391f, 0.0430f, 0.0471f, 0.0514f, 0.0560f, 0.0608f, 0.0660f, 0.0714f, 0.0771f, 0.0830f, 0.0893f, 0.0959f, 0.1029f, 0.1101f, 0.1177f, 0.1257f, 0.1339f, 0.1426f, 0.1516f, 0.1610f, 0.1707f, 0.1808f, 0.1913f, 0.2021f, 0.2133f, 0.2248f, 0.2366f, 0.2487f, 0.2611f, 0.2738f, 0.2867f, 0.2998f, 0.3131f, 0.3265f, 0.3400f, 0.3536f, 0.3673f, 0.3810f, 0.3946f, 0.4082f, 0.4217f, 0.4352f, 0.4485f, 0.4616f, 0.4746f, 0.4874f, 0.5000f, 0.5124f, 0.5246f, 0.5365f, 0.5482f, 0.5597f, 0.5710f, 0.5820f, 0.5928f, 0.6033f, 0.6136f, 0.6237f, 0.6335f, 0.6431f, 0.6525f, 0.6616f, 0.6706f, 0.6793f, 0.6878f, 0.6961f, 0.7043f, 0.7122f, 0.7199f, 0.7275f, 0.7349f, 0.7421f, 0.7491f, 0.7559f, 0.7626f, 0.7692f, 0.7756f, 0.7818f, 0.7879f, 0.7938f, 0.7996f, 0.8053f, 0.8108f, 0.8162f, 0.8215f, 0.8266f, 0.8317f, 0.8366f, 0.8414f, 0.8461f, 0.8507f, 0.8551f, 0.8595f, 0.8638f, 0.8679f, 0.8720f, 0.8760f, 0.8798f, 0.8836f, 0.8873f, 0.8909f, 0.8945f, 0.8979f, 0.9013f, 0.9046f, 0.9078f, 0.9109f, 0.9139f, 0.9169f, 0.9198f, 0.9227f, 0.9254f, 0.9281f, 0.9307f, 0.9333f, 0.9358f, 0.9382f, 0.9406f, 0.9429f, 0.9452f, 0.9474f, 0.9495f, 0.9516f, 0.9536f, 0.9556f, 0.9575f, 0.9594f, 0.9612f, 0.9629f, 0.9646f, 0.9663f, 0.9679f, 0.9695f, 0.9710f, 0.9725f, 0.9739f, 0.9753f, 0.9766f, 0.9779f, 0.9791f, 0.9803f, 0.9815f, 0.9826f, 0.9837f, 0.9848f, 0.9858f, 0.9867f, 0.9877f, 0.9885f, 0.9894f, 0.9902f, 0.9910f, 0.9917f, 0.9924f, 0.9931f, 0.9937f, 0.9944f, 0.9949f, 0.9955f, 0.9960f, 0.9964f, 0.9969f, 0.9973f, 0.9977f, 0.9980f, 0.9984f, 0.9986f, 0.9989f, 0.9991f, 0.9993f, 0.9995f, 0.9997f, 0.9998f, 0.9999f, 0.9999f, 1.0000f, 1.0000f)
    }
}