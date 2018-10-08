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


    /** Defines callbacks for service binding, passed to bindService()  */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            MainActivityState.setServerHandler((service as ServerHandler.LocalBinder).getService())
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            MainActivityState.dropServerHandler()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = this.getSharedPreferences(Constants.preferencesFileName, Context.MODE_PRIVATE)
        val storedSelectedServer = sharedPreferences.getString(Constants.selectedServerPrefName, "")
        val storedServerList = sharedPreferences.getStringSet(Constants.serverListPrefName, setOf())
        val storedUsername = sharedPreferences.getString(Constants.usernamePrefName, "")

        MainActivityState.storableStateModifiedCallback = { servers, selected, username ->
            val sharedPreferences = this.getSharedPreferences(Constants.preferencesFileName, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val serversAsString = servers.map {it.host + ":" + it.port}.toSet()
            val selectedAsString = selected?.host?.plus(":")?.plus(selected.port.toString())
            editor.putStringSet(Constants.serverListPrefName, serversAsString)
            editor.putString(Constants.selectedServerPrefName, selectedAsString)
            editor.putString(Constants.usernamePrefName, username)
            editor.apply()
        }

        storedServerList.forEach {
            val host = it.split(":").take(1)[0]
            val port = it.split(":").drop(1)[0].toIntOrNull()

            if (host != "" && port != null && port > 0) {
                val server = ServerInfo(host, port)
                MainActivityState.addServer(server)
            }
        }

        if (storedSelectedServer != ""){
            val host = storedSelectedServer.split(":").take(1)[0]
            val port = storedSelectedServer.split(":").drop(1)[0].toIntOrNull()
            if (host != "" && port != null && port > 0)
                MainActivityState.selectedServer = ServerInfo(host, port)
        }

        if (storedUsername != ""){
            MainActivityState.username = storedUsername
        }


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

        val bindServerHandlerIntent = Intent(this, ServerHandler::class.java)
        bindService(bindServerHandlerIntent, serviceConnection, 0)

    }


}
