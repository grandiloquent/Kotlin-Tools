package psycho.euphoria.player

import android.app.AlertDialog
import android.content.Context
import android.os.Environment
import android.view.Menu
import android.widget.EditText
import psycho.euphoria.common.App
import psycho.euphoria.common.Services
import java.io.File

private const val MENU_ADJUST_SRT_1X = 15

private fun ajustSrtTimeline(srtFile: File, targetDirectory: File, step: Int = 500) {

    if (!srtFile.exists()) return
    if (!targetDirectory.exists()) targetDirectory.mkdirs()

    var v = srtFile.readText()
    val r = Regex("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}");
    v = r.replace(v) {
        var ms = it.value.substringAfterLast(',').toIntOrNull() ?: 0
        var s = it.value.substringBeforeLast(',').substringAfterLast(':').toIntOrNull() ?: 0
        var m = it.value.substringBeforeLast(',').substringBeforeLast(':').substringAfterLast(':').toIntOrNull()
                ?: 0
        var h = it.value.substringBeforeLast(':').toIntOrNull() ?: 0
        var t = ms + s * 1000 + m * 1000 * 60 + h * 1000 * 60 * 60
        t += step
        if (t <= 0) {
            "00:00:00,000"
        } else {
            ms = t % 1000
            s = (t / 1000) % 60
            m = (t / 1000 / 60) % 60
            h = t / (1000 * 60 * 60)

            val vr = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')},${ms.toString().padStart(3, '0')}"
            vr
        }

    }

    File(targetDirectory, srtFile.name).writeText(v)
    // 00:03:17,834
}

fun menuAdjustSrt(menu: Menu) {
    menu.add(0, MENU_ADJUST_SRT_1X, 0, "Ajust Srt")
}

fun menuAdjustSrtAction(actionId: Int, srtFile: File, context: Context): Boolean {
    if (actionId != MENU_ADJUST_SRT_1X)
        return false

    when (actionId) {
        MENU_ADJUST_SRT_1X -> {
            val editText = EditText(context)
            editText.setText("500")
            val dialog = AlertDialog.Builder(context)
                    .setView(editText)
                    .setPositiveButton("Ok") { _, _ ->
                        val targetDirectory = File(Environment.getExternalStorageDirectory(), "Subtitles")
                        ajustSrtTimeline(srtFile, targetDirectory, editText.text.toString().toIntOrNull()
                                ?: 500);
                    }
            dialog.show()
        }
    }


    return true
}