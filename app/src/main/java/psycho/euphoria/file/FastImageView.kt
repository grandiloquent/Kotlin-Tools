package psycho.euphoria.file

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FastImageView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    var drawble: Drawable? = null
        set(value) {
            field = value.also {
                invalidate()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //Log.e(TAG, "widthMeasureSpec => ${widthMeasureSpec} \nheightMeasureSpec => ${heightMeasureSpec} \n")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        //Log.e(TAG, "changed => ${changed} \nleft => ${left} \ntop => ${top} \nright => ${right} \nbottom => ${bottom} \n")
    }

    override fun onDraw(canvas: Canvas) {
        drawble?.apply {
            /**
             * Specify a bounding rectangle for the Drawable. This is where the drawable
             * will draw when its draw() method is called.
             */

            var left = 0
            var top = 0
            var w = 0
            var h = 0
//            canvas.translate(16f,16f)
//            canvas.clipRect(Rect(0, 0, 126, 126))
//
            if (intrinsicHeight == intrinsicWidth) {
                Log.e(TAG,
                        "intrinsicHeight ${intrinsicHeight}\n"
                                + "intrinsicWidth ${intrinsicWidth}\n")

                left = abs(width - intrinsicHeight) / 2
                top = left
                w = max(width, intrinsicWidth) - (left * 2)
                h = w
            } else {
                // (intrinsicWidth / width*1.0f) The result is a rounded float
                val scaleWidth = intrinsicWidth / 126f
                val scaleHeight = intrinsicHeight / 126f
                val scale = max(scaleHeight, scaleWidth)
                w = (intrinsicWidth / scale).toInt()
                h = (intrinsicHeight / scale).toInt()
                left = (width - w) / 2
                top = (height - h) / 2
                Log.e(TAG,
                        "h ${h}\n"
                                + "height ${height}\n"
                                + "intrinsicHeight ${intrinsicHeight}\n"
                                + "intrinsicWidth ${intrinsicWidth}\n"
                                + "left ${left}\n"
                                + "scale ${scale}\n"
                                + "scaleHeight ${scaleHeight}\n"
                                + "scaleWidth ${scaleWidth}\n"
                                + "top ${top}\n"
                                + "w ${w}\n"
                                + "width ${width}\n"
                )
            }


            canvas.translate(left.toFloat(), top.toFloat())
            setBounds(0, 0, w, h)


            draw(canvas)
        }
    }

    companion object {
        private const val TAG = "FastImageView"
    }
}
