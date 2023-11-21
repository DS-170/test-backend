package mobi.sevenwinds.app.budget

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.IOException

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetRecord, BudgetRecord>(info("Добавить запись")) { param, body ->
            respond(BudgetService.addRecord(body))
        }

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))
            }
        }
    }
}

data class BudgetRecord(
    @Min(1900) val year: Int,
    @Min(1) @Max(12) val month: Int,
    @Min(1) val amount: Int,
    val type: BudgetType,
    val author: AuthorRecord?
)


data class AuthorRecord(
    val fio: String,
) {
    @JsonDeserialize(using = CustomDateTimeDeserializer::class)
    @JsonSerialize(using = CustomDateTimeSerializer::class)
    var creationDate: DateTime = DateTime.now()
}

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

data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetRecord>
)

enum class BudgetType {
    Приход, Расход
}