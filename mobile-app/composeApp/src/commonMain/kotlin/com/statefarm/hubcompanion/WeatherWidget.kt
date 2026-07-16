package com.statefarm.hubcompanion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val WeatherBlue = Color(0xFF173B67)
private val WeatherBlueLight = Color(0xFF3E79B7)
private val WeatherSecondary = Color(0xFFD8E8FA)

@Composable
internal fun WeatherWidget(hub: UserLocation) {
    var refreshCount by remember(hub) { mutableIntStateOf(0) }
    var state by remember(hub) { mutableStateOf<WeatherState>(WeatherState.Loading) }

    LaunchedEffect(hub, refreshCount) {
        WeatherLog.debug("Widget load started: hub=${hub.hubName}, refresh=$refreshCount")
        state = WeatherState.Loading
        state = try {
            WeatherState.Ready(WeatherRepository.weatherFor(hub)).also {
                WeatherLog.debug("Widget load completed: hub=${hub.hubName}")
            }
        } catch (throwable: Throwable) {
            WeatherLog.error(
                "Widget load failed: hub=${hub.hubName}, " +
                    "type=${throwable::class.simpleName}, message=${throwable.message}",
                throwable,
            )
            WeatherState.Error("Weather is temporarily unavailable")
        }
    }

    Box(
        Modifier.fillMaxWidth().background(
            brush = Brush.linearGradient(listOf(WeatherBlue, WeatherBlueLight)),
            shape = RoundedCornerShape(24.dp),
        ).padding(18.dp),
    ) {
        when (val currentState = state) {
            WeatherState.Loading -> WeatherLoading(hub)
            is WeatherState.Error -> WeatherError(hub, currentState.message) { refreshCount++ }
            is WeatherState.Ready -> WeatherContent(hub, currentState.weather) { refreshCount++ }
        }
    }
}

@Composable
private fun WeatherLoading(hub: UserLocation) {
    Column(Modifier.height(108.dp), verticalArrangement = Arrangement.SpaceBetween) {
        WeatherHeading(hub)
        Text("Loading live weather…", color = WeatherSecondary)
    }
}

@Composable
private fun WeatherError(hub: UserLocation, message: String, onRetry: () -> Unit) {
    Column(Modifier.height(108.dp), verticalArrangement = Arrangement.SpaceBetween) {
        WeatherHeading(hub)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(message, color = WeatherSecondary, style = MaterialTheme.typography.bodySmall)
            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRetry).padding(6.dp))
        }
    }
}

@Composable
private fun WeatherContent(hub: UserLocation, weather: WeatherSnapshot, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                WeatherHeading(hub)
                Text("${weather.condition} · H ${weather.high}°  L ${weather.low}°", color = WeatherSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${weather.temperature}°", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("↻ Refresh", color = WeatherSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable(onClick = onRefresh).padding(4.dp))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            weather.hourly.forEach { (time, temperature) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(time, color = WeatherSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("$temperature°", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Rain", color = WeatherSecondary, style = MaterialTheme.typography.labelSmall)
                Text("${weather.rainChance}%", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WeatherHeading(hub: UserLocation) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Color(0xFF75D99E), CircleShape))
            Text("LIVE WEATHER", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Text(hub.city, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
