/*
Author: Manuel Furia
Represents a message from the client to the server (identified by host and port)
*/

package com.example.manuel.chatclient

class MessageTo(val host: String, val port: Int, val room: String, val text: String)