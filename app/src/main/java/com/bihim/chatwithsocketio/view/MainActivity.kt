package com.bihim.chatwithsocketio.view

import android.app.Dialog
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.bihim.chatwithsocketio.R
import com.bihim.chatwithsocketio.adapter.ChatRecyclerViewAdapter
import com.bihim.chatwithsocketio.database.ChatDataModel
import com.bihim.chatwithsocketio.global.GlobalValues
import com.bihim.chatwithsocketio.model.ChatViewModel
import com.bihim.chatwithsocketio.network.SocketHandler
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.realm.Realm
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    lateinit var mSocket: Socket
    val list = ArrayList<ChatViewModel>()
    val adapter = ChatRecyclerViewAdapter(this, list)
    var realm: Realm? = null
    val chatDataModel = ChatDataModel()
    var isConnected = false
    var isToastShowed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        socketInit()
        checkingConnection()
        val sharedPref =
            getSharedPreferences(GlobalValues().sharedPrefName, MODE_PRIVATE)
        if (!sharedPref.contains(GlobalValues().number)) {
            showDialog()
        }
        typingIndicator(mSocket)
        readAllData()
        sendingMessage()
        settingToolbarData()
        gettingAndStoringMessages()
        setRecyclerview()
    }

    private fun checkingConnection() {
        mSocket.on(Socket.EVENT_DISCONNECT) {
            Log.d("SOCKETETET", "onCreate: Disconnected")
            this.runOnUiThread {
                Toast.makeText(this, "Server disconnected. Please reconnect.", Toast.LENGTH_SHORT)
                    .show()
            }
            isConnected = false
            isToastShowed = false
        }

        mSocket.on(Socket.EVENT_CONNECT) {
            this.runOnUiThread {
                Toast.makeText(this, "Connection Established", Toast.LENGTH_SHORT).show()
            }
            isConnected = true
            isToastShowed = true
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d("SOCKETETET", "onCreate: Connection Error")
            isConnected = false
            this.runOnUiThread {
                if (!isConnected && !isToastShowed) {
                    Toast.makeText(this, "Server disconnected. Please reconnect.", Toast.LENGTH_SHORT)
                        .show()
                    isToastShowed = true
                }
            }
        }
    }

    private fun gettingAndStoringMessages() {
        mSocket.on("message") { args ->
            if (args[0] != null) {
                val message = args[0] as String
                runOnUiThread {
                    Log.d("TESTINGTE", "onCreate: $message")
                    val splittedText = message.split("~&")
                    val sharedPref =
                        getSharedPreferences(GlobalValues().sharedPrefName, MODE_PRIVATE)
                    val number = sharedPref.getString(GlobalValues().number, "")
                    val username = sharedPref.getString(GlobalValues().username, "")
                    val numberFromServer = splittedText[0]
                    val messages = splittedText[2]
                    Log.d("NUMBERSASA", "onCreate: server $numberFromServer and number $number")
                    if (number == numberFromServer) {
                        list.add(ChatViewModel(GlobalValues().SENDER, username!!, messages))
                        addDataToDb(GlobalValues().SENDER, messages, username)

                    } else {
                        list.add(ChatViewModel(GlobalValues().RECEIVER, username!!, messages))
                        addDataToDb(GlobalValues().RECEIVER, messages, username)
                    }
                    recycler_chat.smoothScrollToPosition(adapter.itemCount)
                }
            }
        }
    }

    private fun sendingMessage() {
        isToastShowed = false
        outlinedTextField.setEndIconOnClickListener {
            Log.d("SOCKETETET", "onCreate: Connected")
            val message = chat_editText.text.toString()
            val sharedPref = getSharedPreferences(GlobalValues().sharedPrefName, MODE_PRIVATE)
            val number = sharedPref.getString(GlobalValues().number, "")
            val username = sharedPref.getString(GlobalValues().username, "")
            if (message.isNotEmpty()) {
                mSocket.emit("message", "$number~&$username~&$message")
                mSocket.emit("typing", false)
                chat_editText.text?.clear()
            }
        }

    }

    private fun settingToolbarData() {
        val toolbarData: String = materialToolbar.title.toString()
        if (toolbarData == "Nobody joined yet") {
            mSocket.on("message") { args ->
                if (args[0] != null) {
                    val message = args[0] as String
                    runOnUiThread {
                        Log.d("TESTINGTE", "onCreate: $message")
                        val splittedText = message.split("~&")
                        val sharedPref =
                            getSharedPreferences(GlobalValues().sharedPrefName, MODE_PRIVATE)
                        val number = sharedPref.getString(GlobalValues().number, "")
                        val numberFromServer = splittedText[0]
                        Log.d("NUMBERSASA", "onCreate: server $numberFromServer and number $number")
                        if (number != numberFromServer) {
                            materialToolbar.title = "${splittedText[1]} is joined"
                        }
                    }
                }
            }
        }
    }

    private fun readAllData() {
        realm = Realm.getDefaultInstance()
        if (realm!!.schema.contains("ChatDataModel")) {
            val chatDataModels: List<ChatDataModel> =
                realm!!.where(ChatDataModel::class.java).findAll()
            if (chatDataModels.isNotEmpty()) {
                for (values in chatDataModels.indices) {
                    list.add(
                        ChatViewModel(
                            chatDataModels[values].viewType,
                            chatDataModels[values].userName!!,
                            chatDataModels[values].message!!
                        )
                    )
                }
            }
        }
    }

    private fun addDataToDb(viewType: Int, messages: String, username: String) {
        val getId = realm!!.where(ChatDataModel::class.java).max("id")
        val id = if (getId == null) {
            1
        } else {
            getId.toInt() + 1
        }
        try {
            chatDataModel.id = id
            chatDataModel.message = messages
            chatDataModel.userName = username
            chatDataModel.viewType = viewType
            realm!!.executeTransaction { realm ->
                realm.copyToRealm(chatDataModel)
            }
            Log.d("DATAGET", "Data Inserted !!!")

        } catch (e: Exception) {
            Log.d("DATAGET", "error: $e")
        }
    }

    private fun setRecyclerview() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recycler_chat.layoutManager = layoutManager
        recycler_chat.adapter = adapter
    }

    private fun typingIndicator(mSocket: Socket) {
        chat_editText.doOnTextChanged { text, start, before, count ->
            if (count == 0) {
                mSocket.emit("typing", false)
            } else {
                mSocket.emit("typing", true)
            }
            Log.d("Typing", "onCreate: $count and $text")
        }

        mSocket.on("typing") { args ->
            if (args[0] != null) {
                val typing = args[0] as Boolean
                runOnUiThread {
                    if (typing) {
                        lottie_typing.visibility = View.VISIBLE
                    } else {
                        lottie_typing.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun socketInit() {
        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        mSocket = SocketHandler.getSocket()
    }

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_name)
        val yesButton = dialog.findViewById(R.id.dialog_yes) as MaterialButton
        val noButton = dialog.findViewById(R.id.dialog_no) as TextView
        val username = dialog.findViewById(R.id.dialog_name) as TextInputEditText
        val number = dialog.findViewById(R.id.dialog_number) as TextInputEditText
        yesButton.setOnClickListener {
            val sharedPreferences: SharedPreferences =
                getSharedPreferences(GlobalValues().sharedPrefName, MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(GlobalValues().username, username.text.toString())
            editor.putString(GlobalValues().number, number.text.toString())
            editor.apply()
            dialog.dismiss()

            mSocket.on("whoJoined") { args ->
                if (args[0] != null) {
                    val whoJoined = args[0] as String
                    runOnUiThread {
                        if (whoJoined != username.text.toString()) {
                            materialToolbar.title = "$whoJoined is joined"
                        } else {
                            materialToolbar.title = "Nobody joined yet"
                        }
                    }
                }
            }

        }
        noButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()

    }

    override fun onDestroy() {
        super.onDestroy()
        SocketHandler.closeConnection()
    }
}