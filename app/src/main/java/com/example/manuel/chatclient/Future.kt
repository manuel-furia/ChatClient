/*
Author: Manuel Furia
Represents a simple implementation of basic self executing future of generic type.
The code will be executed at the moment of creation of the object, and the
result will be ready when completed.
*/

package com.example.manuel.chatclient

class Future<T>(code: () -> T) {

    /**
     * Contains the result once the computation completed (ready == true)
     * NOTE: The result can be null.
     */
    var result: T? = null
    private set

    /**
     * True if the computation has returned a value and is now completed
     */
    var ready: Boolean = false
    private set

    private val wrapper: () -> Unit = {result = code.invoke(); ready = true}
    private val thread = Thread(wrapper)

    init {
        thread.start()
    }
}