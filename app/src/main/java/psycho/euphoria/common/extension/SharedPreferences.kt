package psycho.euphoria.common.extension

import android.content.SharedPreferences

fun SharedPreferences.int(key: String, defaultValue: Int = 0): Int {
    return getInt(key, defaultValue)
}
fun SharedPreferences.putInt(key: String, value: Int) {
    return edit().putInt(key, value).apply()
}
fun SharedPreferences.putString(key: String, value: String) {
    return edit().putString(key, value).apply()
}
fun SharedPreferences.string(key: String, defaultValue: String? = null): String? {
    return getString(key, defaultValue)
}
