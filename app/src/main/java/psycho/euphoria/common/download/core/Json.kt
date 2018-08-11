package psycho.euphoria.common.download.core


import org.json.JSONArray
import org.json.JSONObject

class Json(val content: String) {

    fun obj(): JSONObject = JSONObject(content)

    fun array(): JSONArray = JSONArray(content)

}


