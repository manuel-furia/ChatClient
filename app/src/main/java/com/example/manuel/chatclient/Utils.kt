package com.example.manuel.chatclient

import android.os.Handler
import android.os.Looper

object Utils {
    fun<T> futureUITask(code: () -> T){
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            code.invoke()
        }
    }
}