package mobi.sevenwinds.app.author

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.IOException

private const val DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS"

class CustomDateTimeDeserializer : JsonDeserializer<DateTime>() {
    @Throws(IOException::class)
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): DateTime {
        val dateString = parser.text
        val formatter = DateTimeFormat.forPattern(DATE_TIME_PATTERN)
        return formatter.parseDateTime(dateString)
    }
}

class CustomDateTimeSerializer : JsonSerializer<DateTime>() {
    @Throws(IOException::class)
    override fun serialize(value: DateTime?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
        } else {
            val formatter = DateTimeFormat.forPattern(DATE_TIME_PATTERN)
            gen.writeString(formatter.print(value))
        }
    }
}
