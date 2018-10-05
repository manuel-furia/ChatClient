package com.example.manuel.chatclient

interface Observer<T> {
    fun update(event: T)
}