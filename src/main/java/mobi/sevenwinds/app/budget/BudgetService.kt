package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val authorEntity = if (body.author != null) {
                AuthorEntity.new {
                    fio = body.author.fio
                    creationDate = DateTime.now()
                }
            } else {
                null
            }

            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = authorEntity
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val selectAll = BudgetTable
                .select { BudgetTable.year eq param.year }

            val query = BudgetTable
                .select { BudgetTable.year eq param.year }
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
}