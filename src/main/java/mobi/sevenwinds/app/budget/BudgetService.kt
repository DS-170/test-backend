package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecordRequest): BudgetRecordResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.authorId?.let {
                    AuthorTable.findAuthorById(it)
                }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val selectAll = BudgetTable
                .select { BudgetTable.year eq param.year }

            val query = BudgetTable
                .join(AuthorTable, JoinType.LEFT, additionalConstraint = {
                    BudgetTable.authorId eq AuthorTable.id
                })
                .select {
                    if (param.authorFio != null) {
                        (BudgetTable.year eq param.year) and (AuthorTable.fio ilike "%${param.authorFio}%")
                    } else {
                        (BudgetTable.year eq param.year)
                    }
                }
                .limit(param.limit, param.offset)

            val total = selectAll
                .count()

            val data = BudgetEntity.wrapRows(query)
                .map { it.toResponse() }
                .sortedWith(Comparator { r1, r2 ->
                    val monthResult = r1.month.compareTo(r2.month)
                    if (monthResult == 0) {
                        r2.amount - r1.amount
                    } else {
                        monthResult
                    }
                })

            val dataAll = BudgetEntity.wrapRows(selectAll).map { it.toResponse() }

            val sumByType = dataAll.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }

    class ILikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")

    infix fun <T : String?> ExpressionWithColumnType<T>.ilike(pattern: String): Op<Boolean> =
        ILikeOp(this, QueryParameter(pattern, columnType))
}
