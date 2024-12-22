package com.example.imrsaaes.api

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class SocketApi {

    companion object {
        private const val SOCKET_URL = "http://192.168.122.38:3000" // Ganti sesuai dengan alamat server kamu
        private var socket: Socket? = null

        fun getSocketInstance(): Socket? {
            if (socket == null) {
                try {
                    socket = IO.socket(SOCKET_URL)
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
            return socket
        }
    }
}
