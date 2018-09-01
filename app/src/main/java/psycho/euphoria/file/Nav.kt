package psycho.euphoria.file


class Native {
    external fun deleteFile(path: String)

    companion object {
        private var instance: Native? = null
        fun getInstacne(): Native = instance ?: Native().also { instance = it };


        init {
            System.loadLibrary("native-lib")
        }
    }
}