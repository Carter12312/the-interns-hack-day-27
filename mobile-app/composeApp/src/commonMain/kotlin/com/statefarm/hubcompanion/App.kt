package com.statefarm.hubcompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val BrandRed = Color(0xFFD82117)
private val Ink = Color(0xFF181A1F)
private val SecondaryText = Color(0xFF666A73)
private val CanvasColor = Color(0xFFF5F6F9)
private val BorderColor = Color(0xFFE1E3E8)
private val Success = Color(0xFF147A4A)
private val SuccessSoft = Color(0xFFE0F5E8)
private val BlueSoft = Color(0xFFE4F1FC)
private val AmberSoft = Color(0xFFFFF1D1)

internal data class UserLocation(
    val hubName: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
)

private data class ParkingSpaceUi(
    val id: String,
    val isOpen: Boolean,
    val type: String = "Standard",
)

private data class ParkingLotUi(
    val id: String,
    val name: String,
    val distanceMinutes: Int,
    val walkMinutes: Int,
    val spaces: List<ParkingSpaceUi>,
) {
    val openSpaces: Int get() = spaces.count { it.isOpen }
}

private enum class Screen(val label: String, val symbol: String) {
    Home("Home", "⌂"), Parking("Parking", "P"), Navigate("Navigate", "→"), Jake("Jake", "J")
}

@Composable
fun HubCompanionApp() {
    var screen by remember { mutableStateOf(Screen.Home) }
    val hubs = remember { demoHubs() }
    var selectedHub by remember { mutableStateOf(hubs.first()) }

    MaterialTheme {
        Surface(color = CanvasColor, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            location = selectedHub,
                            hubs = hubs,
                            onHubSelected = { selectedHub = it },
                            onNavigate = { screen = it },
                        )
                        Screen.Parking -> ParkingScreen(selectedHub) { screen = Screen.Navigate }
                        Screen.Navigate -> NavigationScreen()
                        Screen.Jake -> JakeScreen(
                            onParking = { screen = Screen.Parking },
                            onNavigate = { screen = Screen.Navigate },
                        )
                    }
                }
                AppNavigation(screen) { screen = it }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    location: UserLocation,
    hubs: List<UserLocation>,
    onHubSelected: (UserLocation) -> Unit,
    onNavigate: (Screen) -> Unit,
) = Page {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Good morning", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            Text("Carter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        IconTile("C", Ink, Color.White, 44.dp)
    }

    HubSelector(location, hubs, onHubSelected)
    WeatherWidget(location)

    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.Parking) }) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile("P", BrandRed, Color.White, 42.dp)
                    Column {
                        Text("Parking ready", fontWeight = FontWeight.Bold)
                        Text("Best option for Building A", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Pill("42 OPEN", SuccessSoft, Success)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("South Garage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("4 min walk · High confidence", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Text("View  →", color = BrandRed, fontWeight = FontWeight.Bold)
            }
        }
    }

    Text("Around the hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction("→", "Navigate", "Find rooms", Modifier.weight(1f)) { onNavigate(Screen.Navigate) }
        QuickAction("J", "Ask Jake", "Campus help", Modifier.weight(1f)) { onNavigate(Screen.Jake) }
    }
    FeatureRow("L", AmberSoft, "Lunch at Café 1", "Tacos · salads · sandwiches", "View  →") {}
}

private fun demoHubs(): List<UserLocation> = listOf(
    UserLocation("Dallas Hub", "Dallas, TX", 32.7767, -96.7970),
    UserLocation("Bloomington Hub", "Bloomington, IL", 40.4842, -88.9937),
    UserLocation("Phoenix Hub", "Phoenix, AZ", 33.4484, -112.0740),
)

