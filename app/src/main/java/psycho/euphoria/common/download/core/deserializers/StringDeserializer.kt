package psycho.euphoria.common.download.core.deserializers

import psycho.euphoria.common.download.core.Deserializable
import psycho.euphoria.common.download.core.Response
import java.nio.charset.Charset

class StringDeserializer(private val charset: Charset = Charsets.UTF_8) : Deserializable<String> {
    override fun deserialize(response: Response): String = String(response.data, charset)
}



