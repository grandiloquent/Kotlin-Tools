package psycho.euphoria.tools.ui

import android.app.Activity
import android.os.Bundle
import psycho.euphoria.tools.R

class PlayerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }
}