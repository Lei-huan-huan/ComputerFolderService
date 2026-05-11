package com.computerfolder.app

/** 线程安全；HTTP /selected 与界面共用 */
internal class SelectionStore {
    private val lock = Any()
    private val order = mutableListOf<String>()

    fun toggle(relativePath: String) {
        synchronized(lock) {
            if (!order.remove(relativePath)) order.add(relativePath)
        }
    }

    fun remove(relativePath: String) {
        synchronized(lock) {
            order.remove(relativePath)
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { order.toList() }
}
