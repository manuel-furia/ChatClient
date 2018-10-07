package com.example.manuel.chatclient

import android.content.Context
import android.content.res.ColorStateList
import android.provider.ContactsContract
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_chat.*
import java.text.SimpleDateFormat
import java.util.*

class ServerElemAdapter(private val context: Context) : RecyclerView.Adapter<ServerElemAdapter.ViewHolder>(){

    private var servers: MutableList<ServerInfo> = mutableListOf()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    enum class ElementType{
        CONNECTED,
        DISCONNECTED
    }

    override fun getItemViewType(position: Int): Int {
        val connected = MainActivityState.connectionHandler?.isConnected(servers[position])
        if (connected == true){
            return ElementType.CONNECTED.ordinal
        } else
            return ElementType.DISCONNECTED.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (viewType == ElementType.CONNECTED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.server_element, parent,false) as LinearLayout)
        else (viewType == ElementType.DISCONNECTED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.server_element, parent,false) as LinearLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val view = holder?.view ?: return

        val textServerAddr = view.findViewById<TextView>(R.id.textServerElemName)
        val buttonServerElemConnect = view.findViewById<ImageButton>(R.id.buttonServerElemConnect)
        val buttonServerElemRemove = view.findViewById<ImageButton>(R.id.buttonServerElemRemove)
        val imageServerElemStatus = view.findViewById<ImageView>(R.id.imageServerElemStatus)
        val clickableAreaServerElemStatus = view.findViewById<LinearLayout>(R.id.clickableAreaServerElemStatus)

        val server = servers[position]
        val serverName = "${server.host}:${server.port}"

        val selectServer = View.OnClickListener {
            MainActivityState.selectedServer = server
            MainActivityState.messageToObserver?.update(MessageTo(
                    server.host,
                    server.port,
                    Constants.mainServerRoom,
                    ":user ${MainActivityState.username}"))

        }

        textServerAddr.text = serverName

        if (MainActivityState.connectionHandler?.isConnected(server) == true){
            val color = ContextCompat.getColor(this.context, R.color.colorConnected)
            imageServerElemStatus?.imageTintList = ColorStateList.valueOf(color)
            buttonServerElemConnect.setImageResource(R.drawable.ic_clear_black_24dp)
            buttonServerElemConnect.setOnClickListener {
                MainActivityState.connectionHandler?.dropConnection(server.host, server.port)
            }
        } else {
            val color = ContextCompat.getColor(this.context, R.color.colorDisconnected)
            imageServerElemStatus?.imageTintList = ColorStateList.valueOf(color)
            buttonServerElemConnect.setImageResource(R.drawable.ic_play_circle_outline_black_24dp)
            buttonServerElemConnect.setOnClickListener {
                MainActivityState.connectionHandler?.createConnection(server.host, server.port)
                if (MainActivityState.selectedServer == null){
                    MainActivityState.selectedServer = server
                }
            }
            //if (server == MainActivityState.selectedServer)
            //MainActivityState.selectedServer = null
        }

        buttonServerElemRemove.setOnClickListener {
            MainActivityState.messageToObserver?.update(MessageTo(server.host, server.port, Constants.mainServerRoom, ":quit"))
            MainActivityState.connectionHandler?.dropConnection(server.host, server.port)
            MainActivityState.removeServer(server)
        }

        textServerAddr?.setOnClickListener (selectServer)

        clickableAreaServerElemStatus?.setOnClickListener (selectServer)

        if (server == MainActivityState.selectedServer){
            //val color = ContextCompat.getColor(this.context, R.color.colorConnected)
            //imageServerElemStatus?.imageTintList = ColorStateList.valueOf(color)
            imageServerElemStatus.setImageResource(R.drawable.ic_check_black_24dp)
        } else {
            imageServerElemStatus.setImageResource(R.drawable.ic_lens_black_12dp)
        }

    }

    override fun getItemCount(): Int = servers.size

    fun update(servers: List<ServerInfo>){
        this.servers.clear()
        this.servers.addAll(servers)
        this.notifyDataSetChanged()
    }

    fun update(){
        this.notifyDataSetChanged()
    }

}