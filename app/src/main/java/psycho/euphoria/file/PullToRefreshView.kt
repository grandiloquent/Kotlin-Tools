package psycho.euphoria.file

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.AbsListView
import android.widget.ImageView
import psycho.euphoria.common.Services
import psycho.euphoria.common.isLessHoneycomb
import psycho.euphoria.common.isLessIceCreamSandwich
import kotlin.math.abs
import kotlin.math.min

class PullToRefreshView : ViewGroup {
    var listener: OnRefreshListener? = null
    private var mActivePointerId = 0
    private var mCurrentDragPercent = 0.0f
    private var mCurrentOffsetTop = 0
    private val mDecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
    private var mFrom = 0
    private var mFromDragPercent = 0.0f
    private var mInitialMotionY = 0.0f
    private var mIsBeingDragged = false
    private var mNotify = false
    private var mRefreshing = false
    private val mRefreshView: ImageView = ImageView(context)
    private var mTarget: View? = null
    private var mTargetPaddingBottom = 0
    private var mTargetPaddingLeft = 0
    private var mTargetPaddingRight = 0
    private var mTargetPaddingTop = 0
    var totalDragDistance: Int = 0
    private var mBaseRefreshView: BaseRefreshView? = null

    private val mToStartListener = object : Animation.AnimationListener {
        override fun onAnimationRepeat(p0: Animation?) {
            //
        }

        override fun onAnimationEnd(p0: Animation?) {
            mBaseRefreshView?.stop()
            mCurrentOffsetTop = mTarget?.top ?: 0
        }

        override fun onAnimationStart(p0: Animation?) {
        }
    }
    private val mAnimationToStartPosition = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            moveToStart(interpolatedTime)
        }
    }
    private val mAnimateToCorrectPosition = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            mTarget?.let {
                var targetTop = 0
                val endTarget = totalDragDistance
                targetTop = (mFrom + ((endTarget - mFrom) * interpolatedTime).toInt())
                val offset = targetTop - it.top
                mCurrentDragPercent = mFromDragPercent - (mFromDragPercent - 1.0f) * interpolatedTime
                mBaseRefreshView?.setPercent(mCurrentDragPercent, false)
                setTargetOffsetTop(offset, false)
            }
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        totalDragDistance = Services.dp2px(DRAG_MAX_DISTANCE)
        setRefreshStyle(STYLE_SUN)
        addView(mRefreshView)

        setWillNotDraw(false)
        isChildrenDrawingOrderEnabled = true

    }


    private fun animateOffsetToCorrectPosition() {

        mFrom = mCurrentOffsetTop
        mFromDragPercent = mCurrentDragPercent
        mAnimateToCorrectPosition.apply {
            reset()
            duration = MAX_OFFSET_ANIMATION_DURATION.toLong()
            interpolator = mDecelerateInterpolator
            mRefreshView.clearAnimation()
            mRefreshView.startAnimation(mAnimateToCorrectPosition)
            if (mRefreshing) {
                mBaseRefreshView?.start()
                if (mNotify) {
                    listener?.onRefresh()
                }
            } else {
                mBaseRefreshView?.stop()
                animateOffsetToStartPosition()
            }
        }
        mTarget?.let {
            mCurrentOffsetTop = it.top
            it.setPadding(mTargetPaddingLeft, mTargetPaddingTop, mTargetPaddingRight, totalDragDistance)
        }
    }

    private fun animateOffsetToStartPosition() {

        mFrom = mCurrentOffsetTop
        mFromDragPercent = mCurrentDragPercent
        val animationDuration = Math.abs((MAX_OFFSET_ANIMATION_DURATION * mFromDragPercent).toLong());
        mAnimationToStartPosition.apply {
            reset()
            duration = animationDuration
            interpolator = mDecelerateInterpolator
            setAnimationListener(mToStartListener)
            mRefreshView.clearAnimation()
            mRefreshView.startAnimation(mAnimationToStartPosition)
        }
    }

    private fun canChildScrollUp(): Boolean {
        if (isLessIceCreamSandwich) {
            if (mTarget is AbsListView) {
                val v = mTarget as AbsListView
                return v.childCount > 0 && (v.firstVisiblePosition > 0 || v.getChildAt(0).top < v.paddingTop)
            } else {
                return mTarget?.scrollY ?: 0 > 0
            }
        } else {
            return mTarget?.canScrollVertically(-1) ?: false
            /**
             * Check if this view can be scrolled vertically in a certain direction.
             *
             * @param view The View against which to invoke the method.
             * @param direction Negative to check scrolling up, positive to check scrolling down.
             * @return true if this view can be scrolled in the specified direction, false otherwise.
             *
             * @deprecated Use {@link View#canScrollVertically(int)} directly.
             */
            //return ViewCompat.canScrollVertically(mTarget, -1)
        }
    }

    private fun ensureTarget() {
        if (mTarget != null) return
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != mRefreshView) {
                    mTarget = child
                    mTargetPaddingLeft = child.paddingLeft
                    mTargetPaddingTop = child.paddingTop
                    mTargetPaddingRight = child.paddingRight
                    mTargetPaddingBottom = child.paddingBottom
                }
