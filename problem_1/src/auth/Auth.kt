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
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import ui.et.Column
import ui.et.StringValue
import ui.et.Value

class AuthDb(val dbPath : String) {
    // login -> user
    public val users : Map<String, User> = LinkedHashMap<String, User>()
    public val infos : List<Info> = LinkedList<Info>()

    fun load(val encrypted : Boolean = false) {
        if(encrypted) {
            decryptFile(ui.key, File(dbPath))
        }

        users.clear()
        for (line in FileUtils.readLines(File(dbPath))) {
            val split = line.sure().split(" ").sure()
            if(split.size == 4) {
                val login = split[0].sure().fromB64()
                val password = split[1].sure().fromB64()
                val role = if (Integer.parseInt(split[2]) == 0) Role.ADMIN else if (Integer.parseInt(split[2]) == 1) Role.EDITOR else Role.USER
                val lastModified = Long.parseLong(split[3])

                users.put(login, User(login, password, role, lastModified))
            } else if(split.size == 5) {
                val name = split[0].fromB64()
                val middleName = split[1].fromB64()
                val lastName = split[2].fromB64()
                val phone = split[3].fromB64()
                val address = split[4].fromB64()

                infos.add(Info(name, middleName, lastName, phone, address))
            }
        }

        if (encrypted){
            encryptFile(ui.key, File(dbPath))
        }
    }

    fun save(val encrypt : Boolean = false) {
        val list = ArrayList<Any?>()
        list.addAll(users.b64Values())
        list.addAll(infos.b64Values2())
        FileUtils.writeLines(File(dbPath), list)
        if (encrypt) {
            encryptFile(ui.key, File(dbPath))
        }
    }
}

enum class Role {
    ADMIN
    EDITOR
    USER
}

class User(val login : String, val password : String, val role : Role, val lastModified : Long) {

    //todo think about password hashing

    fun toString() = "${login} ${password} ${if (role == Role.ADMIN) 0 else if (role == Role.EDITOR) 1 else 2} $lastModified"

    fun asB64String() = "${login.toB64()} ${password.toB64()} ${if (role == Role.ADMIN) 0 else if (role == Role.EDITOR) 1 else 2} $lastModified"

    fun asColumns() : List<Value> {
        val columns = ArrayList<Value>()

        columns.add(StringValue(login))
        columns.add(StringValue(password))
        columns.add(StringValue(if (role == Role.ADMIN) "admin" else if (role == Role.EDITOR) "editor" else "user"))
        columns.add(StringValue(lastModified.toString()))

        return columns
    }

    class object {
        public val columns : List<Column> = arrayList(Column("login"), object : Column("password", true){
            public override fun onChanged(val value : String) : List<#(Int, Any?)>? = arrayList(#(1, StringValue(value.sha())))
        }, Column("role"), Column("lastModified"));

        public fun fromColumns(val it : List<Value>) : User = User(
                (it[0] as StringValue).str,
                (it[1] as StringValue).str,
                if(it[2] is StringValue && (it[2] as StringValue).str.equals("admin")) Role.ADMIN else
                    (if(it[2] is StringValue && (it[2] as StringValue).str.equals("editor")) Role.EDITOR else Role.USER),
                (it[3] as StringValue).str.toLong()
        )
    }
}

class Info (val name : String, val middleName : String, val lastName : String, val phone : String, val address : String) {
    fun asB64String() = "${name.toB64()} ${middleName.toB64()} ${lastName.toB64()} ${phone.toB64()} ${address.toB64()}"

    fun asColumns() : List<Value> = arrayList(
            StringValue(name),
            StringValue(middleName),
            StringValue(lastName),
            StringValue(phone),
            StringValue(address)
    )

    class object {
        public val editableColumns : List<Column> = arrayList(Column("name"), Column("middle name"), Column("last name"), Column("phone"), Column("address"))

        public val columns : List<Column> = arrayList(Column("name", false), Column("middle name", false), Column("last name", false), Column("phone", false), Column("address", false))

        public fun fromColumns(val it : List<Value>) : Info = Info(
                (it[0] as StringValue).str,
                (it[1] as StringValue).str,
                (it[2] as StringValue).str,
                (it[3] as StringValue).str,
                (it[4] as StringValue).str
        )

    }
}

fun Map<String, User>.b64Values() : List<String> {
    val result = LinkedList<String>()

    for (value in this.values()) {
        result.add(value.asB64String())
    }

    return result
}

fun List<Info>.b64Values2() : List<String> {
    val result = LinkedList<String>()

    for (entry in this) {
        result.add(entry.asB64String())
    }

    return result
}

fun String.toB64() = Base64.encodeBase64URLSafeString(this.getBytes()).sure()

fun String.fromB64() = StringUtils.newStringUtf8(Base64.decodeBase64(this.getBytes())).sure()

fun checkCredentials(val login : String, val password : String, val key : String = "test key", val dbPath : String = "database") : Boolean {
    val db = AuthDb(dbPath)
    db.load(true)
    val result = db.users.get(login)?.password.equals(password.sha())

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

fun String.sha() = DigestUtils.sha256Hex(this)!!
