package com.example.manuel.chatclient

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.PrintStream
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

/**
 * Created by manuel on 9/17/18.
 */
class ServerHandler() : Service(), Observable<MessageFrom>, Observer<MessageTo>, ConnectionHandler{

    private data class ConnectionParameters(val address: String, val port: Int)
    private data class Connection(val socket: Socket, val reader: Scanner, val writer: PrintStream,
                                  val receivingThread: Thread, val pingThread: Thread)
    private data class ConnectionStatus(val lastPinged: Long){
        val connected: Boolean
        get() {
            val now = System.currentTimeMillis()
            val difference = now - lastPinged
            return    difference < Constants.connectionTimeoutAfterMilliseconds
        }
    }
    private data class ConnectionInfo(val connection: Future<Connection?>, val status: ConnectionStatus){
        val connected: Boolean
        get() {
            val socketOpen = connection?.result?.socket?.isConnected
            if (socketOpen != null) {
                return status.connected
            } else {
                return false
            }
        }
    }

    private val binder = LocalBinder()
    private val connections = mutableMapOf<ConnectionParameters, ConnectionInfo>()
    private val observers: MutableSet<Observer<MessageFrom>> = mutableSetOf()

    inner class LocalBinder : Binder() {
        //Return this instance of ServerHandler so clients can call public methods
        fun getService(): ServerHandler = this@ServerHandler
    }

    override fun update(event: MessageTo) {
        thread {
            val parameters = ConnectionParameters(event.host, event.port)
            val connection = connections[parameters]?.connection

            if (connection != null){
                if (connection.ready)
                    connection.result?.writer?.println("@" + event.room + " " + event.text)
            }
        }
    }

