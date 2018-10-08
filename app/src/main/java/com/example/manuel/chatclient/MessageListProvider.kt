/*
Author: Manuel Furia
Represents an instance providing recorded messages to a user class
*/

package com.example.manuel.chatclient

interface MessageListProvider {
    fun getMessages(host: String, port: Int): List<MessageFrom>?
}