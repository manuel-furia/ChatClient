/*
Author: Manuel Furia
Generic observer for the observer pattern
*/

package com.example.manuel.chatclient

interface Observer<T> {
    fun update(event: T)
}