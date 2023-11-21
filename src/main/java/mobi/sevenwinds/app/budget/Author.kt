package mobi.sevenwinds.app.budget

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.sql.Timestamp
import kotlin.reflect.KProperty

object AuthorTable : IntIdTable("author") {
    val fio = text("fio")
    val creationDate = datetime("creation_date")
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fio by AuthorTable.fio
    var creationDate by AuthorTable.creationDate

    fun toResponse(): AuthorRecord {
        val authorRecord = AuthorRecord(fio)
        authorRecord.creationDate = creationDate
        return authorRecord
    }
}
