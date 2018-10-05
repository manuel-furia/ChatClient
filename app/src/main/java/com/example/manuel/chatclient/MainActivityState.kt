package com.example.manuel.chatclient

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

object MainActivityState {

    private val serverList: MutableList<ServerInfo> = mutableListOf()
    private val roomList: MutableList<RoomInfo> = mutableListOf()
    private val userList: MutableList<UserInfo> = mutableListOf()
    private var serverHandler: ServerHandler? = null

    var selectedServerIndex: Int? = 0
        set(value) = if (serverList.indices.contains(value)) field = value else field = null

    var selectedRoomIndex: Int? = 0
        set(value) = if (roomList.indices.contains(value)) field = value else field = null

    val servers: List<ServerInfo>
        get() = serverList

    val rooms: List<RoomInfo>
        get() = roomList

    val users: List<UserInfo>
        get() = userList

    var selectedServer: ServerInfo?
    get() = serverList.getOrNull(selectedServerIndex ?: -1)
    set(server: ServerInfo?){
        val index = serverList.indexOfFirst { it == server }
        selectedServerIndex = if (index >= 0) index else null
    }

    var selectedRoom: RoomInfo?
        get() = roomList.getOrNull(selectedRoomIndex ?: -1)
        set(room: RoomInfo?){
            val index = roomList.indexOfFirst { it == room }
            selectedRoomIndex = if (index >= 0) index else null
        }

    fun addServer(server: ServerInfo){
        if (!serverList.contains(server))
            serverList.add(server)
    }

    fun removeServer(server: ServerInfo){
        if (serverList.contains(server)) serverList.remove(server)
    }

    fun removeServerAt(index: Int){
        if (serverList.indices.contains(index)) serverList.removeAt(index)
    }

    fun dropServerHandler(){
        serverHandler = null
    }

    fun setServerHandler(service: ServerHandler){
        serverHandler = service
    }

    val boundToServerHandler: Boolean = serverHandler != null

    val messageFromObservable : Observable<MessageFrom>?
        get() = serverHandler

    val messageToObserver : Observer<MessageTo>?
        get() = serverHandler

    val connectionHandler : ConnectionHandler?
        get() = serverHandler
}