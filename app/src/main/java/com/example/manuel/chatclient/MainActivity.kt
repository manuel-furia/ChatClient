package com.example.manuel.chatclient

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.media.audiofx.BassBoost
import android.os.Binder
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

        val startServerHandlerIntent = Intent(this, ServerHandler::class.java)
        //startServerHandlerIntent.putExtra(Constants.addressExtraName, Constants.testIP)
        //startServerHandlerIntent.putExtra(Constants.portExtraName, Constants.testPort)
        startService(startServerHandlerIntent)

        //Create the fragments for the main activity
        val fragmentServer = ServerFragment()
        val fragmentRoom = RoomFragment()
        val fragmentUser = UserFragment()

        val fragmentPagerAdapter = MainFragmentPagerAdapter(
                listOf(fragmentServer, fragmentRoom, fragmentUser),
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
                    2 -> R.id.navigation_users
                    else -> R.id.navigation_servers
                }
            }
        })

        mainBottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.navigation_servers -> mainViewPager.currentItem = 0
                R.id.navigation_rooms -> mainViewPager.currentItem = 1
                R.id.navigation_users -> mainViewPager.currentItem = 2
            }

            true
        }

        val bindServerHandlerIntent = Intent(this, ServerHandler::class.java)
        bindService(bindServerHandlerIntent, serviceConnection, 0)

        //val startChatAdapterIntent = Intent(this, ChatActivity::class.java)
        //startActivity(startChatAdapterIntent)
    }


}
