package psycho.euphoria.download

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import psycho.euphoria.tools.commons.App
import java.io.File

class DownloadTaskProvider(context: Context = App.instance) : SQLiteOpenHelper(context,
        File(Environment.getExternalStorageDirectory(), DATANAME).absolutePath,
        null,
        VERSION) {

    override fun onCreate(database: SQLiteDatabase) {
        val sb = StringBuilder()



        sb.append("CREATE TABLE `tasks` (\r\n")

        sb.append("\t`_id`\tINTEGER,\r\n")

        sb.append("\t`finished`\tINTEGER NOT NULL,\r\n")

        sb.append("\t`uri`\tTEXT NOT NULL UNIQUE,\r\n")

        sb.append("\t`filename`\tTEXT NOT NULL UNIQUE,\r\n")

        sb.append("\t`failed`\tINTEGER,\r\n")

        sb.append("\t`create_time`\tINTEGER,\r\n")

        sb.append("\t`etag`\tTEXT,\r\n")

        sb.append("\t`current_bytes`\tINTEGER,\r\n")

        sb.append("\t`total_bytes`\tINTEGER,\r\n")

        sb.append("\tPRIMARY KEY(`_id`)\r\n")

        sb.append(");\n")

        database.execSQL(sb.toString())

    }

    fun insert(uri: String,
               filename: String,
               failed: Int = 0,
               finished: Int = 0,
               current_bytes: Long = 0L,
               total_bytes: Long = 0L) {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_CREATE_TIME, System.currentTimeMillis())
        contentValues.put(COLUMN_CURRENT_BYTES, current_bytes)
        contentValues.put(COLUMN_FAILED, failed)
        contentValues.put(COLUMN_FILENAME, filename)
        contentValues.put(COLUMN_FINISHED, finished)
        contentValues.put(COLUMN_TOTAL_BYTES, total_bytes)
        contentValues.put(COLUMN_URI, uri)
        writableDatabase.insertWithOnConflict(TABLE_NAME_TASKS, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun listTasks(): MutableList<Request> {
        val list = mutableListOf<Request>()
        //
        val cursor = readableDatabase.rawQuery("SELECT _id,uri,filename,etag,current_bytes,total_bytes,failed from tasks where finished = 0 and failed <= 5", null);
        try {
            while (cursor.moveToNext()) {
                val downloadInfo = Request(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        0L,//  cursor.getLong(4),
                        0L,// cursor.getLong(5),
                        cursor.getInt(6),
                        0

                )
                list.add(downloadInfo)
            }
        } finally {
            cursor.close()
        }
        return list

    }

    fun update(_id: Long,
               uri: String? = null,
               filename: String? = null,
               failed: Int = 0,
               finished: Int = 0,
               current_bytes: Long = 0L,
               total_bytes: Long = 0L) {
        val contentValues = ContentValues()
        contentValues.put(COLUMN_CURRENT_BYTES, current_bytes)
        contentValues.put(COLUMN_FAILED, failed)
        if (filename != null)
            contentValues.put(COLUMN_FILENAME, filename)
        contentValues.put(COLUMN_FINISHED, finished)
        contentValues.put(COLUMN_TOTAL_BYTES, total_bytes)
        if (uri != null)
            contentValues.put(COLUMN_URI, uri)
        writableDatabase.updateWithOnConflict(TABLE_NAME_TASKS, contentValues, "$COLUMN_ID = ?", arrayOf("$_id"), SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun update(downloadInfo: Request) {
        val  contentValues = ContentValues()
        contentValues.put(COLUMN_CURRENT_BYTES, downloadInfo.currentBytes)
        contentValues.put(COLUMN_FAILED, downloadInfo.failedCount)
        contentValues.put(COLUMN_FILENAME, downloadInfo.fileName)
        contentValues.put(COLUMN_FINISHED, downloadInfo.finish)
        contentValues.put(COLUMN_TOTAL_BYTES, downloadInfo.totalBytes)
        contentValues.put(COLUMN_URI, downloadInfo.uri)
        writableDatabase.updateWithOnConflict(TABLE_NAME_TASKS, contentValues, "$COLUMN_ID = ?", arrayOf("${downloadInfo.id}"), SQLiteDatabase.CONFLICT_IGNORE)

    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }

    companion object {
        private const val VERSION = 1
        private const val DATANAME = "donwload_task.db"
        private const val TABLE_NAME_TASKS = "tasks"
        private const val COLUMN_ID = "_id"
        private const val COLUMN_FINISHED = "finished"
        private const val COLUMN_URI = "uri"
        private const val COLUMN_FILENAME = "filename"
        private const val COLUMN_FAILED = "failed"
        private const val COLUMN_CREATE_TIME = "create_time"
        private const val COLUMN_CURRENT_BYTES = "current_bytes"
        private const val COLUMN_TOTAL_BYTES = "total_bytes"
        private const val COLUMN_ETAG = "etag"

        private var instance: DownloadTaskProvider? = null

        fun getInstance(): DownloadTaskProvider {
            return instance ?: synchronized(this) {
                DownloadTaskProvider().also {
                    instance = it
                }
            }
        }
    }
}
