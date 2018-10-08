package com.example.manuel.chatclient

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_chat.*

import com.example.manuel.chatclient.Utils.futureUITask

class ChatActivity : AppCompatActivity(), Observer<MessageFrom>, Observable<MessageTo> {


    private var observable: Observable<MessageFrom>? = null
    private var connectionHandler: ConnectionHandler? = null
    private var observers: MutableSet<Observer<MessageTo>> = mutableSetOf()

    private var lastPingReceivedTimestamp = 0L


    override fun notifyObservers(event: MessageTo) {
        observers.forEach {it.update(event)}
    }

    override fun registerObserver(observer: Observer<MessageTo>) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: Observer<MessageTo>) {
        observers.remove(observer)
    }
    private val messages: MutableList<MessageFrom.TextMessageFromServer> = mutableListOf()

    fun sendMessage(msg: String){
        val host = MainActivityState.selectedServer?.host
        val port = MainActivityState.selectedServer?.port
        val room = MainActivityState.selectedRoom?.name
        if (host != null && port != null && room != null) {
            val message = MessageTo(host, port, room, msg)
            notifyObservers(message)
        }
    }

    fun setToolbarInfo(host: String, room: String) {
        val username = MainActivityState.username
        val roomText = if (!room.contains(".")) {
            room
        } else if (username == null) {
            room
        } else {
            "Pvt: " + room.split(".").minus(username).joinToString()
        }
        serverNameText.text = host
        roomNameText.text = roomText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setSupportActionBar(chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        //Create the fragments for the main activity
        val fragmentChat = ChatFragment()
        val fragmentUsers = UsersFragment()

        val fragmentPagerAdapter = MainFragmentPagerAdapter(
                listOf(fragmentChat, fragmentUsers),
                supportFragmentManager
        )

        chatViewPager.adapter = fragmentPagerAdapter

        chatViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                chatBottomNavigation.selectedItemId = when (position) {
                    0 -> R.id.navigation_chat_chat
                    1 -> R.id.navigation_chat_users
                    else -> R.id.navigation_chat_chat
                }
            }
        })

        chatBottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId){
                R.id.navigation_chat_chat -> chatViewPager.currentItem = 0
                R.id.navigation_chat_users -> chatViewPager.currentItem = 1
            }

            true
        }


        leaveButton.setOnClickListener{
            sendMessage(":leave")
            this.finish()
        }

        observable = MainActivityState.messageFromObservable
        observable?.registerObserver(this)

        val observer: Observer<MessageTo>? = MainActivityState.messageToObserver
        if (observer != null) observers.add(observer)

        connectionHandler = MainActivityState.connectionHandler
        connectionHandler?.createConnection(Constants.testIP, Constants.testPort)

        //Check the connection every second
        Future {
            while (!this.isDestroyed) {
                futureUITask { checkConnection() }
                Thread.sleep(1000)
            }

        }
    }

    override fun update(event: MessageFrom) {
        when (event) {
            is MessageFrom.PingFromServer -> {
                futureUITask { lastPingReceivedTimestamp = System.currentTimeMillis() }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        val observer = MainActivityState.messageToObserver
        if (observer != null) {
            unregisterObserver(observer)
        }
        observable?.unregisterObserver(this)
    }

    fun checkConnection(){
        if (System.currentTimeMillis() > lastPingReceivedTimestamp + Constants.connectionTimeoutAfterMilliseconds){
            val color = ContextCompat.getColor(this.baseContext, R.color.colorDisconnected)
            imageConnected?.imageTintList = ColorStateList.valueOf(color)
        } else {
            val color = ContextCompat.getColor(this.baseContext, R.color.colorConnected)
            imageConnected?.imageTintList = ColorStateList.valueOf(color)
        }
    }

    fun goToChatFragment(host: String? = null, room: String? = null){
        if (host != null && room != null){
            setToolbarInfo(host, room)
        }
        chatViewPager.currentItem = 0
    }


}
