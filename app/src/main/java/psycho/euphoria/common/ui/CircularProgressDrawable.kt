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
