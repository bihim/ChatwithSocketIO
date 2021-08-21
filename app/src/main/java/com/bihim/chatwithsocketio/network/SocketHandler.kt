package com.bihim.chatwithsocketio.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {

    lateinit var socketConf: Socket

    @Synchronized
    fun setSocket() {
        try {
            socketConf = IO.socket("http://192.168.0.106:3000")
        } catch (e: URISyntaxException) {
            Log.d("SOCKETETET", "setSocket: $e")
        }
    }

    @Synchronized
    fun getSocket(): Socket {
        return socketConf
    }

    @Synchronized
    fun establishConnection() {
        socketConf.connect()
    }

    @Synchronized
    fun closeConnection() {
        socketConf.disconnect()
    }
}