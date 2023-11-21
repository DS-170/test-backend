package mobi.sevenwinds.app.author

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.select

object AuthorTable : IntIdTable("author") {
    val fio = text("fio")
    val creationDate = datetime("creation_date")

    fun findAuthorById(id: Int): AuthorEntity {
        return AuthorTable
            .select { AuthorTable.id eq id }
            .map { AuthorEntity.wrapRow(it) }
            .singleOrNull() ?: throw RuntimeException("Автор не найден")
    }
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var fio by AuthorTable.fio
    var creationDate by AuthorTable.creationDate

    fun toResponse(): AuthorRecord {
        return AuthorRecord(fio, creationDate)
    }

    fun toAddResponse(): AddAuthorResponse {
        return AddAuthorResponse(id.value)
    }
}
