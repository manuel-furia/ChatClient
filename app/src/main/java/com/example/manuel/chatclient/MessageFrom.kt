package com.example.manuel.chatclient

import android.os.Parcel
import android.os.Parcelable

sealed class MessageFrom() {

    class TextMessageFromServer(val host: String, val port: Int, val user: String, val room: String, val timestamp: Long, val text: String, val serviceMessage: Boolean = false) : MessageFrom()
    class PingFromServer() : MessageFrom()
    class ParsableInfoFromServer(val host: String, val port: Int, val text: String): MessageFrom()
}