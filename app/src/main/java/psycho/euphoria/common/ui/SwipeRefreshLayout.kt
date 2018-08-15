package psycho.euphoria.common.ui
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.view.animation.Animation.AnimationListener
import psycho.euphoria.common.Services
import psycho.euphoria.common.visible

class SwipeRefreshLayout : ViewGroup {
    private val mCircleView: CircleImageView = CircleImageView(context, CIRCLE_BG_LIGHT.toInt())
    private val mDecelerateInterpolator: DecelerateInterpolator
    private val mTouchSlop: Int
    private var mActivePointerId = INVALID_POINTER
    private var mCircleDiameter: Int = 0
    private var mFrom: Int = 0
    private var mIsBeingDragged: Boolean = false
    private var mOriginalOffsetTop = 0
    private var mRefreshing = false
    private var mReturningToStart: Boolean = false
    private var mTarget: View? = null
    private var mTotalDragDistance = -1
    private var mUsingCustomStart: Boolean = false
    var mCurrentTargetOffsetLeft: Int = 0
    var mSpinnerOffsetEnd: Int = 0
    private val mMediumAnimationDuration: Int
    private var mScaleAnimation: Animation? = null
    private var mScaleDownAnimation: Animation? = null
    private var mCircleViewIndex = -1
    var listener: OnRefreshListener? = null
    private var mNotify = false
    private val mProgress = CircularProgressDrawable(context)
    private val STARTING_PROGRESS_ALPHA = (.3f * MAX_ALPHA).toInt()
    private var mInitialDownX = 0f
    private var mInitialMotionX = 0f
    private var mAlphaStartAnimation: Animation? = null
    private var mAlphaMaxAnimation: Animation? = null
    private val mRefreshListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {}
        override fun onAnimationRepeat(animation: Animation) {}
        override fun onAnimationEnd(animation: Animation?) {
            if (mRefreshing) {
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();
                if (mNotify) {
                    listener?.onRefresh()
                }
                mCurrentTargetOffsetLeft = mCircleView.left
            } else {
                reset()
            }
        }
    }
    private val mAnimateToCorrectPosition = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            var targetTop = 0
            val endTarget = mSpinnerOffsetEnd
