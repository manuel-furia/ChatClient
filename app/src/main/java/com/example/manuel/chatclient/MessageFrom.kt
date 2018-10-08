/*
Author: Manuel Furia
Represents a message from the server (text, ping or information)
*/
package com.example.manuel.chatclient

import android.os.Parcel
import android.os.Parcelable

sealed class MessageFrom() {

    class TextMessageFromServer(val host: String, val port: Int, val user: String, val room: String, val timestamp: Long, val text: String, val serviceMessage: Boolean = false) : MessageFrom()

    //Used to check if the connection is still alive
    class PingFromServer() : MessageFrom()

    //For example user list and room list
    class ParsableInfoFromServer(val host: String, val port: Int, val text: String): MessageFrom()
}