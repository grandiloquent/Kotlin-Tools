package psycho.euphoria.tools
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_translator.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class TranslatorActivity : AppCompatActivity() {
    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mHandler: Handler
    private var mTargetLanguage = "en"
    private val mHandlerCallback = Handler.Callback { message ->
        when (message.what) {
            MSG_QUERY -> {
                query(message.obj as String)
            }
        }
        true
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translator)
        query_button.setOnClickListener {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUERY, edit_text.text.toString()))
        }
    }
    private fun query(str: String) {
        try {
            val url = generateTranslateURL(str, mTargetLanguage)
            val request = Request.Builder()
                    .url(url).build()
            val res = OkHttpClient.Builder()
                    .connectTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
                    .build().newCall(request).execute()
            if (res.isSuccessful) {
                res.body()?.let {
                    text_view.text = parseJSON(it.string())
                }
            } else {
                text_view.text = res.message()
            }
        } catch (e: Exception) {
            text_view.setText(e.message)
        }
    }
    private fun parseJSON(str: String): String? {
        val obj = JSONObject(str)
        if (obj.has("sentences")) {
            val sentences = obj.getJSONArray("sentences")
            if (sentences.length() < 1) return null
            val sb = StringBuilder()
            for (i in 0 until sentences.length()) {
                sb.append(sentences.getJSONObject(i).getString("trans"))
            }
            return sb.toString()
        }
        return null
    }
    private fun initialize() {
        mHandlerThread = HandlerThread("TranslatorActivity")
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper, mHandlerCallback)
    }
    override fun onStart() {
        initialize()
        super.onStart()
    }
    override fun onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null)
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit()
        }
        super.onStop()
    }
    fun generateTranslateURL(str: String, targetLanguage: String, sourceLanguage: String = "auto"): String {
        return "https://translate.google.cn/translate_a/single?client=gtx&sl=$sourceLanguage&tl=$targetLanguage&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=${Uri.decode(str)}";
    }
    companion object {
        const val MSG_QUERY = 1
        const val TIME_OUT = 1000 * 20L
    }
}