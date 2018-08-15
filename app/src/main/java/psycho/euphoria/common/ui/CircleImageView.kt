package psycho.euphoria.common.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.widget.ImageView
import psycho.euphoria.common.Services

class CircleImageView : ImageView {
    private var mShadowRadius: Int = 0
    var listener: Animation.AnimationListener? = null
    constructor(context: Context, color: Int) : super(context) {
        val shadowYOffset = Services.density * Y_OFFSET
        val shadowXOffset = Services.density * X_OFFSET
        mShadowRadius = (Services.density * SHADOW_RADIUS).toInt()
        val circle: ShapeDrawable
        if (Build.VERSION.SDK_INT >= 21) {
            circle = ShapeDrawable(OvalShape())
            this.elevation = SHADOW_ELEVATION * Services.density
        } else {
            val oval = OvalShadow(mShadowRadius)
            circle = ShapeDrawable(oval)
            setLayerType(View.LAYER_TYPE_SOFTWARE, circle.paint)
            circle.paint.setShadowLayer(mShadowRadius.toFloat(), shadowXOffset, shadowYOffset, KEY_SHADOW_COLOR)
            val padding = mShadowRadius
            setPadding(padding, padding, padding, padding)
        }
        circle.paint.color = color
        Services.setBackground(this, circle)
    }
    companion object {
        private const val TAG = "CircleImageView"
        private const val FILL_SHADOW_COLOR = 0x3D000000
        private const val KEY_SHADOW_COLOR = 0x1E000000
        private const val SHADOW_ELEVATION = 4
        private const val SHADOW_RADIUS = 3.5f
        private const val X_OFFSET = 0f
        private const val Y_OFFSET = 1.75f
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (Build.VERSION.SDK_INT < 21) {
            setMeasuredDimension(measuredWidth + mShadowRadius * 2, measuredHeight + mShadowRadius * 2)
        }
    }
    fun setBackgroundColorRes(colorRes: Int) {
        setBackgroundColor(Services.getColor(colorRes))
    }
    override fun onAnimationStart() {
        super.onAnimationStart()
        listener?.onAnimationStart(animation)
    }
    override fun onAnimationEnd() {
        super.onAnimationEnd()
        if (listener == null) {
            Log.e(TAG, "[onAnimationEnd]")
        }
        listener?.onAnimationEnd(animation)
    }
    override fun setBackgroundColor(color: Int) {
        val b = background
        if (b is ShapeDrawable) {
            b.paint.color = color
        }
    }
    private inner class OvalShadow(shadowRadius: Int) : OvalShape() {
        private val mShadowPaint = Paint()
        private var mRadialGradient: RadialGradient? = null
        init {
            mShadowRadius = shadowRadius
            updateRadialGradient(rect().width())
        }
        override fun onResize(width: Float, height: Float) {
            super.onResize(width, height)
            updateRadialGradient(width)
        }
        private fun updateRadialGradient(diameter: Float) {
            val center = diameter / 2f
            /**
             * Create a shader that draws a radial gradient given the center and radius.
             *
             * @param centerX  The x-coordinate of the center of the radius
             * @param centerY  The y-coordinate of the center of the radius
             * @param radius   Must be positive. The radius of the circle for this gradient.
             * @param colors   The colors to be distributed between the center and edge of the circle
             * @param stops    May be <code>null</code>. Valid values are between <code>0.0f</code> and
             *                 <code>1.0f</code>. The relative position of each corresponding color in
             *                 the colors array. If <code>null</code>, colors are distributed evenly
             *                 between the center and edge of the circle.
             * @param tileMode The Shader tiling mode
             */
            mRadialGradient = RadialGradient(
                    center,
                    center,
                    mShadowRadius.toFloat(),
                    intArrayOf(FILL_SHADOW_COLOR, Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP)
            mShadowPaint.setShader(mRadialGradient)
        }
    }
}