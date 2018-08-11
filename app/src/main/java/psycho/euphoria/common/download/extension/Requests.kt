package psycho.euphoria.common.download.extension


import psycho.euphoria.common.download.core.*
import psycho.euphoria.common.download.result.Result

//jsonObject
fun Request.responseJson(handler: (Request, Response, Result<Json, FuelError>) -> Unit) =
        response(jsonDeserializer(), handler)

fun Request.responseJson(handler: Handler<Json>) = response(jsonDeserializer(), handler)

fun Request.responseJson() = response(jsonDeserializer())

fun jsonDeserializer(): Deserializable<Json> {
    return object : Deserializable<Json> {
        override fun deserialize(response: Response): Json {
            return Json(String(response.data))
        }
    }
}
