package com.statefarm.hubcompanion

internal expect object WeatherLog {
    fun debug(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

