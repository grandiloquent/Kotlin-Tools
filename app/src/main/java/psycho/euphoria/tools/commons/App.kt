package psycho.euphoria.tools.commons

import android.app.Application
import com.squareup.leakcanary.LeakCanary
import psycho.euphoria.tools.BuildConfig

class App : Application() {

    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
//            if (LeakCanary.isInAnalyzerProcess(this)) {
//                return
//            }
//            // Monitor memory leaks in debug mode
//            LeakCanary.install(this)
        }
    }
}