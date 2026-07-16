package com.statefarm.hubcompanion

import android.util.Log

internal actual object WeatherLog {
    private const val TAG = "HubWeather"

    actual fun debug(message: String) {
        Log.d(TAG, message)
    }

    actual fun error(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}