    override fun registerObserver(observer: Observer<MessageFrom>) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: Observer<MessageFrom>) {
        observers.remove(observer)
    }

    override fun notifyObservers(event: MessageFrom) {
        observers.forEach { it.update(event) }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = Service.START_STICKY

        /*if (intent == null) return mode
        val address: String = intent.getStringExtra(Constants.addressExtraName) ?: ""
        val port: Int = intent.getIntExtra(Constants.portExtraName, 0)
        if (address == "" || port == 0) return mode
        val connectionParameters = ConnectionParameters(address, port)
        if (connections.containsKey(connectionParameters)) return mode


        val connectionTask = Future<Connection?> {
            try {
                val socket = Socket(address, port)
                if (!socket.isClosed) {
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()
                    if (input != null && output != null) {
                        val reader = Scanner(input)
                        val writer = PrintStream(output)
                        val receivingThread = Thread {

                            while (!socket.isClosed) {
                                Thread.sleep(200)
                                if (!reader.hasNext()) continue
                                val messageText = reader.nextLine()
                                if (messageText == null) continue
                                val message = parseIncomingMessage(address, port, messageText)

                                if (message is MessageFrom.PingFromServer){

                                }

                                if (message != null) {
                                    notifyObservers(message)
                                }
                            }

                        }

                        val pingThread = Thread {
                            while(!socket.isClosed && socket.isConnected) {
                                val ping = MessageFrom.PingFromServer()
                                writer.println(Constants.pingString)
                                Thread.sleep(1000)
                            }

                        }

                        receivingThread.start()
                        pingThread.start()

                        Connection(socket, reader, writer, receivingThread, pingThread, 0L)

                    } else null
                } else null

            } catch (ex: Exception){
                Log.e("ERROR", ex.message)
                null
            }
        }
        connections[connectionParameters] = connectionTask
*/
        return mode

    }

    override fun onCreate() {
        super.onCreate()
    }

    private fun pingReceived(host: String, port: Int){
        val connectionParameters = ConnectionParameters(host, port)
        val oldConnection = connections[connectionParameters]
        val newConnection = oldConnection?.copy(status = ConnectionStatus(System.currentTimeMillis()))

        if (newConnection != null){
            synchronized(this) {
                connections[connectionParameters] = newConnection
            }
        }
    }

    override fun createConnection(address: String, port: Int): Boolean{
        if (address == "" || port == 0) return false
        val connectionParameters = ConnectionParameters(address, port)
        if (connections.containsKey(connectionParameters)) return false

        val connectionTask = Future<Connection?> {
            try {
                val socket = Socket(address, port)
                if (!socket.isClosed) {
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()
                    if (input != null && output != null) {
                        val reader = Scanner(input)
                        val writer = PrintStream(output)
                        val receivingThread = Thread {

                            while (!socket.isClosed) {
                                Thread.sleep(200)
                                if (!reader.hasNext()) continue
                                val messageText = reader.nextLine()
                                if (messageText == null) continue
                                val message = parseIncomingMessage(address, port, messageText)

                                if (message is MessageFrom.PingFromServer){
                                    pingReceived(address, port)
                                }

                                if (message != null) {
                                    notifyObservers(message)
                                }
                            }

                        }

                        val pingThread = Thread {
                            while(!socket.isClosed && socket.isConnected) {
                                val ping = MessageFrom.PingFromServer()
                                writer.println(Constants.pingString)
                                Thread.sleep(1000)
                            }

                        }

                        receivingThread.start()
                        pingThread.start()

                        Connection(socket, reader, writer, receivingThread, pingThread)

                    } else null
                } else null

            } catch (ex: Exception){
                Log.e("ERROR", ex.message)
                null
            }
        }

        synchronized(this) {
            connections[connectionParameters] = ConnectionInfo(connectionTask, ConnectionStatus(0))
        }

        return true
    }

    override fun getConnectedServers(): Set<ServerInfo>{
        val readyConnections = connections.filter { it.value.connection.ready }
        val activeConnections = readyConnections.filter {it.value.connected}
        val activeServers = activeConnections.map {
            ServerInfo(it.key.address, it.key.port, true)
        }
        return activeServers.toSet()
    }

    override fun isConnected(server: ServerInfo): Boolean {
        val connectionParameters = ConnectionParameters(server.host, server.port)
        val maybeConnection = connections[connectionParameters]

        return maybeConnection?.connected ?: false
    }

    override fun dropConnection(address: String, port: Int): Unit{
        val connectionParameters = ConnectionParameters(address, port)
        connections[connectionParameters]?.connection?.result?.socket?.close()
        connections.remove(connectionParameters)
    }

    private fun parseField(text: String, prefix: String, nextPrefix: String): Pair<String, String>{
        val prefixTrimmed = text.drop(prefix.length)
        val field = prefixTrimmed.split(nextPrefix).getOrNull(0) ?: ""
        val remainingText = prefixTrimmed.drop(field.length)

        return Pair(field, remainingText)
    }

    private fun parseIncomingMessage(host:String, port: Int, msg: String): MessageFrom? {

        if (msg.startsWith(Constants.pingString)) {
            return MessageFrom.PingFromServer()
        } else if (msg.startsWith(Constants.serviceMessagePrefix)){
            val trimmed = msg.drop(Constants.serviceMessagePrefix.length).trimStart()
            if (trimmed.startsWith(Constants.roomPrefix)){
                val roomSequence = trimmed.split(" ").getOrNull(0)
                val roomName = roomSequence?.drop(1) ?: ""
                val message = trimmed.drop(roomSequence?.length ?: 0)
                return MessageFrom.TextMessageFromServer(host, port, "", roomName, System.currentTimeMillis(), message, serviceMessage = true)
            } else {
                return MessageFrom.TextMessageFromServer(host, port, "", "", System.currentTimeMillis(), trimmed, serviceMessage = true)
            }
        } else if (msg.startsWith(Constants.roomPrefix)){

            val (room, msgWithoutRoom) = parseField(msg, Constants.roomPrefix, Constants.timestampPrefix)
            val (timestamp, msgWithoutTimestamp) = parseField(msgWithoutRoom, Constants.timestampPrefix, Constants.usernamePrefix)
            val (user, messageText) = parseField(msgWithoutTimestamp, Constants.usernamePrefix, Constants.messageTextPrefix)

            val timestampLong = timestamp.toLongOrNull() ?: System.currentTimeMillis()

            return MessageFrom.TextMessageFromServer(host, port, user, room, timestampLong, messageText, serviceMessage = false)
        }

        return null

    }



}