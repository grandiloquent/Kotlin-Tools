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

        sb.append("CREATE TABLE IF NOT EXISTS downloads(")
        sb.append("id INTEGER PRIMARY KEY AUTOINCREMENT,")
        sb.append("url TEXT,")
        sb.append("fileName TEXT,")
        sb.append("currentBytes INTEGER,")
        sb.append("etag TEXT,")
        sb.append("userAgent TEXT,")
        sb.append("mimeType TEXT,")
        sb.append("totalBytes TEXT,")
        sb.append("finish BOOLEAN,")
        sb.append("create_time INTEGER")
        sb.append(")")

        databae.execSQL(sb.toString())
    }

    fun list(): ArrayList<DownloadInfo> {
        val cursor = readableDatabase.rawQuery("select id,url,fileName,currentBytes,etag,userAgent,mimeType,totalBytes from downloads where finish = 0 order by create_time desc ", null)
        try {
            val ls = ArrayList<DownloadInfo>()
            while (cursor.moveToNext()) {
                ls.add(
                        DownloadInfo(
                                cursor.getLong(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getLong(3),
                                null,
                                cursor.getString(4),
                                cursor.getString(5),
                                cursor.getString(6),
                                cursor.getLong(7)
                        )
                )
            }
            return ls
        } finally {
            cursor.close()
        }
    }

    fun listOne(): DownloadInfo? {
        val cursor = readableDatabase.rawQuery("select id,url,fileName,currentBytes,etag,userAgent,mimeType,totalBytes from downloads where finish = 0 order by create_time desc ", null)
        try {
            if (cursor.moveToNext()) {
                val downloadInfo = DownloadInfo(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        null,
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6),
                        cursor.getLong(7)
                )

                val file = File(downloadInfo.fileName)
                if (file.exists()) {
                    downloadInfo.currentBytes = file.length()
                }
                return downloadInfo
            } else
                return null
        } finally {
            cursor.close()
        }
    }

    fun insert(downloadInfo: DownloadInfo) {
        val values = ContentValues();
        val (_, url, fileName, currentBytes, proxy, etag, userAgent, mimeType, totalBytes, finish) = downloadInfo

        values.put("url", url)
        values.put("fileName", fileName)
        values.put("currentBytes", currentBytes)
        values.put("etag", etag)
        values.put("userAgent", userAgent)
        values.put("mimeType", mimeType)
        values.put("totalBytes", totalBytes)
        values.put("finish", finish)
        values.put("create_time", getTimeStamp())

        writableDatabase.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)

    }

    fun update(downloadInfo: DownloadInfo) {
        val values = ContentValues();
        val (id, url, fileName, currentBytes, proxy, etag, userAgent, mimeType, totalBytes, finish) = downloadInfo
        values.put("url", url)
        values.put("fileName", fileName)
        values.put("currentBytes", currentBytes)
        values.put("etag", etag)
        values.put("userAgent", userAgent)
        values.put("mimeType", mimeType)
        values.put("totalBytes", totalBytes)
        values.put("finish", finish)
        values.put("create_time", getTimeStamp())

        writableDatabase.update("downloads", values, "id = ?", arrayOf("$id"))
    }

    override fun onUpgrade(databae: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "downloads"

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