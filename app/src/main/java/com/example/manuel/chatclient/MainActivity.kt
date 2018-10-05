package com.example.manuel.chatclient

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder

class MainActivity : AppCompatActivity(){


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startServerHandlerIntent = Intent(this, ServerHandler::class.java)
        startServerHandlerIntent.putExtra(Constants.addressExtraName, Constants.testIP)
        startServerHandlerIntent.putExtra(Constants.portExtraName, Constants.testPort)
        startService(startServerHandlerIntent)

        val startChatAdapterIntent = Intent(this, ChatActivity::class.java)
        startActivity(startChatAdapterIntent)
    }


}
