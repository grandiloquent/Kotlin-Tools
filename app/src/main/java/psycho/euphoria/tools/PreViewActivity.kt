package psycho.euphoria.tools

import android.app.Activity
import android.os.Bundle

class PreViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
    }
}