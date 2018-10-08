package com.example.manuel.chatclient

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val context: Context, private val userName: String) : RecyclerView.Adapter<MessageAdapter.ViewHolder>(){

    var messages: MutableList<MessageFrom.TextMessageFromServer> = mutableListOf()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    enum class ElementType{
        OTHER,
        ME,
        SERVICE
    }

    override fun getItemViewType(position: Int): Int {
        if (messages[position].serviceMessage){
            return ElementType.SERVICE.ordinal
        }else if (messages[position].user == userName)
            return ElementType.ME.ordinal
        else
            return ElementType.OTHER.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (viewType == ElementType.SERVICE.ordinal)
            return ViewHolder(inflater.inflate(R.layout.message_element_service, parent,false) as LinearLayout)
        else if (viewType == ElementType.ME.ordinal)
            return ViewHolder(inflater.inflate(R.layout.message_element_me, parent,false) as LinearLayout)
        else
            return ViewHolder(inflater.inflate(R.layout.message_element, parent, false) as LinearLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val view = holder?.view ?: return

        val textMessage = view.findViewById<TextView>(R.id.textMessage)
        val textUser = view.findViewById<TextView>(R.id.textUser)
        val textTime = view.findViewById<TextView>(R.id.textTime)

        val format = SimpleDateFormat("HH:mm", Locale.US)
        val messageTime = Date(messages[position].timestamp)
        val timeAsString = format.format(messageTime)

        textTime.text = timeAsString
        textMessage.text = messages[position].text

        if (!messages[position].serviceMessage)
            textUser.text = messages[position].user

    }

    override fun getItemCount(): Int = messages.size

    fun update(messages: List<MessageFrom.TextMessageFromServer>){
        this.messages.clear()
        this.messages.addAll(messages)
        this.notifyDataSetChanged()
    }

}