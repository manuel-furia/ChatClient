package com.example.manuel.chatclient

import android.content.Context
import android.provider.ContactsContract
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class ServerElemAdapter(private val context: Context) : RecyclerView.Adapter<ServerElemAdapter.ViewHolder>(){

    var servers: MutableList<ServerInfo> = mutableListOf()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    enum class ElementType{
        CONNECTED,
        DISCONNECTED
    }

    override fun getItemViewType(position: Int): Int {
        if (servers[position].connected){
            return ElementType.CONNECTED.ordinal
        } else
            return ElementType.DISCONNECTED.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (viewType == ElementType.CONNECTED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.fragment_server, parent,false) as LinearLayout)
        else (viewType == ElementType.DISCONNECTED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.fragment_server, parent,false) as LinearLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val view = holder?.view ?: return

        val textServerAddr = view.findViewById<TextView>(R.id.textServerElemName)
        val buttonServerElemConnect = view.findViewById<ImageButton>(R.id.buttonServerElemConnect)
        val imageServerElemStatus = view.findViewById<ImageView>(R.id.imageServerElemStatus)
    }

    override fun getItemCount(): Int = servers.size

    fun update(servers: List<ServerInfo>){
        this.servers.clear()
        this.servers.addAll(servers)
        this.notifyDataSetChanged()
    }

}