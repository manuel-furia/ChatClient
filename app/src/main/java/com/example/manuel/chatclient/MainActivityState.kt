package com.example.manuel.chatclient

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

object MainActivityState {

    var username: String? = null

    private val serverList: MutableList<ServerInfo> = mutableListOf()
    //private val roomList: MutableList<RoomInfo> = mutableListOf()
    //private val userList: MutableList<UserInfo> = mutableListOf()
    private var serverHandler: ServerHandler? = null

    var selectedServerIndex: Int? = null
        set(value) = if (serverList.indices.contains(value)) field = value else field = null

    var selectedRoom: RoomInfo? = null
        //set(value) = if (roomList.indices.contains(value)) field = value else field = null

    val servers: List<ServerInfo>
        get() = serverList

    /*val rooms: List<RoomInfo>
        get() = roomList*/

    /*val users: List<UserInfo>
        get() = userList*/


    var selectedServer: ServerInfo?
    get() = serverList.getOrNull(selectedServerIndex ?: -1)
    @Synchronized set(server: ServerInfo?){
        val index = serverList.indexOfFirst { it.host == server?.host && it.port == server.port }
        selectedServerIndex = if (index >= 0) index else null
    }

   /* var selectedRoom: RoomInfo?
        get() = roomList.getOrNull(selectedRoomIndex ?: -1)
    @Synchronized set(room: RoomInfo?){
            val index = roomList.indexOfFirst { it == room }
            selectedRoomIndex = if (index >= 0) index else null
        }*/

    /*
    @Synchronized
    fun updateConnectionStatus(){
        for (i in serverList.indices) {
            val server = serverList[i]
            serverList[i] = ServerInfo(server.host, server.port, serverHandler?.isConnected(server) ?: false)
        }
    }*/

    @Synchronized
    fun addServer(server: ServerInfo){
        if (!serverList.contains(server))
            serverList.add(server)
    }

    @Synchronized
    fun removeServer(server: ServerInfo){

        if (serverList.contains(server)){
            if (selectedServerIndex == serverList.indexOf(server)){
                selectedServerIndex = null
            }
            serverList.remove(server)
        }
    }

    @Synchronized
    fun removeServerAt(index: Int){
        if (serverList.indices.contains(index)) {
            if (index == selectedServerIndex){
                selectedServerIndex = null
            }
            serverList.removeAt(index)
        }
    }

    /*@Synchronized
    fun addRoom(room: RoomInfo){
        if (!roomList.contains(room))
            roomList.add(room)
    }

    @Synchronized
    fun removeRoom(room: RoomInfo){

        if (roomList.contains(room)){
            if (selectedRoomIndex == roomList.indexOf(room)){
                selectedRoomIndex = null
            }
            roomList.remove(room)
        }
    }

    @Synchronized
    fun removeRoomAt(index: Int){
        if (roomList.indices.contains(index)) {
            if (index == selectedRoomIndex){
                selectedRoomIndex = null
            }
            roomList.removeAt(index)
        }
    }*/


    @Synchronized
    fun dropServerHandler(){
        serverHandler = null
    }

    @Synchronized
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

    val messageListProvider : MessageListProvider?
        get() = serverHandler
}