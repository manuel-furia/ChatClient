/*
Author: Manuel Furia
Adapter for a room element in the rooms fragment
*/


package com.example.manuel.chatclient

import android.app.AlertDialog
import android.support.v4.app.Fragment
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class RoomElemAdapter(private val fragment: Fragment, private val roomsUpdateCallback: () -> Unit) : RecyclerView.Adapter<RoomElemAdapter.ViewHolder>(){

    private var rooms: MutableList<RoomInfo> = mutableListOf()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    enum class ElementType{
        JOINED,
        NOTJOINED
    }

    override fun getItemViewType(position: Int): Int {
        if (rooms[position].joined){
            return ElementType.JOINED.ordinal
        } else
            return ElementType.NOTJOINED.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = fragment.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (viewType == ElementType.JOINED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.room_element, parent,false) as LinearLayout)
        else (viewType == ElementType.NOTJOINED.ordinal)
            return ViewHolder(inflater.inflate(R.layout.room_element, parent,false) as LinearLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val view = holder?.view ?: return

        val textRoomElemName = view.findViewById<TextView>(R.id.textRoomElemName)
        val imageRoomElemStatus = view.findViewById<ImageView>(R.id.imageRoomElemStatus)
        val buttonLeave = view.findViewById<ImageButton>(R.id.buttonJoinLeave)
        //val roomElemClickableArea = view.findViewById<LinearLayout>(R.id.clickableAreaRoomElemStatus)

        val host = MainActivityState.selectedServer?.host ?: return
        val port = MainActivityState.selectedServer?.port ?: return

        val room = rooms[position]

        val leaveRoom = View.OnClickListener {
            if (room.name != Constants.mainServerRoom) {
                //Get a reference to the MessageTo observer and send it a leave message
                MainActivityState.messageToObserver?.update(MessageTo(host, port, room.name, ":leave"))
                roomsUpdateCallback.invoke()
            } else {
                AlertDialog.Builder(fragment.context)
                        .setTitle("Error")
                        .setMessage("You can't leave the main room")
                        .show()
            }
        }


        val joinRoom = View.OnClickListener {
            if ((MainActivityState.username ?: "") != ""){
                val username = MainActivityState.username
                //Get a reference to the MessageTo observer and notify it of the username (it might be not set)
                //and the intento to create or join a room
                MainActivityState.messageToObserver?.update(MessageTo(host, port, Constants.mainServerRoom, ":user $username"))
                MainActivityState.messageToObserver?.update(MessageTo(host, port, Constants.mainServerRoom, ":room ${room.name}"))
                MainActivityState.selectedRoom = room
                val startChatAdapterIntent = Intent(fragment.context, ChatActivity::class.java)
                fragment.startActivityForResult(startChatAdapterIntent, Constants.activityResultLeftRoom)
                roomsUpdateCallback.invoke()
            } else {
                AlertDialog.Builder(fragment.context)
                        .setTitle("Error")
                        .setMessage("Set a username first.")
                        .show()
            }

        }

        textRoomElemName.text = room.name

        //Show different color and enable the leave button depending on the joined status of the room
        if (room.joined){
            val color = ContextCompat.getColor(fragment.context, R.color.colorConnected)
            imageRoomElemStatus?.imageTintList = ColorStateList.valueOf(color)
            buttonLeave.visibility = View.VISIBLE
            buttonLeave.setOnClickListener (leaveRoom)
        } else {
            val color = ContextCompat.getColor(fragment.context, R.color.colorDisconnected)
            imageRoomElemStatus?.imageTintList = ColorStateList.valueOf(color)
            buttonLeave.visibility = View.GONE

            if (room == MainActivityState.selectedRoom)
                MainActivityState.selectedRoom = null
        }

        textRoomElemName.setOnClickListener(joinRoom)

    }

    override fun getItemCount(): Int = rooms.size

    fun update(rooms: List<RoomInfo>){
        this.rooms.clear()
        this.rooms.addAll(rooms)
        this.notifyDataSetChanged()
    }

    fun update(){
        this.notifyDataSetChanged()
    }

}