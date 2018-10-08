package com.example.manuel.chatclient

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_chat.*

class ChatFragment: Fragment(),  Observer<MessageFrom>, Observable<MessageTo>  {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_chat, container, false);
    }

    private var observable: Observable<MessageFrom>? = null
    private var connectionHandler: ConnectionHandler? = null
    private var observers: MutableSet<Observer<MessageTo>> = mutableSetOf()

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser) {
            fetchOldMessages()
        }
    }

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

    override fun onResume() {
        super.onResume()

        val host = MainActivityState.selectedServer?.host
        val room = MainActivityState.selectedRoom?.name
        if (host != null && room != null) {
            (activity as ChatActivity).setToolbarInfo(host, room)
        }

        observable = MainActivityState.messageFromObservable
        observable?.registerObserver(this)

        fetchOldMessages()
    }

    override fun onStop() {
        super.onStop()
        observable?.unregisterObserver(this)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        messagesView.layoutManager = LinearLayoutManager(context)

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            sendMessage(messageText)
            messageEditText.text.clear()
        }

        messageEditText.setOnClickListener{
            Future {
                Thread.sleep(200)
                Utils.futureUITask { messagesView.scrollToPosition(messages.size - 1) }
                Thread.sleep(500)
                Utils.futureUITask { messagesView.scrollToPosition(messages.size - 1) }
            }
        }

        val observer: Observer<MessageTo>? = MainActivityState.messageToObserver
        if (observer != null) observers.add(observer)

        connectionHandler = MainActivityState.connectionHandler
        connectionHandler?.createConnection(Constants.testIP, Constants.testPort)

        fetchOldMessages()
    }

    override fun update(event: MessageFrom) {
        when (event) {
            is MessageFrom.TextMessageFromServer -> {
                val host = MainActivityState.selectedServer?.host
                val port = MainActivityState.selectedServer?.port
                val room = MainActivityState.selectedRoom?.name
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
        if (messagesView == null) return
        val username = MainActivityState.username
        if (messagesView.adapter == null && username != null) {
            val adapter = MessageAdapter(context, username)
            messagesView.adapter = adapter
        }
        (messagesView.adapter as MessageAdapter).update(messages)
        messagesView.scrollToPosition(messages.size-1)
    }

    override fun onDestroy() {
        super.onDestroy()
        val observer = MainActivityState.messageToObserver
        if (observer != null) {
            unregisterObserver(observer)
        }
    }

    private fun fetchOldMessages(){
        val host = MainActivityState.selectedServer?.host
        val port = MainActivityState.selectedServer?.port
        val finalHost = host
        val finalPort = port

        if (finalHost != null && finalPort != null) {
            val textMessages = MainActivityState.messageListProvider?.getMessages(finalHost, finalPort) ?: listOf<MessageFrom>()
            messages.clear()
            textMessages.filterIsInstance<MessageFrom.TextMessageFromServer>().forEach {
                addMessageInSameThread(it)
            }
        }
    }

    private fun addMessageInSameThread(event: MessageFrom.TextMessageFromServer) {
        val host = MainActivityState.selectedServer?.host
        val port = MainActivityState.selectedServer?.port
        val room = MainActivityState.selectedRoom?.name
        val correctServer = event.host == host && event.port == port
        val correctRoom = event.room == room || event.room == ""
        if (correctServer && correctRoom) {
            messages.add(event)
            updateMessages()
        }
    }



}