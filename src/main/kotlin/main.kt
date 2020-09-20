import com.example.db.DbHelper
import com.example.db.LogTree
import com.example.db.genLong
import com.example.db.genString
import tables.TableA
import tables.TableB
import tables.TableC
import timber.log.Timber
import timber.log.debug
import kotlin.concurrent.thread

/**
 * I run two threads. Each thread selects all records from TableA, TableB and TableC
 *
 * Sometimes error is: java.sql.SQLException: database in auto-commit mode
 * And sometimes is: org.sqlite.SQLiteException: \[SQLITE_ERROR\] SQL error or missing database (cannot commit - no transaction is active)
 */
fun main(args: Array<String>) {
    Timber.plant(LogTree())
    val dbHelper = DbHelper("data.db", "WAL", true)
    insertSomeData(dbHelper)

    selectsThatFail(dbHelper)

    /**
     * This works
     * Uncomment `this` and comment `fails loop` to see that it works
     */
//    selectsThatWork(dbHelper)

    Thread.sleep(3000)
    dbHelper.close()
}

fun selectsThatFail(dbHelper: DbHelper) {
    for (x in 0..1) {
        thread(true) {
            selectWithTransactions(dbHelper)
        }
    }
}

fun selectsThatWork(dbHelper: DbHelper) {
    for (x in 0..1) {
        thread(true) {
            selectWithoutTransactions(dbHelper)
        }
    }
}

private fun selectWithTransactions(dbHelper: DbHelper) {
    dbHelper.database.transaction {
        val aList = dbHelper.database.tableAQueries.selectAll().executeAsList()
        Timber.debug { "selected from A ${aList.size}" }
    }

    dbHelper.database.transaction {
        val bList = dbHelper.database.tableBQueries.selectAll().executeAsList()
        Timber.debug { "selected from B ${bList.size}" }
    }

    dbHelper.database.transaction {
        val cList = dbHelper.database.tableCQueries.selectAll().executeAsList()
        Timber.debug { "selected from C ${cList.size}" }
    }
}

private fun selectWithoutTransactions(dbHelper: DbHelper) {
    val aList = dbHelper.database.tableAQueries.selectAll().executeAsList()
    Timber.debug { "selected from A ${aList.size}" }

    val bList = dbHelper.database.tableBQueries.selectAll().executeAsList()
    Timber.debug { "selected from B ${bList.size}" }

    val cList = dbHelper.database.tableCQueries.selectAll().executeAsList()
    Timber.debug { "selected from C ${cList.size}" }
}

fun insertSomeData(dbHelper: DbHelper) {
    Timber.debug { "insertSomeData: 100 records to each table" }
    for (i in 0..100) {
        val a = TableA(0, genString(10), genString(10), genLong())
        dbHelper.database.tableAQueries.insert(a)
//        Timber.debug { "inserted ${a.id} ${a.f1} ${a.f2} ${a.f3}" }

        val b = TableB(0, genString(10), genString(10), genLong())
        dbHelper.database.tableBQueries.insert(b)
//        Timber.debug { "inserted ${b.id} ${b.f1} ${b.f2} ${b.f3}" }

        val c = TableC(0, genString(10), genString(10), genLong())
        dbHelper.database.tableCQueries.insert(c)
//        Timber.debug { "inserted ${c.id} ${c.f1} ${c.f2} ${c.f3}" }
    }
}
