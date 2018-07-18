package psycho.euphoria.tools.downloads

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import java.io.File

class DownloadDatabase(private val context: Context) :
        SQLiteOpenHelper(context,
                File(Environment.getExternalStorageDirectory(), "downloads_list.db").absolutePath,
                null,
                DATABASE_VERSION
        ) {
    override fun onCreate(databae: SQLiteDatabase) {

        val sb = StringBuilder()
        sb.append("CREATE TABLE IF NOT EXISTS `downloads` (")
        sb.append("`id`\tINTEGER,")
        sb.append("`url`\tTEXT NOT NULL UNIQUE,")
        sb.append("`finish`\tINTEGER NOT NULL,")
        sb.append("`create_time`\tINTEGER,")
        sb.append("`updte_time`\tINTEGER,")
        sb.append("PRIMARY KEY(`_id`)")
        sb.append(");")

        databae.execSQL(sb.toString())
    }

    fun list(): ArrayList<String> {
        val cursor = readableDatabase.rawQuery("select url from downloads where finish = 0", null)
        try {
            val ls = ArrayList<String>()
            while (cursor.moveToNext()) {
                ls.add(cursor.getString(0))
            }
            return ls
        } finally {
            cursor.close()
        }
    }

    fun insert(url: String) {
        val v = ContentValues()
        v.put("url", url)
        v.put("finish", 0)
        v.put("create_time", getTimeStamp())
        v.put("update_time", getTimeStamp())
        writableDatabase.insert("downloads", null, v)
    }

    fun update(id: Int) {
        val v = ContentValues()

        v.put("finish", 1)

        writableDatabase.update("downloads", v, "id = ?", arrayOf("$id"))
    }

    override fun onUpgrade(databae: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    companion object {
        private const val DATABASE_VERSION = 1
        private var instance: DownloadDatabase? = null


        fun getInstance(context: Context): DownloadDatabase {
            return instance ?: synchronized(this) {
                DownloadDatabase(context).also {
                    instance = it
                }
            }
        }
    }
}