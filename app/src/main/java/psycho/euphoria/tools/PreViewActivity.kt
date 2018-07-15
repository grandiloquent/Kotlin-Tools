package psycho.euphoria.tools

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.NumberPicker

class PreViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val dialog = AlertDialog.Builder(this)
                .setView(NumberPicker(this)      )
                .create()
        dialog.show()
    }
}