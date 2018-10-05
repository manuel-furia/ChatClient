package com.example.manuel.chatclient

class Future<T>(code: () -> T) {

    var result: T? = null
    private set

    var ready: Boolean = false
    private set

    private val wrapper: () -> Unit = {result = code.invoke(); ready = true}
    private val thread = Thread(wrapper)

    init {
        thread.start()
    }
}