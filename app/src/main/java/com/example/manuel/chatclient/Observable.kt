/*
Author: Manuel Furia
Generic observable for the observer pattern
*/

package com.example.manuel.chatclient

interface Observable<T> {

    fun registerObserver(observer: Observer<T>)
    fun unregisterObserver(observer: Observer<T>)
    fun notifyObservers(event: T)

}