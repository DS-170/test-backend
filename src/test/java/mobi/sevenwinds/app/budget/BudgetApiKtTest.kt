package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AddAuthorRequest
import mobi.sevenwinds.app.author.AddAuthorResponse
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecordRequest(2020, 5, 10, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 5, 5, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 5, 20, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 5, 30, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 5, 40, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2030, 1, 1, BudgetType.Расход ))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecordRequest(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecordRequest(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecordRequest(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecordRequest(2020, 1, 30, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 5, 400, BudgetType.Приход ))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testAuthorFilter() {
        val id1 = addAuthor(AddAuthorRequest("Иван Иванов"))
        val id2 = addAuthor(AddAuthorRequest("Валерий Петров"))
        val id3 = addAuthor(AddAuthorRequest("Валерий Иванов"))
        val id4 = addAuthor(AddAuthorRequest("Дмитрий Киров"))

        addRecord(BudgetRecordRequest(2020, 5, 100, BudgetType.Приход, id1))
        addRecord(BudgetRecordRequest(2020, 1, 5, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2020, 6, 50, BudgetType.Приход, id2))
        addRecord(BudgetRecordRequest(2020, 5, 50, BudgetType.Приход, id3))
        addRecord(BudgetRecordRequest(2020, 1, 30, BudgetType.Приход ))
        addRecord(BudgetRecordRequest(2030, 5, 400, BudgetType.Приход, id4))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&authorFio=Иванов")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(2, response.items.size)
                Assert.assertEquals("Иван Иванов", response.items[0].author?.fio)
                Assert.assertEquals("Валерий Иванов", response.items[1].author?.fio)
            }
    }

    @Test
    fun testAuthorNotFound() {

        val response = RestAssured.given()
            .jsonBody(BudgetRecordRequest(2020, 5, 100, BudgetType.Приход, 123))
            .post("/budget/add")

        response.then().assertThat()
            .statusCode(500)

    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecordRequest(2020, -5, 5, BudgetType.Приход ))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecordRequest(2020, 15, 5, BudgetType.Приход ))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(request: BudgetRecordRequest) {
        RestAssured.given()
            .jsonBody(request)
            .post("/budget/add")
            .toResponse<BudgetRecordResponse>().let { response ->
                Assert.assertEquals(request.amount, response.amount)
                Assert.assertEquals(request.year, response.year)
                Assert.assertEquals(request.month, response.month)
                Assert.assertEquals(request.type, response.type)
                if (request.authorId != null){
                    val authorEntity = transaction {
                        return@transaction AuthorTable.findAuthorById(request.authorId!!)
                    }
                    Assert.assertEquals(authorEntity.fio, response.author?.fio)
                    Assert.assertEquals(authorEntity.creationDate, response.author?.creationDate)
                }
            }
    }
    private fun addAuthor(record: AddAuthorRequest) : Int {
        var result : Int
        RestAssured.given()
            .jsonBody(record)
            .post("/author/add")
            .toResponse<AddAuthorResponse>().let { response ->
                val authorEntity = transaction {
                    return@transaction AuthorTable.findAuthorById(response.authorId)
                }

                Assert.assertEquals(authorEntity.id.value, response.authorId)

                result = response.authorId
            }
        return result
    }
}