@Composable
private fun HubSelector(
    selectedHub: UserLocation,
    hubs: List<UserLocation>,
    onSelected: (UserLocation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconTile("H", BlueSoft, Ink, 36.dp)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("Current hub", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                    Text(selectedHub.hubName, fontWeight = FontWeight.Bold)
                }
                Text("Change  ▾", color = BrandRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            hubs.forEach { hub ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(hub.hubName, fontWeight = FontWeight.SemiBold)
                            Text(hub.city, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(hub)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ParkingScreen(
    location: UserLocation,
    onNavigate: () -> Unit,
) {
    var expandedLotId by remember { mutableStateOf<String?>("south-garage") }
    val lots = remember { demoParkingLots() }

    Page {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("AI Parking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Nearby lots at ${location.hubName}", color = SecondaryText)
        }
        Pill("HUB SELECTED", BlueSoft, Ink)
    }

        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile("◎", BlueSoft, Ink, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Selected hub location", fontWeight = FontWeight.Bold)
                    Text("${location.city} · ${location.latitude}, ${location.longitude}", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.size(9.dp).background(Success, CircleShape))
            }
        }

        ParkingMap()

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nearby parking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap a lot to view spaces", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
        }

        lots.forEachIndexed { index, lot ->
            ParkingLotCard(
                lot = lot,
                recommended = index == 0,
                expanded = expandedLotId == lot.id,
                onToggle = { expandedLotId = if (expandedLotId == lot.id) null else lot.id },
                onNavigate = onNavigate,
            )
        }
    }
}

private fun demoParkingLots(): List<ParkingLotUi> = listOf(
    ParkingLotUi(
        id = "south-garage",
        name = "South Garage",
        distanceMinutes = 2,
        walkMinutes = 4,
        spaces = listOf(
            ParkingSpaceUi("S-201", true), ParkingSpaceUi("S-202", false),
            ParkingSpaceUi("S-203", true), ParkingSpaceUi("S-204", true, "EV"),
            ParkingSpaceUi("S-205", false), ParkingSpaceUi("S-206", true),
            ParkingSpaceUi("S-207", true, "Accessible"), ParkingSpaceUi("S-208", false),
        ),
    ),
    ParkingLotUi(
        id = "west-lot",
        name = "West Lot",
        distanceMinutes = 4,
        walkMinutes = 9,
        spaces = listOf(
            ParkingSpaceUi("W-101", true), ParkingSpaceUi("W-102", true),
            ParkingSpaceUi("W-103", false), ParkingSpaceUi("W-104", true),
            ParkingSpaceUi("W-105", true), ParkingSpaceUi("W-106", true, "EV"),
        ),
    ),
    ParkingLotUi(
        id = "north-garage",
        name = "North Garage",
        distanceMinutes = 5,
        walkMinutes = 6,
        spaces = listOf(
            ParkingSpaceUi("N-301", false), ParkingSpaceUi("N-302", true),
            ParkingSpaceUi("N-303", false), ParkingSpaceUi("N-304", true),
        ),
    ),
)

@Composable
private fun ParkingLotCard(
    lot: ParkingLotUi,
    recommended: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onNavigate: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (expanded) BrandRed else BorderColor),
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile("P", if (recommended) BrandRed else CanvasColor, if (recommended) Color.White else Ink, 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lot.name, fontWeight = FontWeight.Bold)
                        if (recommended) Pill("BEST", SuccessSoft, Success)
                    }
                    Text("${lot.distanceMinutes} min drive · ${lot.walkMinutes} min walk", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${lot.openSpaces}", color = Success, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("open", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                }
                Text(if (expanded) "⌃" else "⌄", color = SecondaryText, fontWeight = FontWeight.Bold)
            }

            if (expanded) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Available spaces", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpaceLegend(SuccessSoft, "Open")
                            SpaceLegend(CanvasColor, "Occupied")
                        }
                    }
                    lot.spaces.chunked(2).forEach { rowSpaces ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowSpaces.forEach { space -> ParkingSpaceTile(space, Modifier.weight(1f)) }
                            if (rowSpaces.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    PrimaryButton("Navigate to ${lot.name}", onNavigate)
                }
            }
        }
    }
}

