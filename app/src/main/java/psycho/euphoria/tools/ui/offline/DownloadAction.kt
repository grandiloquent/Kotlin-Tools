package psycho.euphoria.tools.ui.offline

import java.io.*
import java.lang.IllegalStateException

abstract class DownloadAction(
        private val type: String,
        private val version: Int,
        private val isRemoveAction: Boolean,
        private val data: ByteArray

) {
    abstract fun writeToStream(outputStream: DataOutputStream)

    fun toByteArray(): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            serializeToStream(this, output)
        } catch (e: IOException) {
            throw IllegalStateException()
        }
        return output.toByteArray()
    }


    companion object {
        fun serializeToStream(action: DownloadAction, output: OutputStream) {
            val dataOutputStream = DataOutputStream(output)
            dataOutputStream.writeUTF(action.type)
            dataOutputStream.writeInt(action.version)
            action.writeToStream(dataOutputStream)
            dataOutputStream.flush()
        }

        fun deserializeFromStream(deserializers: Array<Deserializer>,
                                  input: InputStream): DownloadAction {
            val dataInputStream = DataInputStream(input)
            val type = dataInputStream.readUTF()
            val version = dataInputStream.readInt()
            for (deserializer in deserializers) {
                if (type.equals(deserializer.type) && deserializer.version >= version) {
                    return deserializer.readFromStream(version, dataInputStream)
                }
            }
            throw DownloadException("No deserializer found for:$type,$version")
        }
    }

    abstract class Deserializer(val type: String, val version: Int) {
        abstract fun readFromStream(version: Int, input: DataInputStream): DownloadAction
    }

}