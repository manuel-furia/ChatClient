package com.example.manuel.chatclient

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_server.*
import kotlinx.android.synthetic.main.message_element.*

class ChatActivity : AppCompatActivity(), Observer<MessageFrom>, Observable<MessageTo> {


    private var observable: Observable<MessageFrom>? = null
    private var connectionHandler: ConnectionHandler? = null
    private var observers: MutableSet<Observer<MessageTo>> = mutableSetOf()

    private var username = "manuel"
    private var host = Constants.testIP
    private var room = "hall"
    private var port = Constants.testPort

    private var lastPingReceivedTimestamp = 0L

/*
    /** Defines callbacks for service binding, passed to bindService()  */
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val server = (service as ServerHandler.LocalBinder).getService()
            observable = server
            observable?.registerObserver(this@ChatActivity)
            observers.add(server)

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            observable?.unregisterObserver(this@ChatActivity)
            unregisterObserver(observable as Observer<MessageTo>)
        }
    }
*/
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
        val message = MessageTo(host, port, room, msg)
        notifyObservers(message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setSupportActionBar(chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //val bindServerHandlerIntent = Intent(this, ServerHandler::class.java)

        //bindService(bindServerHandlerIntent, serviceConnection, 0)

        messagesView.layoutManager = LinearLayoutManager(this.baseContext)

        serverNameText.text = host
        roomNameText.text = room

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            sendMessage(messageText)
            messageEditText.text.clear()
        }

        messageEditText.setOnClickListener{
            Future {
                Thread.sleep(200)
                futureUITask {messagesView.scrollToPosition(messages.size - 1)}
                Thread.sleep(500)
                futureUITask {messagesView.scrollToPosition(messages.size - 1)}
            }
        }

        leaveButton.setOnClickListener{
            sendMessage(":leave")
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
            is MessageFrom.TextMessageFromServer -> {
                val correctServer = event.host == host && event.port == port
                val correctRoom = event.room == room || event.room == ""
                if (correctServer && correctRoom) {
                    val handler = Handler(Looper.getMainLooper())

                    handler.post {
                        messages.add(event)
                        updateMessages()
                    }
                }
            }
        }
    }

    fun updateMessages(){
        if (messagesView.adapter == null) {
            val adapter = MessageAdapter(this.baseContext, username)
            messagesView.adapter = adapter
        }
        (messagesView.adapter as MessageAdapter).update(messages)
        messagesView.scrollToPosition(messages.size-1)
    }

    override fun onDestroy() {
        super.onDestroy()
        //unbindService(serviceConnection)
        val observer = MainActivityState.messageToObserver
        if (observer != null) {
            unregisterObserver(observer)
        }
    }

    private fun checkConnection(){
        if (System.currentTimeMillis() > lastPingReceivedTimestamp + 3000){
            val color = ContextCompat.getColor(this.baseContext, R.color.colorDisconnected)
            imageConnected?.imageTintList = ColorStateList.valueOf(color)
        } else {
            val color = ContextCompat.getColor(this.baseContext, R.color.colorConnected)
            imageConnected?.imageTintList = ColorStateList.valueOf(color)
        }
    }

    fun<T> futureUITask(code: () -> T){
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            code.invoke()
        }
    }
}
