package psycho.euphoria.common.download.util


import android.os.Handler
import android.os.Looper
import psycho.euphoria.common.download.core.Environment
import java.util.concurrent.Executor

internal class AndroidEnvironment : Environment {

    val handler = Handler(Looper.getMainLooper())

    override var callbackExecutor: Executor = Executor { command -> handler.post(command) }

}