//            if (!mUsingCustomStart) {
//                endTarget = mSpinnerOffsetEnd - Math.abs(mOriginalOffsetTop)
//            } else {
//                endTarget = mSpinnerOffsetEnd
//            }
            targetTop = mFrom + ((endTarget - mFrom) * interpolatedTime).toInt()
            val offset = targetTop - mCircleView.left
            //Log.i(TAG, "[applyTransformation]:mAnimateToCorrectPosition offset $offset ")
            setTargetOffsetLeftAndRight(offset)
            mProgress.setArrowScale(1 - interpolatedTime);
        }
    }
    private val mAnimateToStartPosition = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToStart(interpolatedTime)
        }
    }
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    init {
        mProgress.setStyle(CircularProgressDrawable.DEFAULT)
        mCircleView.setImageDrawable(mProgress)
        addView(mCircleView)
        mTouchSlop = Services.touchSlop
        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);
        setWillNotDraw(false)
        mDecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        mCircleDiameter = (CIRCLE_DIAMETER * Services.density).toInt()
        isChildrenDrawingOrderEnabled = true;
        mSpinnerOffsetEnd = (DEFAULT_CIRCLE_TARGET * Services.density).toInt()
        mTotalDragDistance = mSpinnerOffsetEnd;
        mOriginalOffsetTop = -mCircleDiameter
        mCurrentTargetOffsetLeft = -mCircleDiameter
        moveToStart(1.0f);
    }
    private fun animateOffsetToCorrectPosition(from: Int, listener: Animation.AnimationListener) {
        mFrom = from
        mAnimateToCorrectPosition.reset()
        mAnimateToCorrectPosition.duration = ANIMATE_TO_TRIGGER_DURATION
        mAnimateToCorrectPosition.interpolator = mDecelerateInterpolator
        mCircleView.listener = listener
        mCircleView.clearAnimation()
        mCircleView.startAnimation(mAnimateToCorrectPosition)
    }
    private fun animateOffsetToStartPosition(from: Int, listener: AnimationListener?) {
        mFrom = from
        mAnimateToStartPosition.reset()
        mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION)
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator)
        if (listener != null) {
            mCircleView.listener = listener
        }
        mCircleView.clearAnimation()
        mCircleView.startAnimation(mAnimateToStartPosition)
    }
    private fun ensureTarget() {
        if (mTarget == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != mCircleView) {
                    mTarget = child
                    break
                }
            }
        }
    }
    private fun finishSpinner(overscrollTop: Float) {
        if (overscrollTop > mTotalDragDistance) {
            setRefreshing(true, true)
        } else {
            mRefreshing = false
            mProgress.setStartEndTrim(0f, 0f)
            var listener: Animation.AnimationListener? = null
            listener = object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation?) {
                    startScaleDownAnimation(null)
                }
                override fun onAnimationRepeat(animation: Animation) {}
            }
            animateOffsetToStartPosition(mCurrentTargetOffsetLeft, listener)
            mProgress.setArrowEnabled(false)
        }
    }
    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        return if (mCircleViewIndex < 0) {
            i
        } else if (i == childCount - 1) {
            mCircleViewIndex
        } else if (i >= mCircleViewIndex) {
            i + 1
        } else {
            i
        }
    }
    private fun isAnimationRunning(animation: Animation?): Boolean {
        return animation != null && animation.hasStarted() && !animation.hasEnded()
    }
    private fun moveSpinner(overscrollTop: Float) {
        mProgress.setArrowEnabled(true)
        val originalDragPercent = overscrollTop / mTotalDragDistance
        val dragPercent = Math.min(1f, Math.abs(originalDragPercent))
        val adjustedPercent = Math.max(dragPercent - .4f, 0f) * 5 / 3
        val extraOS = Math.abs(overscrollTop) - mTotalDragDistance
        val slingshotDist = mSpinnerOffsetEnd
        val tensionSlingshotPercent = Math.max(0f, (Math.min(extraOS, slingshotDist.toFloat() * 2) / slingshotDist))
        val tensionPercent = ((((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4).toDouble(), 2.0))).toFloat() * 2f)
        val extraMove = (slingshotDist) * tensionPercent * 2f
        val targetY = mOriginalOffsetTop + ((slingshotDist * dragPercent) + extraMove).toInt()
        mCircleView.visible()
        mCircleView.scaleX = 1f;
        mCircleView.scaleY = 1f;
        if (overscrollTop < mTotalDragDistance) {
            if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                    && !isAnimationRunning(mAlphaStartAnimation)) {
                // Animate the alpha
                startProgressAlphaStartAnimation();
            }
        } else {
            if (mProgress.getAlpha() < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
                // Animate the alpha
                startProgressAlphaMaxAnimation();
            }
        }
        val strokeStart = adjustedPercent * .8f
        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart))
        mProgress.setArrowScale(Math.min(1f, adjustedPercent))
        val rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f
        mProgress.setProgressRotation(rotation)
        setTargetOffsetLeftAndRight(targetY - mCurrentTargetOffsetLeft)
    }
    private fun moveToStart(interpolatedTime: Float) {
        var targetTop = 0
        targetTop = mFrom + ((mOriginalOffsetTop - mFrom) * interpolatedTime).toInt()
        val offset = targetTop - mCircleView.left
        setTargetOffsetLeftAndRight(offset)
    }
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()
        val action = ev.getActionMasked()
        val pointerIndex: Int
        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }
        if (!isEnabled || mReturningToStart || mRefreshing) {
            return false
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                setTargetOffsetLeftAndRight(mOriginalOffsetTop - mCircleView.top)
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                mInitialDownX = ev.getX(pointerIndex)
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                val x = ev.getX(pointerIndex)
                startDragging(x)
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
            }
        }
        return mIsBeingDragged
    }
    override fun onLayout(p0: Boolean, p1: Int, p2: Int, p3: Int, p4: Int) {
        val width = measuredWidth
        val height = measuredHeight
        if (childCount == 0) {
            return
        }
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }
        val child = mTarget
        val childLeft = paddingLeft
        val childTop = paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        child?.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
        val circleWidth = mCircleView.getMeasuredWidth()
        val circleHeight = mCircleView.getMeasuredHeight()
        mCircleView.layout(mCurrentTargetOffsetLeft,
                height / 2 - circleHeight / 2,
                mCurrentTargetOffsetLeft + circleWidth,
                height / 2 + circleHeight / 2)
