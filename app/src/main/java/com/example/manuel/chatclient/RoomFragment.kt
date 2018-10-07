package com.example.manuel.chatclient

import android.app.AlertDialog
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_rooms.*

class RoomFragment: Fragment(), Observer<MessageFrom> {

    var roomsAdapter: RoomElemAdapter? = null
    var destination: Observer<MessageTo>? = null
    var source: Observable<MessageFrom>? = null
    var receivingRoomInfo: Boolean = false
    val rooms: MutableList<RoomInfo> = mutableListOf()

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser){
            val server = MainActivityState.selectedServer

            if (server == null) {
                rooms.clear()
                update()
                return
            }

            val connected = MainActivityState.connectionHandler?.isConnected(server) ?: false
            if (connected) {
                Future { fetchRoomsFromServer(server) }
            } else {
                MainActivityState.connectionHandler?.createConnection(server.host, server.port)
                Future {
                    var nowConnected = false
                    while (!nowConnected) {
                        nowConnected = MainActivityState.connectionHandler?.isConnected(server) ?: false
                        Thread.sleep(100)
                    }
                    fetchRoomsFromServer(server)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_rooms, container, false);
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        //recyclerRooms = view?.findViewById<RecyclerView>(R.id.recyclerRooms)
        roomsAdapter = RoomElemAdapter(this.context) {
            val server = MainActivityState.selectedServer
            fetchRoomsFromServer(server)
        }

    }

    override fun onStart() {
        super.onStart()

        recyclerRooms?.layoutManager = LinearLayoutManager(this.context)
        recyclerRooms?.adapter = roomsAdapter

        destination = null
        source = null

        update()
    }

    override fun update(event: MessageFrom) {
        if (event is MessageFrom.ParsableInfoFromServer){

            val selectedServer = MainActivityState.selectedServer

            selectedServer ?: return

            val connected = MainActivityState.connectionHandler?.isConnected(selectedServer) ?: false

            if (connected && selectedServer.host == event.host && selectedServer.port == event.port){
                event.text.split("\n").forEach {
                    if (it.startsWith(Constants.serviceParsableMessagePrefix + " "))
                        parseRoomInformation(it.drop(Constants.serviceParsableMessagePrefix.length + 1))
                    else if (it.startsWith(Constants.serviceParsableMessagePrefix))
                        parseRoomInformation(it.drop(Constants.serviceParsableMessagePrefix.length))
                    else
                        parseRoomInformation(it)
                }
            }
        }
    }


    private fun parseRoomInformation(text: String) {
        val data = text.trim()
        if (data == "Rooms:"){
            receivingRoomInfo = true
            rooms.clear()
        } else if (data.startsWith(":")){
            receivingRoomInfo = false
        } else if (receivingRoomInfo){
            val room = text.trimStart('*')
            val isJoined = text.startsWith('*')
            rooms.add(RoomInfo(room, isJoined))
            Utils.futureUITask {
                update()
            }
        }
    }


    private fun update(){
        rooms.sortBy { it.name }
        roomsAdapter?.update(rooms)
        recyclerRooms?.adapter = roomsAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        source?.unregisterObserver(this)
        source = null
    }

    private fun fetchRoomsFromServer(server: ServerInfo?){
        destination = MainActivityState.messageToObserver

        if (server != null) {
            Future {
                destination?.update(MessageTo(server.host, server.port, Constants.mainServerRoom, ":rooms"))
            }
            if (source != MainActivityState.messageFromObservable) {
                source?.unregisterObserver(this)
                source = MainActivityState.messageFromObservable
                source?.registerObserver(this)
            }
        }
    }

}