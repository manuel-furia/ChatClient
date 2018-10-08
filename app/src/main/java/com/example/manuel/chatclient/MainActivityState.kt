package com.example.manuel.chatclient

object MainActivityState {

    var storableStateModifiedCallback: ((List<ServerInfo>, ServerInfo?, String?) -> Unit)? = null

    var username: String? = null
    set(value){
        field = value
        storableStateModifiedCallback?.invoke(serverList, selectedServer, field)
    }

    private val serverList: MutableList<ServerInfo> = mutableListOf()
    private var serverHandler: ServerHandler? = null

    var selectedServerIndex: Int? = null
        set(value) = if (serverList.indices.contains(value)) field = value else field = null

    var selectedRoom: RoomInfo? = null

    val servers: List<ServerInfo>
        get() = serverList


    var selectedServer: ServerInfo?
    get() = serverList.getOrNull(selectedServerIndex ?: -1)
    @Synchronized set(server: ServerInfo?){
        val index = serverList.indexOfFirst { it.host == server?.host && it.port == server.port }
        selectedServerIndex = if (index >= 0) index else null
        storableStateModifiedCallback?.invoke(serverList, selectedServer, username)
    }

    @Synchronized
    fun addServer(server: ServerInfo){
        if (!serverList.contains(server)) {
            serverList.add(server)
            storableStateModifiedCallback?.invoke(serverList, selectedServer, username)
        }
    }

    @Synchronized
    fun removeServer(server: ServerInfo){

        if (serverList.contains(server)){
            if (selectedServerIndex == serverList.indexOf(server)){
                selectedServerIndex = null
            }
            serverList.remove(server)
            storableStateModifiedCallback?.invoke(serverList, selectedServer, username)
        }
    }

    @Synchronized
    fun removeServerAt(index: Int){
        if (serverList.indices.contains(index)) {
            if (index == selectedServerIndex){
                selectedServerIndex = null
            }
            serverList.removeAt(index)
            storableStateModifiedCallback?.invoke(serverList, selectedServer, username)
        }
    }

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