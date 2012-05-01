package crypt

import java.io.File
import java.io.PrintWriter
import org.apache.commons.io.FileUtils
import org.jasypt.util.text.BasicTextEncryptor as TextEncryptor
import auth.AuthDb
import auth.User
import auth.Role

// Algorithm: PBEWithMD5AndDES
// Key obtention iterations: 1000

// if to == null then use from
fun encryptFile(key : String, from : File, to : File? = null) {
    val content = from.getLines()
    val encryptor = TextEncryptor()
    encryptor.setPassword(key)
    val writer = PrintWriter(to ?: from)
    writer.println(encryptor.encrypt(content))
    writer.close()
}

// if to == null then use from, always returns decrypted content
fun decryptFile(key : String, from : File, onlyRead : Boolean = false, to : File? = null) : String {
    val encryptor = TextEncryptor()
    encryptor.setPassword(key)
    val content = encryptor.decrypt(from.getLines()).sure()
    if (!onlyRead) {
        FileUtils.writeStringToFile(to ?: from, content)
    }
    return content
}

fun File.getLines() = FileUtils.readFileToString(this)!!
