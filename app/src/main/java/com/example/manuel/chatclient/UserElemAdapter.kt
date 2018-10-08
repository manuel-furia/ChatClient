/*
Author: Manuel Furia
Represent a user in a recyclerView, with a button to open a private message room.
*/

package com.example.manuel.chatclient

import android.app.AlertDialog
import android.support.v4.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class UserElemAdapter(private val fragment: Fragment, private val userPvtCallback: () -> Unit) : RecyclerView.Adapter<UserElemAdapter.ViewHolder>(){

    private var users: MutableList<UserInfo> = mutableListOf()

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val inflater = fragment.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return ViewHolder(inflater.inflate(R.layout.user_element, parent,false) as LinearLayout)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val view = holder?.view ?: return

        val textUserElemName = view.findViewById<TextView>(R.id.textUserElemName)
        val buttonPvt = view.findViewById<ImageButton>(R.id.buttonOpenPvt)
        //val roomElemClickableArea = view.findViewById<LinearLayout>(R.id.clickableAreaRoomElemStatus)

        val host = MainActivityState.selectedServer?.host ?: return
        val port = MainActivityState.selectedServer?.port ?: return

        val user = users[position]
        val username = user.username

        textUserElemName.text = username

        if (username != Constants.serverConsoleName) {
            buttonPvt.setOnClickListener {
                val myUsername = MainActivityState.username ?: ""
                val pvtRoomName = listOf(myUsername, username).sorted().joinToString(".")
                MainActivityState.messageToObserver?.update(MessageTo(host, port, Constants.mainServerRoom, ":pvt $username"))
                MainActivityState.selectedRoom = RoomInfo(pvtRoomName, true)
                userPvtCallback.invoke()
            }
        } else {
            buttonPvt.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int = users.size

    fun update(users: List<UserInfo>){
        this.users.clear()
        this.users.addAll(users)
        this.notifyDataSetChanged()
    }

    fun update(){
        this.notifyDataSetChanged()
    }

}