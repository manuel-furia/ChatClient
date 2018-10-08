/*
Author: Manuel Furia
The main activity of the app, used to select server and rooms the user wants to join
*/
package com.example.manuel.chatclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.view.ViewPager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(){


    //Callbacks for service binding
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            //Set the network service to the main state, so other activities can access/
            //its functionalities via interfaces
            MainActivityState.setServerHandler((service as ServerHandler.LocalBinder).getService())
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            MainActivityState.dropServerHandler()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Fetch stored preferences
        val sharedPreferences = this.getSharedPreferences(Constants.preferencesFileName, Context.MODE_PRIVATE)
        val storedSelectedServer = sharedPreferences.getString(Constants.selectedServerPrefName, "")
        val storedServerList = sharedPreferences.getStringSet(Constants.serverListPrefName, setOf())
        val storedUsername = sharedPreferences.getString(Constants.usernamePrefName, "")

        //This callback will be executed when one of the values that can be stored in preferences
        //changes
        MainActivityState.storableStateModifiedCallback = { servers, selected, username ->
            val sharedPreferences = this.getSharedPreferences(Constants.preferencesFileName, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            //Tansform ServerInfo into strings
            val serversAsString = servers.map {it.host + ":" + it.port}.toSet()
            val selectedAsString = selected?.host?.plus(":")?.plus(selected.port.toString())
            editor.putStringSet(Constants.serverListPrefName, serversAsString)
            editor.putString(Constants.selectedServerPrefName, selectedAsString)
            editor.putString(Constants.usernamePrefName, username)
            editor.apply()
        }

        //Fetch the list of stored servers, serialized as strings
        storedServerList.forEach {
            val host = it.split(":").take(1)[0]
            val port = it.split(":").drop(1)[0].toIntOrNull()

            if (host != "" && port != null && port > 0) {
                val server = ServerInfo(host, port)
                MainActivityState.addServer(server)
            }
        }

        //Fetch the stored selected server, serialized as string
        if (storedSelectedServer != ""){
            val host = storedSelectedServer.split(":").take(1)[0]
            val port = storedSelectedServer.split(":").drop(1)[0].toIntOrNull()
            if (host != "" && port != null && port > 0)
                MainActivityState.selectedServer = ServerInfo(host, port)
        }

        //Fetch the stored username
        if (storedUsername != ""){
            MainActivityState.username = storedUsername
        }

        //Start the network service
        val startServerHandlerIntent = Intent(this, ServerHandler::class.java)
        startService(startServerHandlerIntent)

        //Create the fragments for the main activity
        val fragmentServer = ServerFragment()
        val fragmentRoom = RoomFragment()

        val fragmentPagerAdapter = MainFragmentPagerAdapter(
                listOf(fragmentServer, fragmentRoom),
                supportFragmentManager
        )

        mainViewPager.adapter = fragmentPagerAdapter

        //Setup the interaction between pager and menu
        mainViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                mainBottomNavigation.selectedItemId = when (position) {
                    0 -> R.id.navigation_servers
                    1 -> R.id.navigation_rooms
                    else -> R.id.navigation_servers
                }
            }
        })

        mainBottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.navigation_servers -> mainViewPager.currentItem = 0
                R.id.navigation_rooms -> mainViewPager.currentItem = 1
            }

            true
        }

        //Bind to the network service
        val bindServerHandlerIntent = Intent(this, ServerHandler::class.java)
        bindService(bindServerHandlerIntent, serviceConnection, 0)

    }

}
