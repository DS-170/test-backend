package mobi.sevenwinds.app.author

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import org.joda.time.DateTime

fun NormalOpenAPIRoute.author() {
    route("/author") {
        route("/add").post<Unit, AddAuthorResponse, AddAuthorRequest>(info("Добавить запись")) { param, body ->
            respond(AuthorService.addRecord(body))
        }
    }
}

data class AuthorRecord(
    val fio: String,
    @JsonDeserialize(using = CustomDateTimeDeserializer::class)
    @JsonSerialize(using = CustomDateTimeSerializer::class)
    var creationDate: DateTime
)

data class AddAuthorRequest(
    val fio: String,
)

data class AddAuthorResponse(
    val authorId: Int
)