//
    }
    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }
        mTarget?.measure(View.MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(
                measuredHeight - paddingTop - paddingBottom, View.MeasureSpec.EXACTLY))
        mCircleView.measure(View.MeasureSpec.makeMeasureSpec(mCircleDiameter, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(mCircleDiameter, View.MeasureSpec.EXACTLY))
        mCircleViewIndex = -1
        for (index in 0 until childCount) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index
                break
            }
        }
    }
    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = ev.getPointerId(newPointerIndex)
        }
    }
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        var pointerIndex = -1
        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }
        if (!isEnabled || mReturningToStart || mRefreshing) {
            return false
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                val x = ev.getX(pointerIndex)
                startDragging(x)
                if (mIsBeingDragged) {
                    val overscrollTop = (x - mInitialMotionX) * DRAG_RATE
                    if (overscrollTop > 0) {
                        moveSpinner(overscrollTop)
                    } else {
                        return false
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                pointerIndex = ev.actionIndex
                if (pointerIndex < 0) {
                    return false
                }
                mActivePointerId = ev.getPointerId(pointerIndex)
            }
            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
            MotionEvent.ACTION_UP -> {
                pointerIndex = ev.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                if (mIsBeingDragged) {
                    val x = ev.getX(pointerIndex)
                    val overscrollTop = (x - mInitialMotionX) * DRAG_RATE
                    mIsBeingDragged = false
                    finishSpinner(overscrollTop)
                }
                mActivePointerId = INVALID_POINTER
                return false
            }
            MotionEvent.ACTION_CANCEL -> return false
        }
        return true
    }
    fun reset() {
        mCircleView.clearAnimation()
        mProgress.stop()
        mCircleView.visibility = View.GONE
        setTargetOffsetLeftAndRight(mOriginalOffsetTop - mCurrentTargetOffsetLeft)
        mCurrentTargetOffsetLeft = mCircleView.left
    }
    private fun setAnimationProgress(progress: Float) {
        mCircleView.scaleX = progress
        mCircleView.scaleY = progress
    }
    fun setRefreshing(refreshing: Boolean) {
        if (refreshing && mRefreshing != refreshing) {
            mRefreshing = refreshing
            var endTarget = mSpinnerOffsetEnd
//            if (!mUsingCustomStart) {
//                endTarget = mSpinnerOffsetEnd + mOriginalOffsetTop
//            } else {
//                endTarget = mSpinnerOffsetEnd
//            }
            setTargetOffsetLeftAndRight(endTarget - mCurrentTargetOffsetLeft)
            mNotify = false
        } else {
            Log.e(TAG, "[setRefreshing] $refreshing")
            setRefreshing(refreshing, false)
        }
    }
    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {
        if (mRefreshing != refreshing) {
            mNotify = notify
            ensureTarget()
            mRefreshing = refreshing
            if (mRefreshing) {
                Log.i(TAG, "mCurrentTargetOffsetLeft ${mCurrentTargetOffsetLeft}\n ${mCircleView.left}")
                animateOffsetToCorrectPosition(mCurrentTargetOffsetLeft, mRefreshListener)
            } else {
                startScaleDownAnimation(mRefreshListener);
            }
        }
    }
    fun setTargetOffsetLeftAndRight(offset: Int) {
        mCircleView.bringToFront()
        mCircleView.offsetLeftAndRight(offset)
        mCurrentTargetOffsetLeft = mCircleView.left
    }
    private fun startAlphaAnimation(startingAlpha: Int, endingAlpha: Int): Animation {
        val alpha = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                mProgress.alpha = (startingAlpha + (endingAlpha - startingAlpha) * interpolatedTime).toInt()
            }
        }
        alpha.duration = ALPHA_ANIMATION_DURATION
        // Clear out the previous animation listeners.
        mCircleView.listener = null
        mCircleView.clearAnimation()
        mCircleView.startAnimation(alpha)
        return alpha
    }
    private fun startDragging(x: Float) {
        val xDiff = x - mInitialDownX
        if (xDiff > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionX = mInitialDownX + mTouchSlop
            mIsBeingDragged = true
            mProgress.setAlpha(STARTING_PROGRESS_ALPHA)
        }
    }
    private fun startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(mProgress.alpha, MAX_ALPHA)
    }
    private fun startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation(mProgress.alpha, STARTING_PROGRESS_ALPHA)
    }
    fun startScaleDownAnimation(listener: Animation.AnimationListener?) {
        mScaleDownAnimation = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(1 - interpolatedTime)
            }
        }
        mScaleDownAnimation?.setDuration(SCALE_DOWN_DURATION)
        mCircleView.listener = listener
        mCircleView.clearAnimation()
        mCircleView.startAnimation(mScaleDownAnimation)
    }
    private fun startScaleUpAnimation() {
        mCircleView.visibility = View.VISIBLE
        mScaleAnimation = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(interpolatedTime)
            }
        }
        mScaleAnimation?.setDuration(mMediumAnimationDuration.toLong())
        mCircleView.clearAnimation()
        mCircleView.startAnimation(mScaleAnimation)
    }
    companion object {
        private const val TAG = "SwipeRefreshLayout"
        private const val CIRCLE_DIAMETER = 40
        const val DECELERATE_INTERPOLATION_FACTOR = 2f
        const val DRAG_RATE = .5f
        private const val SCALE_DOWN_DURATION = 150L
        const val INVALID_POINTER = -1
        private const val ANIMATE_TO_TRIGGER_DURATION = 200L
        private const val CIRCLE_BG_LIGHT = 0xFFFAFAFA
        private const val DEFAULT_CIRCLE_TARGET = 64
        private const val MAX_ALPHA = 255
        private const val ALPHA_ANIMATION_DURATION = 300L
        private const val ANIMATE_TO_START_DURATION = 200L
        private const val MAX_PROGRESS_ANGLE = .8f
    }
    interface OnRefreshListener {
        fun onRefresh()
    }
}