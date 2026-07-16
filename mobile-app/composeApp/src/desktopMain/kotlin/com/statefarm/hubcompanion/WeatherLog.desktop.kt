package com.statefarm.hubcompanion

internal actual object WeatherLog {
    actual fun debug(message: String) {
        println("[HubWeather] $message")
    }

    actual fun error(message: String, throwable: Throwable?) {
        System.err.println("[HubWeather] ERROR: $message")
        throwable?.printStackTrace()
    }
}