@Composable
private fun ParkingSpaceTile(space: ParkingSpaceUi, modifier: Modifier) {
    val background = if (space.isOpen) SuccessSoft else CanvasColor
    val foreground = if (space.isOpen) Success else SecondaryText
    Row(
        modifier.background(background, RoundedCornerShape(12.dp)).padding(11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(space.id, color = foreground, fontWeight = FontWeight.Bold)
            Text(space.type, color = foreground, style = MaterialTheme.typography.labelSmall)
        }
        Text(if (space.isOpen) "OPEN" else "FULL", color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpaceLegend(color: Color, label: String) = Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(8.dp).background(color, CircleShape).border(1.dp, BorderColor, CircleShape))
    Text(label, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun NavigationScreen() = Page {
    Text("Building navigation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text("Building A · First floor", color = SecondaryText)
    FloorPlan()

    Surface(color = Ink, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile("→", BrandRed, Color.White, 48.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Turn right at the red wall", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Innovation Room is the second door.", color = Color(0xFFD2D4D9), style = MaterialTheme.typography.bodySmall)
                Text("About 1 minute away", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    LightButton("Choose another destination") {}
}

@Composable
private fun JakeScreen(onParking: () -> Unit, onNavigate: () -> Unit) {
    var answer by remember { mutableStateOf("South Garage is your best option. About 18 spaces are available, with a 4-minute walk to Building A.") }

    Page {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile("J", BrandRed, Color.White, 48.dp)
            Column {
                Text("Ask Jake", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Dallas Hub logistics assistant", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
        }

        Panel {
            MessageBubble(false) {
                Text("Hi Carter — how can I help?", fontWeight = FontWeight.Bold)
                Text("I can help with parking, rooms, food, and campus amenities.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
            MessageBubble(true) { Text("Where should I park?", color = Color.White) }
            MessageBubble(false) {
                Text(answer, fontWeight = FontWeight.SemiBold)
                LightButton("Open parking  →", compact = true, onClick = onParking)
            }
            Text("Suggestions", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Suggestion("Find a room", BlueSoft) {
                    answer = "Choose a destination and I’ll open the first-floor route for you."
                }
                Suggestion("Lunch today", AmberSoft) {
                    answer = "Café 1 has tacos, salads, and sandwiches today."
                }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ask about this hub…", color = SecondaryText, modifier = Modifier.padding(start = 6.dp).weight(1f))
                IconTile("↑", BrandRed, Color.White, 36.dp, onNavigate)
            }
        }
    }
}

@Composable
private fun ParkingMap() {
    Box(Modifier.fillMaxWidth().height(326.dp).background(Color(0xFFE4E9E5), RoundedCornerShape(22.dp))) {
        Box(Modifier.width(48.dp).height(360.dp).offset(x = 132.dp, y = (-16).dp).background(Color.White))
        Box(Modifier.fillMaxWidth().height(38.dp).offset(y = 158.dp).background(Color.White))
        MapBlock("WEST LOT", 28.dp, 52.dp, 112.dp, 64.dp, Color.White, Ink)
        MapBlock("BUILDING A", 202.dp, 54.dp, 118.dp, 88.dp, Ink, Color.White)
        MapBlock("SOUTH GARAGE", 174.dp, 222.dp, 150.dp, 76.dp, BrandRed, Color.White)
        Box(Modifier.size(22.dp).offset(x = 236.dp, y = 190.dp).background(BrandRed, CircleShape).border(4.dp, Color.White, CircleShape))
    }
}

@Composable
private fun FloorPlan() {
    Box(Modifier.fillMaxWidth().height(390.dp).background(Color.White, RoundedCornerShape(22.dp)).border(1.dp, BorderColor, RoundedCornerShape(22.dp))) {
        Room("Open work", 22.dp, 28.dp, 146.dp, 154.dp, CanvasColor)
        Room("Café 1", 220.dp, 28.dp, 108.dp, 86.dp, AmberSoft)
        Room("Lobby", 22.dp, 240.dp, 110.dp, 112.dp, BlueSoft)
        Room("Innovation", 220.dp, 258.dp, 108.dp, 96.dp, SuccessSoft)
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * .55f, size.height * .88f)
                lineTo(size.width * .55f, size.height * .47f)
                lineTo(size.width * .75f, size.height * .47f)
            }
            drawPath(path, BrandRed, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Color.White, 8.dp.toPx(), Offset(size.width * .55f, size.height * .88f), style = Stroke(4.dp.toPx()))
            drawCircle(BrandRed, 9.dp.toPx(), Offset(size.width * .75f, size.height * .47f))
        }
    }
}

@Composable
private fun AppNavigation(active: Screen, onSelect: (Screen) -> Unit) {
    Surface(color = Color.White) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            Screen.entries.forEach { item ->
                val selected = item == active
                Column(
                    Modifier.width(76.dp).background(if (selected) BrandRed else Color.Transparent, RoundedCornerShape(14.dp)).clickable { onSelect(item) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(item.symbol, color = if (selected) Color.White else SecondaryText, fontWeight = FontWeight.Bold)
                    Text(item.label, color = if (selected) Color.White else SecondaryText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun LocationLabel() = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Box(Modifier.size(10.dp).background(Success, CircleShape))
    Text("Dallas Hub · Location confirmed", color = Success, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun QuickAction(symbol: String, title: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(symbol, BlueSoft, Ink, 36.dp)
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeatureRow(symbol: String, iconColor: Color, title: String, detail: String, action: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(symbol, iconColor, Ink, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
            Text(action, color = BrandRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IconTile(symbol: String, background: Color, foreground: Color, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(size).background(background, RoundedCornerShape(size / 3)).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = foreground, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Pill(label: String, background: Color, foreground: Color) {
    Box(Modifier.background(background, RoundedCornerShape(100.dp)).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandRed)) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LightButton(label: String, compact: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = if (compact) BrandRed else Ink),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (compact) 6.dp else 11.dp),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Column(modifier.background(CanvasColor, RoundedCornerShape(10.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MapBlock(label: String, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, background: Color, foreground: Color) {
    Box(Modifier.width(width).height(height).offset(x, y).background(background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Room(label: String, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, color: Color) {
    Box(Modifier.width(width).height(height).offset(x, y).background(color, RoundedCornerShape(12.dp)).border(1.dp, BorderColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MessageBubble(outgoing: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(if (outgoing) .82f else .92f).background(if (outgoing) BrandRed else CanvasColor, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun Suggestion(label: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.background(color, RoundedCornerShape(100.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}
