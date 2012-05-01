package auth

import crypt.decryptFile
import crypt.encryptFile
import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.List
import java.util.Map
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.io.FileUtils
import ui.et.Column
import ui.et.StringValue
import ui.et.Value
import java.text.NumberFormat

class AuthDb(val dbPath : String) {
    // login -> user
    public val users : Map<String, User> = LinkedHashMap<String, User>()
    public val confs : Map<String, String> = LinkedHashMap<String, String>()

    fun load(val encrypted : Boolean = false) {
        if(encrypted) {
            decryptFile(ui.key, File(dbPath))
        }

        users.clear()
        for (line in FileUtils.readLines(File(dbPath))) {
            val split = line.sure().split(" ").sure()
            if(split.size == 5) {
                val login = split[0].sure().fromB64()
                val password = split[1].sure().fromB64()
                val role = if (Integer.parseInt(split[2]) == 0) Role.ADMIN else Role.USER
                val lastModified = Long.parseLong(split[3]!!)
                val strange = split[4]!!.fromB64()

                users.put(login, User(login, password, role, lastModified, strange))
            } else if(split.size == 2) {
                val key = split[0]!!.fromB64()
                val value = split[1]!!.fromB64()

                confs.put(key, value)
            }
        }

        if (encrypted){
            encryptFile(ui.key, File(dbPath))
        }
    }

    fun save(val encrypt : Boolean = false) {
        val list = ArrayList<Any?>()
        list.addAll(users.b64Values())
        list.addAll(confs.b64Values2())
        FileUtils.writeLines(File(dbPath), list)
        if (encrypt) {
            encryptFile(ui.key, File(dbPath))
        }
    }
}

enum class Role {
    ADMIN
    USER
}

class User(val login : String, val password : String, val role : Role, val lastModified : Long, val passwordStrange : String) {

    //todo think about password hashing

    fun toString() = "${login} ${password} ${if (role == Role.ADMIN) 0 else 1} $lastModified strange=${passwordStrange}"

    fun asB64String() = "${login.toB64()} ${password.toB64()} ${if (role == Role.ADMIN) 0 else 1} $lastModified ${passwordStrange.toB64()}"

    fun asColumns() : List<Value> {
        val columns = ArrayList<Value>()

        columns.add(StringValue(login))
        columns.add(StringValue(password))
        columns.add(StringValue(if (role == Role.ADMIN) "admin" else "user"))
        columns.add(StringValue(lastModified.toString()))
        columns.add(StringValue(passwordStrange))

        return columns
    }

    class object {
        public val columns : List<Column> = arrayList(Column("login"), object : Column("password", true){
            public override fun onChanged(val value : String) : List<#(Int, Any?)>? = arrayList(#(3, StringValue(System.currentTimeMillis().toString())), #(4, StringValue(calcPasswordStrange(value))))
        }, Column("role"), Column("lastModified"), Column("Strange", false));

        public fun fromColumns(val it : List<Value>) : User = User(
                (it[0] as StringValue).str,
                (it[1] as StringValue).str,
                if(it[2] is StringValue && (it[2] as StringValue).str.equals("admin")) Role.ADMIN else Role.USER,
                (it[3] as StringValue).str.toLong(),
                (it[4] as StringValue).str
        )

        public fun calcPasswordStrange(val password : String) : String {
            val db = AuthDb("database")
            db.load(true)

            val bruteForceSpeed = db.confs.get("password bruteforce speed")!!.toDouble()
            val maxLifeTime = db.confs.get("max password life time")!!.toDouble()
            val alphabetSize = db.confs.get("alphabet size")!!.toDouble()
            val passwordSpace = Math.pow(alphabetSize, password.length().toDouble())
            val value = Math.min(bruteForceSpeed * maxLifeTime / passwordSpace, 100.toDouble())
            val format = NumberFormat.getInstance()!!
            format.setMaximumFractionDigits(4)

            return "${format.format(100 - value)}%"
        }
    }
}

fun Map<String, User>.b64Values() : List<String> {
    val result = LinkedList<String>()

    for (value in this.values()) {
        result.add(value.asB64String())
    }

    return result
}

fun Map<String, String>.b64Values2() : List<String> {
    val result = LinkedList<String>()

    for (pair in this.entrySet()) {
        result.add(pair.getKey().toB64() + " " + pair.getValue().toB64())
    }

    return result
}

fun String.toB64() = Base64.encodeBase64URLSafeString(this.getBytes()).sure()

fun String.fromB64() = StringUtils.newStringUtf8(Base64.decodeBase64(this.getBytes())).sure()

fun checkCredentials(val login : String, val password : String, val key : String = "test key", val dbPath : String = "database") : Boolean {
    val db = AuthDb(dbPath)
    db.load(true)
    val result = db.users.get(login)?.password.equals(password)

    if (db.users.size == 0) {
        return true
    }

    return result
}

fun getUser(val login : String, val dbPath : String = "database") : User? {
    val db = AuthDb(dbPath)
    db.load(true)

    return db.users.get(login)
}

fun main(args : Array<String>) {
    val db = AuthDb("database")
    db.users.put("admin", User("admin", "admin", Role.ADMIN, 0, "0%"))
    db.save(true)
}
