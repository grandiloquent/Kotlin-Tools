package psycho.euphoria.tools.controls.images

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.round
import android.content.res.Configuration


class CustomImageView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {


    init {
        //initializeImage()

        //scaleType = ScaleType.CENTER_INSIDE
    }

    private lateinit var mAnimator: Animator
    private val mDrawLock = Semaphore(0)
    private var mDrawable: Drawable? = null
    private var mLayout = false
    private var mDisplayWidth = 0
    private var mDisplayHeight = 0
    private var mCount = 0;
    private var mX = 0f;
    private var mY = 0f
    private var mRecycle = false
    var centerX = 0f
        private set
    var centerY = 0f
        private set
    private var mStartX = -1f;
    private var mStartY = -1f
    private var mBoundHeight = 0
    private var mBoundWidth = 0
    private var mStartingScale = 0f
    private var mFitScaleHorizontal = 1f;
    private var mFitScaleVertical = 1f
    private var mGestureImageViewListener: GestureImageViewListener? = null
    private var mGestureImageViewTouchListener: GestureImageViewTouchListener? = null
    private var mScaleAdjust = 1f;
    private var mMinScale = 0.75f
    private var mMaxScale = 5.0f
    private var mScale = 1f
    var mOnTouchListener: OnTouchListener? = null
    var mOnClickListener: OnClickListener? = null
    var imageWidth: Int = 0
        get() = mDrawable?.run { intrinsicWidth } ?: 0
        private set

    var imageHeight: Int = 0
        get() = mDrawable?.run { intrinsicHeight } ?: 0
        private set


    fun waitForDraw(timeout: Long) =
            mDrawLock.tryAcquire(timeout, TimeUnit.MICROSECONDS)

    fun setImage(path: String) {
    }

    fun getScale(): Float = mScaleAdjust
    fun setImageResource(resId: Int) {
        if (mDrawable != null) {
            recycle()
        }
        mDrawable = context.resources.getDrawable(resId)
        initializeImage()
    }

    fun redraw() {
        postInvalidate()
    }

    private fun setUpCanvas(measureWidth: Int, measureHeight: Int) {
        if (mDrawable != null && !mLayout) {
            mBoundWidth = round(imageWidth.toFloat() / 2.0f).toInt();
            mBoundHeight = round(imageHeight.toFloat() / 2.0f).toInt();

            val mw = measureWidth - (paddingLeft + paddingRight)
            val mh = measureHeight - (paddingTop + paddingBottom)
            computeCropScale(imageWidth, imageHeight, mw, mh)

            if (mStartingScale <= 0f) {
                computeStartingScale(imageWidth, imageHeight, mw, mh)
            }
            mScaleAdjust = mStartingScale


            centerX = mw.toFloat() / 2.0f;
            centerY = mh.toFloat() / 2.0f

            if (mStartX > 0f) mX = mStartX else mX = centerX
            if (mStartY > 0f) mY = mStartY else mY = centerY


            mGestureImageViewTouchListener = GestureImageViewTouchListener(this, mw, mh)
            mGestureImageViewTouchListener?.apply {
                setMinScale(mMinScale * mFitScaleVertical)
                setMaxScale(mMaxScale * mStartingScale)
                setFitScaleHorizontal(mFitScaleHorizontal);
                setFitScaleVertical(mFitScaleVertical);
                setCanvasWidth(mw);
                setCanvasHeight(mh);
                setOnClickListener(mOnClickListener);
            }

            mDrawable?.setBounds(-mBoundWidth, -mBoundHeight, mBoundWidth, mBoundHeight)


            super.setOnTouchListener { v, event ->
                if (mOnTouchListener != null) {
                    mOnTouchListener?.onTouch(v, event)
                }
                mGestureImageViewTouchListener!!.onTouch(v, event)
            }
            mLayout = true

        }
    }

    fun setGestureImageViewListener(gestureImageViewListener: GestureImageViewListener) {
        mGestureImageViewListener = gestureImageViewListener
    }

    fun getGestureImageViewListener(): GestureImageViewListener? {
        return mGestureImageViewListener;
    }

    private fun initializeImage() {
        if (!mLayout) {
            requestLayout()
            redraw()
        }
    }

    protected fun recycle() {
        if (mRecycle && mDrawable != null && mDrawable is BitmapDrawable) {
            val bitmap = (mDrawable as BitmapDrawable).bitmap
            bitmap?.recycle()
        }
    }

