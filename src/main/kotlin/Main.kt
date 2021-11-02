package org.jetbrains.exposed.dao
//Import der zugehörigen Bibliotheken
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

//Anlegen eines Beispielobjektes (Student)
object Students : IntIdTable() {
    val matnr = integer("matnr").uniqueIndex()
    val lastname = varchar("lastname", length = 50)
    val firstname = varchar("firstname", length = 50)
    val host = integer("hosts_id") references Hosts.hostId

    override val primaryKey = PrimaryKey(matnr, name = "PK_User_ID") // name is optional here
}

//Passende Klasse für die Nutzung von DAO
class Student(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Student>(Students)

    var matnr by Students.matnr
    var lastname     by Students.lastname
    var firstname by Students.firstname
    var host by Students.host
}

//Tabelle für Universitäten anlegen (Hosts)
object Hosts : IntIdTable() {
    val hostId = integer("id").autoIncrement()// Column<Int>
    val name = varchar("name", 50) // Column<String>

    override val primaryKey = PrimaryKey(id, name = "PK_Hosts_ID")
}

//Passende Klasse für die Nutzung von DAO
class Host(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Host>(Hosts)

    var hostId by Hosts.hostId
    var name by Hosts.name
}

fun main() {
    //Verbindung mit der Datenbank herstellen
    Database.connect("jdbc:h2:mysql://test.mydomain.com:port", driver = "com.mysql.jdbc.Driver", user = "nutzername", password = "passwort")

    transaction {
        //Hinzufügen eines Loggers für Debugzwecke
        addLogger(StdOutSqlLogger)
        //Erstellen des Datenbankschemas, sollte es noch nicht existieren
        SchemaUtils.create (Students, Hosts)


        //Anlegen und Sichern der Hochschule Flensburg ID Variante 1(DSL)
        val hsflensburgid = Hosts.insert {
            it[name] = "HS Flensburg"
        } get Hosts.hostId

        //Anlegen und Sichern der UniFlensburg ID Variante 2 (DSL)
        val uniflensburgid = Hosts.insertAndGetId {
            it[name] = "Uni Flensburg"
        }

        //Heraussuchen eines Namens zu einer ID und speichern in einer Variablen (DSL)
        val uniflensburgname = Hosts.select { Hosts.id eq uniflensburgid }.single()[Hosts.name]

        //Hinzufügen eines Studenten (DSL)
        Students.insert {
            it[matnr] = 670001
            it[firstname] = "Max"
            it[lastname] = "Mustermann"
            it[host] = hsflensburgid
        }

        //Hinzufügen eines weiteren Studenten (DSL)
        Students.insert {
            it[matnr] = 670002
            it[firstname] = "notexisting"
            it[lastname] = "Nutzer"
            it[host] = hsflensburgid
        }

        //Hinzufügen eines Studenten (DAO)
        val newStudent = Student.new {
            matnr = 670003
            firstname = "Erika"
            lastname = "Mustermann"
            host = hsflensburgid
        }

        //Update eines Studenten (DSL)
        Students.update({ Students.firstname eq "Max"}) {
            it[firstname] = "Maximilian"
        }

        //Löschen eines Eintrages (DSL)
        Students.deleteWhere{ Students.firstname like "%existing"}

        //Ausgabe aller Hochschulen (DSL)
        println("All Universities:")
        for (host in Hosts.selectAll()) {
            println("${host[Hosts.id]}: ${host[Hosts.name]}")
        }

        //Gibt alle Hochschulen in einer Liste aus(DAO)
        println("Hochschulen: ${Host.all().joinToString { it.name }}")

    }
}