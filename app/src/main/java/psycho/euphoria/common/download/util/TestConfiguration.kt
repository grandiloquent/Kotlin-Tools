package psycho.euphoria.common.download.util


data class TestConfiguration(
        /**
         * Forced timeout value for every request during test mode is enabled.
         * If set to null, the original timeout will be used.
         * If set to -1, there will be no timeout.
         */
        var timeout: Int? = null,
        /**
         * Forced timeout for read Http operation
         * If set to null, the timeout value will be used.
         * If set to -1, there will be no timeout.
         */
        var timeoutRead: Int? = null,

        /**
         * If set to true, it will block the thread for every request.
         */
        var blocking: Boolean = true) {

    fun coerceTimeout(timeout: Int) = this.timeout?.let { if (it == -1) Int.MAX_VALUE else it } ?: timeout
    fun coerceTimeoutRead(timeout: Int) = this.timeoutRead?.let { if (it == -1) Int.MAX_VALUE else it } ?: timeout
}