    protected fun computeCropScale(imageWidth: Int, imageHeight: Int, measureWidth: Int, measureHeight: Int) {

        mFitScaleHorizontal = measureWidth.toFloat() / imageWidth.toFloat()
        mFitScaleVertical = measureHeight.toFloat() / imageHeight.toFloat()

        Log.e(TAG, "[computeCropScale]:$measureWidth $measureHeight $imageWidth $imageHeight $mFitScaleHorizontal $mFitScaleVertical")
    }

    fun setPosition(x: Float, y: Float) {
        mX = x
        mY = y
    }

    fun getScaledWidth(): Int {
        return Math.round(imageWidth * mScaleAdjust)
    }

    fun getScaledHeight(): Int {
        return Math.round(imageHeight * mScaleAdjust)
    }

    fun isLandscape(): Boolean {
        return imageWidth >= imageHeight
    }

    fun isPortrait(): Boolean {
        return imageWidth <= imageHeight
    }

    fun animationStart(animation: Animation) {
        if (mAnimator != null) {
            mAnimator.play(animation)
        }
    }

    fun getDeviceOrientation(): Int {
        return Configuration.ORIENTATION_PORTRAIT
        //return deviceOrientation
    }

    fun animationStop() {
        if (mAnimator != null) {
            mAnimator.cancel()
        }
    }

    fun setScale(scale: Float) {
        mScaleAdjust = scale
    }

    fun getImageX(): Float = mX;
    fun getImageY(): Float = mY

    protected fun computeStartingScale(imageWidth: Int, imageHeight: Int, measureWidth: Int, measureHeight: Int) {
        mStartingScale = 1f
//
//        val wr = imageWidth.toFloat() / measureWidth.toFloat()
//        val hr = imageHeight.toFloat() / measureHeight.toFloat()
//        if (wr > hr) {
//            mStartingScale = mFitScaleHorizontal
//        } else {
//            mStartingScale = mFitScaleVertical
//        }
//        Log.e(TAG, "[computeStartingScale] $wr $hr")
    }

    protected fun isRecycled(): Boolean {
        if (mDrawable != null && mDrawable is BitmapDrawable) {
            val bitmap = (mDrawable as BitmapDrawable).bitmap
            if (bitmap != null) {
                return bitmap.isRecycled()
            }
        }
        return false
    }

//    override fun setScaleType(scaleType: ScaleType?) {
//        super.setScaleType(scaleType)
//    }

    override fun onDetachedFromWindow() {
        mAnimator?.finish()
        if (mRecycle && mDrawable != null && !isRecycled()) {
            recycle()
            mDrawable = null
        }
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        if (mLayout) {
            if (mDrawable != null && !isRecycled()) {

                canvas?.let {
                    it.save()
                    it.translate(mX, mY)
//                    if (rotation != 0f) {
//                        it.rotate(rotation)
//                    }
                    val adjustedScale = mScale * mScaleAdjust
                    Log.e(TAG, "[onDraw]: ${++mCount} $adjustedScale")

                    if (adjustedScale != 1f) {

                        it.scale(adjustedScale, adjustedScale)
                    }
                    mDrawable?.draw(it)
                    it.restore()
                }

            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed && !mLayout) {
            Log.e(TAG, "[onLayout]: ")
            if (mDrawable != null && !mLayout) {

                setUpCanvas(mDisplayWidth, mDisplayHeight)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mDrawable != null) {
            // Assume Screen Orientation is portrait
            mDisplayWidth = MeasureSpec.getSize(widthMeasureSpec)
            if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                Log.e(TAG, "[onMeasure]: WRAP_CONTENT $imageWidth x $imageHeight")
                val ratio = imageHeight.toFloat() / imageWidth.toFloat()
                mDisplayHeight = Math.round(mDisplayWidth.toFloat() * ratio)
            } else {
                mDisplayHeight = MeasureSpec.getSize(heightMeasureSpec)
            }
        } else {
            mDisplayWidth = MeasureSpec.getSize(widthMeasureSpec)
            mDisplayHeight = MeasureSpec.getSize(heightMeasureSpec)
        }
        Log.e(TAG, "[onMeasure]: $mDisplayWidth x $mDisplayHeight")
        setMeasuredDimension(mDisplayWidth, mDisplayHeight)
    }

    override fun onAttachedToWindow() {
        mAnimator = Animator(this, TAG)
        mAnimator.start()
        super.onAttachedToWindow()
    }

    companion object {
        private const val TAG = "CustomImageView"
    }
}