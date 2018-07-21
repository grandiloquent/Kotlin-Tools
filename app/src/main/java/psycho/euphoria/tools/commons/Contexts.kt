package psycho.euphoria.tools.commons

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.widget.Toast

val Context.notificationManager: NotificationManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(NotificationManager::class.java)
        else
            return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
val Context.alarmManager: AlarmManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(AlarmManager::class.java)
        else
            return getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
val Context.clipboardManager: ClipboardManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(ClipboardManager::class.java)
        else
            return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}