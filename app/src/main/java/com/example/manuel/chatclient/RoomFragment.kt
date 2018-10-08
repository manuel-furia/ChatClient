/*
Author: Manuel Furia
Shows the rooms available on the server, and allow to create new ones
*/

package com.example.manuel.chatclient

import android.app.AlertDialog
import android.content.Intent
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
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
        //When the fragment becomes visible to the user...
        if (isVisibleToUser){
            val server = MainActivityState.selectedServer

            if (server == null) { //If there is no selected server clear the room list
                rooms.clear()
                updateRoomList()
                return
            }
            //Check if connected to a server
            val connected = MainActivityState.connectionHandler?.isConnected(server) ?: false
            if (connected) { //If we are connected, ask for the room list
                Future { fetchRoomsFromServer(server) }
            } else { //If we are not, start a task to connect and then get the room list
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
        //Create a room adapter that will call the following code when the room list changes by user actions
        roomsAdapter = RoomElemAdapter(this) {
            val server = MainActivityState.selectedServer
            fetchRoomsFromServer(server)
        }

        //Allow the user to add a room
        buttonAddRoom.setOnClickListener {
            val server = MainActivityState.selectedServer
            val newRoomName = editNewRoomName?.text?.toString()
            val username = MainActivityState.username
            if (server != null && newRoomName != null && username != null && newRoomName != "") {
                destination?.update(MessageTo(server.host, server.port, Constants.mainServerRoom, ":user $username"))
                destination?.update(MessageTo(server.host, server.port, Constants.mainServerRoom, ":room $newRoomName"))
                fetchRoomsFromServer(server)
            }
            if (username == null){
                AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage("Set your username first.")
                        .show()
            }
        }

    }

    override fun onStart() {
        super.onStart()

        recyclerRooms?.layoutManager = LinearLayoutManager(this.context)
        recyclerRooms?.adapter = roomsAdapter

        destination = null
        source = null

        updateRoomList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //Fetch the new room status when returning from a chat activity
        if (requestCode == Constants.activityResultLeftRoom){
            fetchRoomsFromServer(MainActivityState.selectedServer)
        }
    }

    override fun update(event: MessageFrom) {
        //If the message contains parsable infomation (it may be a room list)
        if (event is MessageFrom.ParsableInfoFromServer){

            val selectedServer = MainActivityState.selectedServer

            selectedServer ?: return

            val connected = MainActivityState.connectionHandler?.isConnected(selectedServer) ?: false

            //If the message is from the right server, try to parse it
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

    /**
     * Parse a room list asynchronously (the messages can arrive at different times)
     */
    private fun parseRoomInformation(text: String) {
        val data = text.trim()
        if (data == Constants.serverParsableRoomsPrefix){ //Start of room list
            receivingRoomInfo = true
            rooms.clear()
        } else if (Constants.serverParsableInfoPrefixesWoRooms.contains(data)){ //End of the list, other type of data arriving
            receivingRoomInfo = false
        } else if (receivingRoomInfo){ //Data relevant for the room list
            val room = text.trimStart('*')
            val isJoined = text.startsWith('*')
            rooms.add(RoomInfo(room, isJoined))
            Utils.futureUITask {
                updateRoomList()
            }
        }
    }


    private fun updateRoomList(){
        rooms.sortBy { it.name.toLowerCase() }
        roomsAdapter?.update(rooms)
        recyclerRooms?.adapter = roomsAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        source?.unregisterObserver(this)
        source = null
    }

    //Ask for the messages from a destination (observer), they will be fetched via a source (observable)
    private fun fetchRoomsFromServer(server: ServerInfo?){
        destination = MainActivityState.messageToObserver

        if (server != null) {
            Future {
                destination?.update(MessageTo(server.host, server.port, Constants.mainServerRoom, ":rooms"))
            }
            if (source != MainActivityState.messageFromObservable) { //The source has changed, or was not set
                source?.unregisterObserver(this)
                source = MainActivityState.messageFromObservable
                source?.registerObserver(this)
            }
        }
    }

}