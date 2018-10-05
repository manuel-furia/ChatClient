package com.example.manuel.chatclient

interface ConnectionHandler {

    fun createConnection(address: String, port: Int): Boolean
    fun dropConnection(address: String, port: Int)
    fun getConnectedServers(): Set<ServerInfo>

}