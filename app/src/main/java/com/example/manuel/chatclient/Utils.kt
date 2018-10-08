/*
Author: Manuel Furia
Utils functions to be used globally in the application.
*/
package com.example.manuel.chatclient

import android.os.Handler
import android.os.Looper

object Utils {
    /**
     * Execute this task in the main UI loop, even if we are in another thread
     */
    fun<T> futureUITask(code: () -> T){
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            code.invoke()
        }
    }
}