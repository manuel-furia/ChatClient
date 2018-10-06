package com.example.manuel.chatclient

import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_server.*
import org.w3c.dom.Text

import com.example.manuel.chatclient.Utils.futureUITask

class ServerFragment: Fragment(){

    var serversAdapter: ServerElemAdapter? = null
    var connectionHandler: ConnectionHandler? = null
    var running = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_server, container, false);
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        serversAdapter = ServerElemAdapter(this.context)
        val recyclerServers: RecyclerView? = view?.findViewById(R.id.recyclerServers)
        val buttonAddServer: ImageButton? = view?.findViewById(R.id.buttonAddServer)
        val buttonSetUsername: ImageButton? = view?.findViewById(R.id.buttonSetUsername)
        val editServerName: EditText? = view?.findViewById(R.id.editServerName)
        val editServerPort: EditText? = view?.findViewById(R.id.editPort)
        val editUsername: EditText? = view?.findViewById(R.id.editUserName)

        connectionHandler?.getConnectedServers()

        buttonAddServer?.setOnClickListener {
            val host = editServerName?.text?.toString()
            val port = editServerPort?.text?.toString()?.toIntOrNull()

            if (host != null && port != null && host != "" && port != 0){
                MainActivityState.addServer(ServerInfo(host, port, false))
            }

            updateRecyclerView(MainActivityState.servers)
        }

        buttonSetUsername?.setOnClickListener {

        }

        recyclerServers?.layoutManager = LinearLayoutManager(this.context)
        recyclerServers?.adapter = serversAdapter

        running = true

        Future {
            while (running){
                MainActivityState.updateConnectionStatus()
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