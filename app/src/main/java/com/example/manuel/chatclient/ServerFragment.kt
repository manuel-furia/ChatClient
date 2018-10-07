package com.example.manuel.chatclient

import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import kotlinx.android.synthetic.main.fragment_server.*

import com.example.manuel.chatclient.Utils.futureUITask

class ServerFragment: Fragment(){

    var serversAdapter: ServerElemAdapter? = null
    var running = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_server, container, false);
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        serversAdapter = ServerElemAdapter(this.context)
        val recyclerServers: RecyclerView? = view?.findViewById(R.id.recyclerServers)
        val buttonAddServer: ImageButton? = view?.findViewById(R.id.buttonAddServer)
        val buttonSetUsername: ImageButton? = view?.findViewById(R.id.buttonSetUsername)
        val editServerName: EditText? = view?.findViewById(R.id.editNewRoomName)
        val editServerPort: EditText? = view?.findViewById(R.id.editPort)
        val editUsername: EditText? = view?.findViewById(R.id.editUserName)

        buttonAddServer?.setOnClickListener {
            val host = editServerName?.text?.toString()
            val port = editServerPort?.text?.toString()?.toIntOrNull()

            if (host != null && port != null && host != "" && port != 0){
                MainActivityState.addServer(ServerInfo(host, port))
                if (MainActivityState.servers.size == 1){
                    MainActivityState.selectedServer = MainActivityState.servers[0]
                }
            }

            updateRecyclerView(MainActivityState.servers)
        }

        buttonSetUsername?.setOnClickListener {
            val userName = editUserName.text.toString()
            if (userName != ""){
                MainActivityState.username = userName
                MainActivityState.connectionHandler?.getConnectedServers()?.forEach {
                    MainActivityState.messageToObserver?.update(MessageTo(it.host, it.port, Constants.mainServerRoom,":user $userName"))
                }
            }

        }

        recyclerServers?.layoutManager = LinearLayoutManager(this.context)
        recyclerServers?.adapter = serversAdapter

        running = true

        Future {
            while (running){
                futureUITask { serversAdapter?.update(MainActivityState.servers) }
                Thread.sleep(1000)
            }
        }
    }

    private fun updateRecyclerView(servers: List<ServerInfo>){
        serversAdapter?.update(servers)
        recyclerServers?.adapter = serversAdapter
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

}