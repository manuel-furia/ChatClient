package com.example.manuel.chatclient

object Constants {

    const val serverConsoleName = "server"
    const val serviceMessagePrefix = ":-"
    const val serviceParsableMessagePrefix = ":="
    const val roomPrefix = "@"
    const val timestampPrefix = "$"
    const val usernamePrefix = "+"
    const val messageTextPrefix = " "
    const val testIP = "192.168.43.64"
    //const val testIP = "10.0.2.2"
    const val testPort = 61673
    const val pingString = ":PING:"
    const val connectionTimeoutAfterMilliseconds = 3000
    const val mainServerRoom = "hall"
    const val activityResultLeftRoom = 13

    val serverParsableRoomsPrefix = "Rooms:"
    val serverParsableUsersPrefix = "Users:"
    val serverParsableQueryPrefix = "Query:"
    val serverParsableMessagesPrefix = "Messages:"
    val serverParsableInfoPrefixes = listOf(serverParsableRoomsPrefix, serverParsableUsersPrefix, serverParsableQueryPrefix, serverParsableMessagesPrefix)
    val serverParsableInfoPrefixesWoRooms = listOf(serverParsableUsersPrefix, serverParsableQueryPrefix, serverParsableMessagesPrefix)
    val serverParsableInfoPrefixesWoUsers = listOf(serverParsableRoomsPrefix, serverParsableQueryPrefix, serverParsableMessagesPrefix)
    val serverParsableInfoPrefixesWoQuery = listOf(serverParsableRoomsPrefix, serverParsableUsersPrefix, serverParsableMessagesPrefix)
    val serverParsableInfoPrefixesWoMessages = listOf(serverParsableRoomsPrefix, serverParsableUsersPrefix, serverParsableQueryPrefix)
}