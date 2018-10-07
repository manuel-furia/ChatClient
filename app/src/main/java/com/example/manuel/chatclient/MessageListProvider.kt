package com.example.manuel.chatclient

interface MessageListProvider {
    fun getMessages(host: String, port: Int): List<MessageFrom>?
}