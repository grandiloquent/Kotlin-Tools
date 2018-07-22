package psycho.euphoria.tools.videos

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import psycho.euphoria.tools.commons.*
import java.io.Serializable
import java.util.*

@Entity(tableName = "mdeida", indices = [(Index(value = "full_path", unique = true))])
data class Medium(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "fileName") var name: String,
        @ColumnInfo(name = "full_path") var path: String,
        @ColumnInfo(name = "parent_path") var parentPath: String,
        @ColumnInfo(name = "last_modified") val modified: Long,
        @ColumnInfo(name = "date_taken") var taken: Long,
        @ColumnInfo(name = "size") val size: Long,
        @ColumnInfo(name = "type") val type: Int,
        @ColumnInfo(name = "is_favorite") var isFavorite: Boolean,
        @ColumnInfo(name = "deleted_ts") var deletedTS: Long) : Serializable, ThumbnailItem() {

    fun isGif() = type == TYPE_GIFS
    fun isImage() = type == TYPE_IMAGES
    fun isVideo() = type == TYPE_VIDEOS
    fun isRaw() = type == TYPE_RAWS
    fun isHidden() = name.startsWith('.')
    fun getIsInRecycleBin() = deletedTS != 0L
    fun getBubbleText(sorting: Int) = when {
        sorting and SORT_BY_NAME != 0 -> name
        sorting and SORT_BY_PATH != 0 -> path
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate()
        else -> taken.formatDate()
    }

    fun getGroupingKey(groupBy: Int) = when {
        groupBy and GROUP_BY_LAST_MODIFIED != 0 -> getDayStartTS(modified)
        groupBy and GROUP_BY_DATE_TAKEN != 0 -> getDayStartTS(taken)
        groupBy and GROUP_BY_FILE_TYPE != 0 -> type.toString()
        groupBy and GROUP_BY_EXTENSION != 0 -> name.getFilenameExtension().toLowerCase()
        groupBy and GROUP_BY_FOLDER != 0 -> parentPath
        else -> ""
    }

    private fun getDayStartTS(ts: Long): String {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis.toString()
    }

    companion object {
        private const val serialVersionUID = -6553149366975655L
    }
}

open class ThumbnailItem