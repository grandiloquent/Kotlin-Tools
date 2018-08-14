package psycho.euphoria.common

import android.app.Application
import psycho.euphoria.common.util.ThreadPool
import psycho.euphoria.tools.BuildConfig

class App : Application() {

    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }

    val threadPool by lazy {
        ThreadPool()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Services.context = this
        if (BuildConfig.DEBUG) {
//            if (LeakCanary.isInAnalyzerProcess(this)) {
//                return
//            }
//            // Monitor memory leaks in debug mode
//            LeakCanary.install(this)
        }
    }
}