package psycho.euphoria.common.download.core.deserializers

import psycho.euphoria.common.download.core.Deserializable
import psycho.euphoria.common.download.core.Response

class ByteArrayDeserializer : Deserializable<ByteArray> {
    override fun deserialize(response: Response): ByteArray = response.data
}



