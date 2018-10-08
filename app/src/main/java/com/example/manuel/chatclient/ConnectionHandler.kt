/*
Author: Manuel Furia
Represents an instance that manages the connections to servers
*/

package com.example.manuel.chatclient

interface ConnectionHandler {

    fun createConnection(address: String, port: Int): Boolean
    fun dropConnection(address: String, port: Int)
    fun getConnectedServers(): Set<ServerInfo>
    fun isConnected(server: ServerInfo): Boolean
}