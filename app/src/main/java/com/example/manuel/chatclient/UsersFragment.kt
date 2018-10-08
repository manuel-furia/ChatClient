/*
Author: Manuel Furia
Fragment that shows the user list. Similar to RoomFragment.
*/

package com.example.manuel.chatclient

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_users.*

class UsersFragment: Fragment(), Observer<MessageFrom> {

    val users: MutableList<UserInfo> = mutableListOf()
    var usersAdapter: UserElemAdapter? = null
    var destination: Observer<MessageTo>? = null
    var source: Observable<MessageFrom>? = null
    var receivingUserInfo: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_users, container, false);
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser){
            val server = MainActivityState.selectedServer

            if (server == null) {
                users.clear()
                updateUserList()
                return
            }

            val connected = MainActivityState.connectionHandler?.isConnected(server) ?: false
            if (connected) {
                Future { fetchUsersFromServer(server) }
            }
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        //recyclerRooms = view?.findViewById<RecyclerView>(R.id.recyclerRooms)
        usersAdapter = UserElemAdapter(this) {
            val host = MainActivityState.selectedServer?.host
            val room = MainActivityState.selectedRoom?.name
            (activity as ChatActivity).goToChatFragment(host, room)
        }


    }

    override fun onStart() {
        super.onStart()

        recyclerUsers?.layoutManager = LinearLayoutManager(this.context)
        recyclerUsers?.adapter = usersAdapter

        destination = null
        source = null

        updateUserList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.activityResultLeftRoom){
            fetchUsersFromServer(MainActivityState.selectedServer)
        }
    }

    override fun update(event: MessageFrom) {
        if (event is MessageFrom.ParsableInfoFromServer){

            val selectedServer = MainActivityState.selectedServer

            selectedServer ?: return

            val connected = MainActivityState.connectionHandler?.isConnected(selectedServer) ?: false

            if (connected && selectedServer.host == event.host && selectedServer.port == event.port){
                event.text.split("\n").forEach {
                    if (it.startsWith(Constants.serviceParsableMessagePrefix + " "))
                        parseUserInformation(it.drop(Constants.serviceParsableMessagePrefix.length + 1))
                    else if (it.startsWith(Constants.serviceParsableMessagePrefix))
                        parseUserInformation(it.drop(Constants.serviceParsableMessagePrefix.length))
                    else
                        parseUserInformation(it)
                }
            }
        }
    }

    //Parse the user list arriving message by message
    private fun parseUserInformation(text: String) {
        val data = text.trim()
        if (data == Constants.serverParsableUsersPrefix){ //Start of the user list
            receivingUserInfo = true
            users.clear()
        } else if (Constants.serverParsableInfoPrefixesWoUsers.contains(data)){ //End of the user list, some different data is arriving
            receivingUserInfo = false
        } else if (receivingUserInfo){ //Element of the user list, record it
            val user = text
            users.add(UserInfo(user, false))
            Utils.futureUITask {
                updateUserList()
            }
        }
    }


    private fun updateUserList(){
        users.sortBy { it.username.toLowerCase() }
        usersAdapter?.update(users)
        recyclerUsers?.adapter = usersAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        source?.unregisterObserver(this)
        source = null
    }

    //Ask the server for the user list
    private fun fetchUsersFromServer(server: ServerInfo?){
        destination = MainActivityState.messageToObserver

        if (server != null) {
            val room = MainActivityState.selectedRoom?.name ?: return
            Future {
                destination?.update(MessageTo(server.host, server.port, room, ":users"))
            }
            if (source != MainActivityState.messageFromObservable) {
                source?.unregisterObserver(this)
                source = MainActivityState.messageFromObservable
                source?.registerObserver(this)
            }
        }
    }
}