//                
            }
        }
    }

    private fun getMothionEventY(event: MotionEvent, activePointerId: Int): Float {
        val index = event.findPointerIndex(activePointerId)
        if (index < 0) return -1f
        return event.getY(index)
    }

    private fun moveToStart(interpolatedTime: Float) {
        mTarget?.let {
            val targetTop = mFrom - (mFrom * interpolatedTime).toInt()
            val targetPercent = mFromDragPercent * (1.0f - interpolatedTime)
            val offset = targetTop - it.top
            mCurrentDragPercent = targetPercent
            mBaseRefreshView?.setPercent(mCurrentDragPercent, true)
            it.setPadding(mTargetPaddingLeft, mTargetPaddingTop, mTargetPaddingRight, mTargetPaddingBottom + targetTop)
            setTargetOffsetTop(offset, false)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {

        if (!isEnabled || canChildScrollUp() || mRefreshing) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setTargetOffsetTop(0, true)
                mActivePointerId = event.getPointerId(0)
                mIsBeingDragged = false
                val initialMotionY = getMothionEventY(event, mActivePointerId)
                if (initialMotionY == -1f) return false
                mInitialMotionY = initialMotionY
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val y = getMothionEventY(event, mActivePointerId)
                if (y == -1f) return false
                val yDiff = y - mInitialMotionY
                if (yDiff > Services.touchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true
                }
//                
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
//                
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
            }
        }
        return mIsBeingDragged
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        ensureTarget()
        mTarget?.let {
            val mw = measuredWidth
            val mh = measuredHeight
            val pl = paddingLeft
            val pr = paddingRight
            val pt = paddingTop
            val pb = paddingBottom
            it.layout(pl, pt + mCurrentOffsetTop, pl + mw - pr, pt + mh - pb + mCurrentOffsetTop)
            mRefreshView.layout(pl, pt, pl + mw - pr, pt + mh - pb)
//            
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        ensureTarget()
        if (mTarget == null) return
        val wms = MeasureSpec.makeMeasureSpec(measuredWidth - paddingRight - paddingLeft, MeasureSpec.EXACTLY)
        val hms = MeasureSpec.makeMeasureSpec(measuredHeight - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
        mTarget?.measure(wms, hms)
        mRefreshView.measure(wms, hms)
//        
    }

    private fun onSecondaryPointerUp(event: MotionEvent) {

        val pointerIndex = event.actionIndex
        /**
         * Call {@link MotionEvent#getAction}, returning only the pointer index
         * portion.
         *
         * @deprecated Call {@link MotionEvent#getActionIndex()} directly. This method will be
         * removed in a future release.
         */
        //val pointerIndex = MotionEventCompat.getActionIndex(event)
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerId == 0) 1 else 0
            mActivePointerId = event.getPointerId(newPointerIndex)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (!mIsBeingDragged) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                if (pointerIndex < 0) return false
                val y = event.getY(pointerIndex)
                val yDiff = y - mInitialMotionY
                val scrollTop = yDiff * DRAG_RATE
                mCurrentDragPercent = scrollTop / totalDragDistance
                if (mCurrentDragPercent < 0) return false
                val boundedDragPercent = min(1f, mCurrentDragPercent)
                val extraOS = abs(scrollTop) - totalDragDistance
                val slingshotDist = totalDragDistance
                val tensionSlingshotPercent = Math.max(0f,
                        Math.min(extraOS, slingshotDist * 2f) / slingshotDist);
                val tensionPercent = ((tensionSlingshotPercent / 4) - Math.pow(
                        (tensionSlingshotPercent / 4.0), 2.0)).toFloat() * 2f;
                val extraMove = slingshotDist * tensionPercent / 2
                val targetY = ((slingshotDist * boundedDragPercent) + extraMove).toInt();
                mBaseRefreshView?.setPercent(mCurrentDragPercent, true)
                setTargetOffsetTop(targetY - mCurrentOffsetTop, true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                mActivePointerId = event.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(event)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (mActivePointerId == INVALID_POINTER) return false
                val pointerIndex = event.findPointerIndex(mActivePointerId)
                val y = event.getY(pointerIndex)
                val overScrollTop = (y - mInitialMotionY) * DRAG_RATE
                mIsBeingDragged = false
                //
                if (overScrollTop > totalDragDistance) {
                    setRefreshing(true, true)
                } else {
                    mRefreshing = false
                    animateOffsetToStartPosition()
                }
                mActivePointerId = INVALID_POINTER
                return false
            }
        }
        return true
    }

    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {

        if (mRefreshing != refreshing) {
            mNotify = notify
            ensureTarget()
            mRefreshing = refreshing
            if (mRefreshing) {
                mBaseRefreshView?.setPercent(1f, true)
                animateOffsetToCorrectPosition()
            } else {
                animateOffsetToStartPosition()
            }
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        if (mRefreshing != refreshing) {
            setRefreshing(refreshing, false)
        }
    }

    fun setRefreshStyle(type: Int) {
        setRefreshing(false)
        if (type == STYLE_SUN) {
            mBaseRefreshView = SunRefreshView(context, this)
        }
        mRefreshView.setImageDrawable(mBaseRefreshView)
    }

    fun setRefreshViewPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mRefreshView.setPadding(left, top, right, bottom)
    }

    fun setTargetOffsetTop(offset: Int, requireUpdate: Boolean) {
        mTarget?.let {
            it.offsetTopAndBottom(offset)
            mCurrentOffsetTop = it.top
            if (requireUpdate && isLessHoneycomb) {
                invalidate()
            }
        }
    }

    companion object {
        private const val TAG = "PullToRefreshView"
        const val DECELERATE_INTERPOLATION_FACTOR = 2f
        const val DRAG_MAX_DISTANCE = 120
        const val DRAG_RATE = .5f
        const val INVALID_POINTER = -1
        const val MAX_OFFSET_ANIMATION_DURATION = 700
        const val STYLE_SUN = 0
    }

    interface OnRefreshListener {
        fun onRefresh()
    }
}