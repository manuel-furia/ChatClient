/*
Author: Manuel Furia
Handles the network connection to multiple servers, recording all the messages received and
notifying observers. In addition, checks via pings if a connection is alive.
*/

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
class ServerHandler() : Service(), Observable<MessageFrom>, Observer<MessageTo>, ConnectionHandler, MessageListProvider{

    //Identifies the server
    private data class ConnectionParameters(val address: String, val port: Int)
    //Represent the tcp connection, the thread that handle it and the data received since its beginning
    private data class Connection(val socket: Socket, val reader: Scanner, val writer: PrintStream,
                                  val receivingThread: Thread, val pingThread: Thread,
                                  val receivedMessages: MutableList<MessageFrom>)
    //Respresent the status of a connection (when did we receive the last ping)
    private data class ConnectionStatus(val lastPinged: Long){
        val connected: Boolean //Verify if a connection is alive by checking the last ping received
        get() {
            val now = System.currentTimeMillis()
            val difference = now - lastPinged
            return    difference < Constants.connectionTimeoutAfterMilliseconds
        }
    }

    //Store a connection (that might not be open yet, see Future) and its status
    private data class ConnectionInfo(val connection: Future<Connection?>, val status: ConnectionStatus){
        val connected: Boolean //Verify if connected (see above)
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
    //Store the connections and their status using their parameters (host and port) as keys
    private val connections = mutableMapOf<ConnectionParameters, ConnectionInfo>()

    private val observers: MutableSet<Observer<MessageFrom>> = mutableSetOf()

    inner class LocalBinder : Binder() {
        //Return this instance of ServerHandler so clients can call public methods
        fun getService(): ServerHandler = this@ServerHandler
    }

    //Received a message from the client to forward to the server
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

        return mode

    }

    override fun onCreate() {
        super.onCreate()
    }

    //Received a ping, register the current time
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

    //Get all the old messages received from a connection since it staterd
    override fun getMessages(host: String, port: Int): List<MessageFrom>? {
        val connectionParameters = ConnectionParameters(host, port)
        val connection = connections[connectionParameters]?.connection?.result

        if (connection != null){
            return connection.receivedMessages
        }

        return null
    }

    override fun createConnection(address: String, port: Int): Boolean{
        if (address == "" || port == 0) return false
        val connectionParameters = ConnectionParameters(address, port)
        if (connections.containsKey(connectionParameters)) return false

        //Create the future for the connection (will return a Connection when opened)
        val connectionTask = Future<Connection?> {
            try {
                val socket = Socket(address, port)
                if (!socket.isClosed) {
                    val input = socket.getInputStream()
                    val output = socket.getOutputStream()
                    val receivedMessages: MutableList<MessageFrom> = mutableListOf()
                    if (input != null && output != null) {
                        val reader = Scanner(input)
                        val writer = PrintStream(output)
                        //The thread that will be listening for incoming messages
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
                                    receivedMessages.add(message)
                                }
                            }

                        }
                        //The thread that will be regularly pinging the server
                        val pingThread = Thread {
                            while(!socket.isClosed && socket.isConnected) {
                                val ping = MessageFrom.PingFromServer()
                                writer.println(Constants.pingString)
                                Thread.sleep(1000)
                            }

                        }

                        receivingThread.start()
                        pingThread.start()

                        Connection(socket, reader, writer, receivingThread, pingThread, receivedMessages)

                    } else null
                } else null

            } catch (ex: Exception){
                Log.e("ERROR", ex.message)
                null
            }
        }
        //Add the future connection and its status to the connections table
        synchronized(this) {
            connections[connectionParameters] = ConnectionInfo(connectionTask, ConnectionStatus(0))
        }

        return true
    }

    //Get all the servers with an alive connection
    override fun getConnectedServers(): Set<ServerInfo>{
        val readyConnections = connections.filter { it.value.connection.ready }
        val activeConnections = readyConnections.filter {it.value.connected}
        val activeServers = activeConnections.map {
            ServerInfo(it.key.address, it.key.port)
        }
        return activeServers.toSet()
    }

    //Is the connection of this server alive?
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

    //Parse a portion of the incoming message (from one prefix to another)
    private fun parseField(text: String, prefix: String, nextPrefix: String): Pair<String, String>{
        val prefixTrimmed = text.drop(prefix.length)
        val field = prefixTrimmed.split(nextPrefix).getOrNull(0) ?: ""
        val remainingText = prefixTrimmed.drop(field.length)

        return Pair(field, remainingText)
    }

    private fun parseIncomingMessage(host:String, port: Int, msg: String): MessageFrom? {
        //Check the type of message by its prefix, and parse it field by field
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
        } else if (msg.startsWith(Constants.serviceParsableMessagePrefix)){
            val trimmed = msg.drop(Constants.serviceParsableMessagePrefix.length).trimStart()
            return MessageFrom.ParsableInfoFromServer(host, port, trimmed)
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