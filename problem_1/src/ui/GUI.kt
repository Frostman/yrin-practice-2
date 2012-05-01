package ui

import auth.AuthDb
import auth.Info
import auth.Role
import auth.User
import auth.checkCredentials
import crypt.decryptFile
import crypt.encryptFile
import java.awt.Color
import java.awt.Frame
import java.awt.Toolkit
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPasswordField
import javax.swing.JTextField
import javax.swing.WindowConstants
import javax.swing.text.JTextComponent
import org.apache.commons.codec.digest.DigestUtils
import ui.et.EditableTable

public val key : String = "test key"


fun String.sha() = DigestUtils.sha256Hex(this)!!

fun main(args : Array<String>) {
    val dbPath = "database"

    if(!File(dbPath).exists()) {
        val db = AuthDb("database")
        db.users.put("admin", User("admin", "admin".sha(), Role.ADMIN, 0))

        db.save()
        encryptFile(key, File("database"))
    }

    val result = askForCredentials(null, {checkCredentials(it._1, it._2, key, dbPath)})

    println("auth: ${result._1}")

    if (result._1 == true) {
        showVariantsWindow(null, result._2)
    } else {
        System.exit(- 1)
    }
}

fun askForCredentials(val parent : Frame? = null, checker : (#(String, String))->Boolean) : #(Boolean, String, String) {
    val dialog = JDialog(parent, true)

    dialog.setTitle("Enter login and password")
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    dialog.setResizable(false)
    dialog.getContentPane()?.setLayout(null)
    val width = 320
    val height = 170
    dialog.setSize(width, height)
    val screenSize = Toolkit.getDefaultToolkit()?.getScreenSize()!!
    val x = (screenSize.width - width) / 2
    val y = (screenSize.height - height) / 2
    dialog.setLocation(x.toInt(), y.toInt())

    val loginField = JTextField()
    loginField.setBounds(60, 10, 200, 24)
    loginField.addFocusListener(PlaceHolder(loginField, "login"))
    dialog.getContentPane()?.add(loginField)

    val passwordField = JPasswordField()
    passwordField.setBounds(60, 44, 200, 24)
    passwordField.addFocusListener(PlaceHolder(passwordField, "password"))
    dialog.getContentPane()?.add(passwordField)

    val errorLabel = JLabel("")
    errorLabel.setBounds(60, 78, 200, 24)
    errorLabel.setHorizontalTextPosition(0)
    errorLabel.setVerticalTextPosition(0)
    errorLabel.setForeground(Color.RED)

    dialog.getContentPane()?.add(errorLabel)

    val okButton = JButton("Ok")
    okButton.setBounds(57, 112, 100, 24)
    dialog.getContentPane()?.add(okButton)

    val cancelButton = JButton("Cancel")
    cancelButton.setBounds(163, 112, 100, 24)
    dialog.getContentPane()?.add(cancelButton)

    var auth = false

    val clickHandler = object : MouseAdapter() {
        public override fun mouseClicked(e : MouseEvent?) {
            when (e?.getSource()) {
                okButton ->
                    if(!checker(#(loginField.getText()!!, passwordField.getText()!!))) {
                        errorLabel.setText("Incorrect login-password pair")
                    } else {
                        auth = true
                        dialog.hide()
                    }
                cancelButton -> dialog.hide()
                else -> println("unknown click source")
            }
        }
    }

    okButton.addMouseListener(clickHandler)
    cancelButton.addMouseListener(clickHandler)

    dialog.setVisible(true)

    return #(auth, loginField.getText()!!, passwordField.getText()!!)
}

class PlaceHolder(val component : JTextComponent, val placeholder : String) : FocusListener {
    var componentColor : Color? = null
    var placeholderMode = false

    {
        focusLost(null)
    }

    public override fun focusGained(e : FocusEvent?) {
        if (placeholderMode) {
            component.setForeground(componentColor)
            component.setText("")
            placeholderMode = false
        }
    }

    public override fun focusLost(e : FocusEvent?) {
        componentColor = component.getForeground()
        if (component.getText()?.trim()?.length == 0) {
            component.setForeground(Color.GRAY)
            component.setText(placeholder)
            placeholderMode = true
        }
    }
}

fun showVariantsWindow(val parent : Frame? = null, val username : String) {
    val user = auth.getUser(username)!!

    val dialog = JDialog(parent, true)

    dialog.setTitle("What do you want?")
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    dialog.setResizable(false)
    dialog.getContentPane()?.setLayout(null)

    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

    val width = 320
    val height = 190
    dialog.setSize(width, height)
    val screenSize = Toolkit.getDefaultToolkit()?.getScreenSize()!!
    val x = (screenSize.width - width) / 2
    val y = (screenSize.height - height) / 2
    dialog.setLocation(x.toInt(), y.toInt())

    val loginField = JTextField("login: " + username)
    loginField.setBounds(60, 10, 200, 24)
    loginField.setEditable(false)
    dialog.getContentPane()?.add(loginField)

    val roleField = JTextField("role: " + if (user.role == Role.ADMIN) "admin" else  if (user.role == Role.EDITOR) "editor" else "user")
    roleField.setBounds(60, 44, 200, 24)
    roleField.setEditable(false)
    dialog.getContentPane()?.add(roleField)

    val usersButton = JButton("Users")
    usersButton.setBounds(60, 78, 80, 25)
    if(user.role != Role.ADMIN) {
        usersButton.setEnabled(false)
    }
    dialog.getContentPane()?.add(usersButton)

    val infoButton = JButton("Info")
    infoButton.setBounds(60, 108, 80, 25)
    dialog.getContentPane()?.add(infoButton)

    val exitButton = JButton("Exit")
    exitButton.setBounds(60, 138, 80, 25)
    dialog.getContentPane()?.add(exitButton)

    val encryptButton = JButton("Encrypt")
    encryptButton.setBounds(180, 78, 80, 25)
    dialog.getContentPane()?.add(encryptButton)

    val decryptButton = JButton("Decrypt")
    decryptButton.setBounds(180, 108, 80, 25)
    dialog.getContentPane()?.add(decryptButton)

    val clickHandler = object : MouseAdapter() {
        public override fun mouseClicked(e : MouseEvent?) {
            when (e?.getSource()) {
                usersButton -> {
                    if(user.role == Role.ADMIN) {
                        showUsersListWindow(username)
                        dialog.dispose()
                    }
                }

                infoButton -> {
                    showInfoWindow(username)
                    dialog.dispose()
                }

                exitButton -> {
                    System.exit(- 1)
                }

                encryptButton -> {
                    val fc = JFileChooser()
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
                    fc.setMultiSelectionEnabled(false)
                    if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(dialog)){
                        val file = fc.getSelectedFile()!!
                        encryptFile(user.login + "#" + user.password, file, File(file.getAbsolutePath() + ".enc"))
                    }
                }

                decryptButton -> {
                    val fc = JFileChooser()
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
                    fc.setMultiSelectionEnabled(false)
                    if(JFileChooser.APPROVE_OPTION == fc.showOpenDialog(dialog)){
                        val file = fc.getSelectedFile()!!
                        decryptFile(user.login + "#" + user.password, file, false, File(file.getAbsolutePath() + ".dec"))
                    }
                }

                else -> println("unknown click source")
            }
        }
    }

    usersButton.addMouseListener(clickHandler)
    infoButton.addMouseListener(clickHandler)
    exitButton.addMouseListener(clickHandler)
    encryptButton.addMouseListener(clickHandler)
    decryptButton.addMouseListener(clickHandler)

    dialog.setVisible(true)
}

fun showUsersListWindow(val username : String) {
    val frame = JFrame("Users list")
    frame.setLayout(null)
    frame.setBounds(100, 100, 600, 650)
    frame.setResizable(false)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    val table = EditableTable(User.columns)
    frame.add(table.getScrollPane(10, 10, 580, 560))

    val db = AuthDb("database")
    db.load(true)
    table.data = db.users.values().map{it.asColumns()}

    val saveButton = JButton("Save")
    saveButton.setBounds(10, 590, 100, 30)
    frame.add(saveButton)

    val closeButton = JButton("Menu")
    closeButton.setBounds(150, 590, 100, 30)
    frame.add(closeButton)

    val addButton = JButton("Add")
    addButton.setBounds(350, 590, 100, 30)
    frame.add(addButton)

    val remButton = JButton("Remove")
    remButton.setBounds(490, 590, 100, 30)
    frame.add(remButton)

    val clickHandler = object : MouseAdapter() {
        public override fun mouseClicked(e : MouseEvent?) {
            when (e?.getSource()) {
                saveButton -> {
                    db.users.clear()
                    for (user in table.getObjects{User.fromColumns(this)}) {
                        db.users.put(user.login, user)
                    }
                    db.save(true)
                }

                closeButton -> {
                    frame.setVisible(false)
                    frame.dispose()
                    showVariantsWindow(null, username)
                }

                addButton -> {
                    table.data.add(User("NAME", "PASSWORD", Role.USER, System.currentTimeMillis()).asColumns())
                    table.data = table.data
                }

                remButton -> {
                    table.removeSelected()
                }

                else -> println("unknown click source")
            }
        }
    }

    saveButton.addMouseListener(clickHandler)
    closeButton.addMouseListener(clickHandler)
    addButton.addMouseListener(clickHandler)
    remButton.addMouseListener(clickHandler)

    frame.setVisible(true)
}

fun showInfoWindow(val username : String) {
    val user = auth.getUser(username)!!

    val frame = JFrame("Configuration")
    frame.setLayout(null)
    frame.setBounds(100, 100, 600, 650)
    frame.setResizable(false)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    val table = EditableTable(if (user.role == Role.USER) Info.columns else Info.editableColumns )
    frame.add(table.getScrollPane(10, 10, 580, 560))

    val db = AuthDb("database")
    db.load(true)
    table.data = db.infos.map{it.asColumns()}

    val saveButton = JButton("Save")
    saveButton.setBounds(10, 590, 100, 30)
    if (user.role == Role.USER) {
        saveButton.setEnabled(false)
    }
    frame.add(saveButton)

    val closeButton = JButton("Menu")
    closeButton.setBounds(150, 590, 100, 30)
    frame.add(closeButton)

    val addButton = JButton("Add")
    addButton.setBounds(350, 590, 100, 30)
    if (user.role == Role.USER) {
        addButton.setEnabled(false)
    }
    frame.add(addButton)

    val remButton = JButton("Remove")
    remButton.setBounds(490, 590, 100, 30)
    if (user.role == Role.USER) {
        remButton.setEnabled(false)
    }
    frame.add(remButton)

    val clickHandler = object : MouseAdapter() {
        public override fun mouseClicked(e : MouseEvent?) {
            when (e?.getSource()) {
                saveButton -> {
                    db.infos.clear()
                    for (entry in table.getObjects{this}) {
                        db.infos.add(Info.fromColumns(entry))
                    }
                    db.save(true)
                }

                closeButton -> {
                    frame.setVisible(false)
                    frame.dispose()
                    showVariantsWindow(null, username)
                }

                addButton -> {
                    if(user.role != Role.USER) {
                        table.data.add(Info("NAME", "MIDDLE", "LAST", "PHONE", "ADDRESS").asColumns())
                        table.data = table.data
                    }
                }

                remButton -> {
                    if(user.role != Role.USER) {
                        table.removeSelected()
                    }
                }

                else -> println("unknown click source")
            }
        }
    }

    saveButton.addMouseListener(clickHandler)
    closeButton.addMouseListener(clickHandler)
    addButton.addMouseListener(clickHandler)
    remButton.addMouseListener(clickHandler)

    frame.setVisible(true)
}
