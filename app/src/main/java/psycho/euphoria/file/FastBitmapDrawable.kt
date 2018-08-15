package psycho.euphoria.file
import android.graphics.*
import android.graphics.drawable.Drawable
class FastBitmapDrawable(private var bitmap: Bitmap) : Drawable() {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), copyBounds(), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    override fun getIntrinsicWidth(): Int {
        return bitmap.width
    }
    override fun getIntrinsicHeight(): Int {
        return bitmap.height
    }
    override fun getMinimumWidth(): Int {
        return bitmap.width
    }
    override fun getMinimumHeight(): Int {
        return bitmap.height
    }
    fun getBitmap(): Bitmap {
        return bitmap
    }
}