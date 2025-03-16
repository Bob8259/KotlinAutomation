package com.example.touch

/**
 * 单例对象，用于持有TouchGestureService实例
 */
object TouchGestureServiceHolder {
    var instance: TouchGestureService? = null
        private set
    
    fun setInstance(service: TouchGestureService?) {
        instance = service
    }
} 