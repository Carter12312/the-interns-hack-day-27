package com.statefarm.hubcompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
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

private data class ParkingLotUi(
    val id: String,
    val name: String,
    val analysisPath: String,
    val distanceMinutes: Int,
    val walkMinutes: Int,
)

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
                        Screen.Parking -> ParkingScreen(
                            location = selectedHub,
                            hubs = hubs,
                            onHubSelected = { selectedHub = it },
                            onNavigate = { screen = Screen.Navigate },
                        )
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
    val isCorporateHq = location.hubName == "Bloomington Hub"
    val featuredLot = if (isCorporateHq) "Corporate HQ Lot A" else "South Garage"
    val parkingDestination = if (isCorporateHq) "Corporate Headquarters" else "Building A"
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
                        Text("Best option for $parkingDestination", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Pill("LIVE AI", SuccessSoft, Success)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(featuredLot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Live availability · Tap to analyze", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
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
    UserLocation("Bloomington Hub", "Bloomington, IL", 40.4842, -88.9937),
    UserLocation("Dallas Hub", "Dallas, TX", 32.7767, -96.7970),
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
    hubs: List<UserLocation>,
    onHubSelected: (UserLocation) -> Unit,
    onNavigate: () -> Unit,
) {
    var expandedLotId by remember(location.hubName) {
        mutableStateOf<String?>(if (location.hubName == "Bloomington Hub") null else "south-garage")
    }
    var selectedLotId by remember(location.hubName) { mutableStateOf<String?>(null) }
    var lotAvailability by remember(location.hubName) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val lots = remember(location.hubName) { parkingLotsFor(location) }
    val displayedLots = remember(lots, selectedLotId) {
        selectedLotId?.let { selected ->
            lots.sortedBy { if (it.id == selected) 0 else 1 }
        } ?: lots
    }
    LaunchedEffect(location.hubName) {
        lotAvailability = if (location.hubName == "Bloomington Hub") {
            runCatching { ParkingRepository.corporateHqSummary() }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }
    }

    Page {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("AI Parking", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Nearby lots at ${location.hubName}", color = SecondaryText)
        }
        Pill("HUB SELECTED", BlueSoft, Ink)
    }

        HubSelector(location, hubs) { hub ->
            selectedLotId = null
            expandedLotId = null
            onHubSelected(hub)
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

        ParkingMap(
            location = location,
            selectedLotId = selectedLotId,
            availability = lotAvailability,
            onLotSelected = { lotId ->
                selectedLotId = lotId
                expandedLotId = lotId
            },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nearby parking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap a lot to view spaces", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
        }

        displayedLots.forEachIndexed { index, lot ->
            ParkingLotCard(
                lot = lot,
                recommended = index == 0,
                expanded = expandedLotId == lot.id,
                knownOpenCount = lot.id.lastOrNull()?.uppercase()?.let(lotAvailability::get),
                onToggle = { expandedLotId = if (expandedLotId == lot.id) null else lot.id },
                onNavigate = onNavigate,
            )
        }
    }
}

private fun parkingLotsFor(location: UserLocation): List<ParkingLotUi> =
    if (location.hubName == "Bloomington Hub") corporateHqParkingLots() else demoParkingLots()

private fun corporateHqParkingLots(): List<ParkingLotUi> = ('A'..'F').map { letter ->
    ParkingLotUi(
        id = "corporate-hq-${letter.lowercaseChar()}",
        name = "Corporate HQ Lot $letter",
        analysisPath = "locations/corporate-hq/lots/$letter/analysis",
        distanceMinutes = when (letter) {
            'A', 'B' -> 2
            'C', 'D' -> 3
            else -> 5
        },
        walkMinutes = when (letter) {
            'A', 'B' -> 4
            'C', 'D' -> 3
            else -> 7
        },
    )
}

private fun demoParkingLots(): List<ParkingLotUi> = listOf(
    ParkingLotUi(
        id = "south-garage",
        name = "South Garage",
        analysisPath = "samples/A/analysis",
        distanceMinutes = 2,
        walkMinutes = 4,
    ),
    ParkingLotUi(
        id = "west-lot",
        name = "West Lot",
        analysisPath = "samples/B/analysis",
        distanceMinutes = 4,
        walkMinutes = 9,
    ),
    ParkingLotUi(
        id = "north-garage",
        name = "North Garage",
        analysisPath = "samples/C/analysis",
        distanceMinutes = 5,
        walkMinutes = 6,
    ),
)

@Composable
private fun ParkingLotCard(
    lot: ParkingLotUi,
    recommended: Boolean,
    expanded: Boolean,
    knownOpenCount: Int?,
    onToggle: () -> Unit,
    onNavigate: () -> Unit,
) {
    var analysisState by remember(lot.id) { mutableStateOf<ParkingAnalysisState>(ParkingAnalysisState.Idle) }
    var retryCount by remember(lot.id) { mutableStateOf(0) }
    LaunchedEffect(expanded, lot.analysisPath, retryCount) {
        if (expanded && analysisState !is ParkingAnalysisState.Ready) {
            analysisState = ParkingAnalysisState.Loading
            analysisState = try {
                ParkingAnalysisState.Ready(ParkingRepository.analyze(lot.analysisPath))
            } catch (throwable: Throwable) {
                ParkingAnalysisState.Error(throwable.message ?: "Unable to load parking availability")
            }
        }
    }
    val openCount = (analysisState as? ParkingAnalysisState.Ready)?.analysis?.freeCount ?: knownOpenCount
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
                    Text(openCount?.toString() ?: "—", color = Success, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (openCount == null) "tap to scan" else "open", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                }
                Text(if (expanded) "⌃" else "⌄", color = SecondaryText, fontWeight = FontWeight.Bold)
            }

            if (expanded) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (val state = analysisState) {
                        ParkingAnalysisState.Idle, ParkingAnalysisState.Loading -> ParkingLoadingPanel()
                        is ParkingAnalysisState.Error -> ParkingErrorPanel(state.message) {
                            retryCount += 1
                        }
                        is ParkingAnalysisState.Ready -> ParkingLotAnalysisView(state.analysis)
                    }
                    PrimaryButton("Navigate to ${lot.name}", onNavigate)
                }
            }
        }
    }
}

@Composable
private fun ParkingLotAnalysisView(analysis: ParkingAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Live lot analysis", fontWeight = FontWeight.Bold)
                Text("${analysis.freeCount} of ${analysis.totalSpots} spaces available", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            }
            Pill("${analysis.freeCount} OPEN", SuccessSoft, Success)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SpaceLegend(Success, "Available")
            SpaceLegend(Color(0xFFB8BDC7), "Occupied")
        }
        Surface(color = Color(0xFF33383F), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Canvas(Modifier.fillMaxWidth().height(250.dp).padding(12.dp)) {
                val rows = analysis.spots.groupBy { it.row }.toSortedMap()
                val maxColumns = rows.values.maxOfOrNull { it.size } ?: 1
                val horizontalPadding = 8.dp.toPx()
                val spaceGap = 2.dp.toPx()
                val spaceWidth = (
                    size.width - horizontalPadding * 2f - spaceGap * (maxColumns - 1)
                ) / maxColumns
                val rowBandHeight = size.height / rows.size.coerceAtLeast(1)
                val spaceHeight = rowBandHeight * 0.48f
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())

                rows.entries.forEachIndexed { rowIndex, (_, rowSpots) ->
                    val sortedSpots = rowSpots.sortedBy { it.number }
                    val rowWidth = sortedSpots.size * spaceWidth +
                        (sortedSpots.size - 1).coerceAtLeast(0) * spaceGap
                    val rowStartX = (size.width - rowWidth) / 2f
                    val rowTop = rowIndex * rowBandHeight + (rowBandHeight - spaceHeight) / 2f

                    sortedSpots.forEachIndexed { columnIndex, spot ->
                        val topLeft = Offset(
                            x = rowStartX + columnIndex * (spaceWidth + spaceGap),
                            y = rowTop,
                        )
                        val spaceSize = androidx.compose.ui.geometry.Size(spaceWidth, spaceHeight)
                        val color = if (spot.available) Color(0xFF20A464) else Color(0xFF858C96)
                        drawRoundRect(color, topLeft, spaceSize, cornerRadius)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = topLeft,
                            size = spaceSize,
                            cornerRadius = cornerRadius,
                            style = Stroke(1.dp.toPx()),
                        )
                    }

                    if (rowIndex < rows.size - 1) {
                        val aisleY = (rowIndex + 1) * rowBandHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(horizontalPadding, aisleY),
                            end = Offset(size.width - horizontalPadding, aisleY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(analysis.freeCount.toString(), "Available", Modifier.weight(1f))
            Metric(analysis.occupiedCount.toString(), "Occupied", Modifier.weight(1f))
            Metric(analysis.totalSpots.toString(), "Total", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ParkingLoadingPanel() {
    Surface(color = BlueSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Analyzing parking lot…", fontWeight = FontWeight.Bold)
            Text("The local AI model is checking each marked space.", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ParkingErrorPanel(message: String, onRetry: () -> Unit) {
    Surface(color = Color(0xFFFFE7E5), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Parking service unavailable", color = BrandRed, fontWeight = FontWeight.Bold)
            Text(message, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
            LightButton("Try again", compact = true, onClick = onRetry)
        }
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
private fun ParkingMap(
    location: UserLocation,
    selectedLotId: String?,
    availability: Map<String, Int>,
    onLotSelected: (String) -> Unit,
) {
    if (location.hubName == "Bloomington Hub") {
        CorporateHqMap(selectedLotId, availability, onLotSelected)
        return
    }
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
private fun CorporateHqMap(
    selectedLotId: String?,
    availability: Map<String, Int>,
    onLotSelected: (String) -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(470.dp).background(Color(0xFFDFF3E5), RoundedCornerShape(22.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val road = Color(0xFFBCC7D1)
            val roadEdge = Color(0xFFF7F8F9)
            val lot = Color(0xFFCBEBD6)
            val lane = Color(0xFFAFBCC8)

            fun roadHorizontal(y: Float, height: Float) {
                drawRect(roadEdge, Offset(0f, y - 2.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, height + 4.dp.toPx()))
                drawRect(road, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, height))
            }
            fun roadVertical(x: Float, width: Float, top: Float, bottom: Float) {
                drawRect(roadEdge, Offset(x - 2.dp.toPx(), top), androidx.compose.ui.geometry.Size(width + 4.dp.toPx(), bottom - top))
                drawRect(road, Offset(x, top), androidx.compose.ui.geometry.Size(width, bottom - top))
            }
            fun lotPath(points: List<Pair<Float, Float>>): Path = Path().apply {
                points.forEachIndexed { index, point ->
                    val x = size.width * point.first
                    val y = size.height * point.second
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            fun drawLot(path: Path) {
                drawPath(path, lot)
                drawPath(path, Color(0xFF9FB5A7), style = Stroke(1.dp.toPx()))
            }
            fun parkingRow(left: Float, right: Float, y: Float, spaces: Int) {
                val startX = size.width * left
                val endX = size.width * right
                val top = size.height * y
                val depth = 10.dp.toPx()
                drawLine(lane, Offset(startX, top), Offset(endX, top), 2.dp.toPx())
                repeat(spaces + 1) { index ->
                    val x = startX + (endX - startX) * index / spaces
                    drawLine(lane.copy(alpha = .75f), Offset(x, top - depth), Offset(x, top + depth), 1.dp.toPx())
                }
            }

            roadHorizontal(size.height * .205f, 13.dp.toPx())
            roadHorizontal(size.height * .585f, 13.dp.toPx())
            roadHorizontal(size.height * .745f, 14.dp.toPx())
            roadVertical(size.width * .485f, 14.dp.toPx(), 0f, size.height)
            roadVertical(size.width * .03f, 10.dp.toPx(), 0f, size.height * .74f)
            roadVertical(size.width * .955f, 10.dp.toPx(), 0f, size.height)

            // Every lot follows its own campus boundary instead of sharing a box.
            drawLot(lotPath(listOf(
                .045f to .02f, .46f to .02f, .46f to .07f, .42f to .07f,
                .42f to .19f, .30f to .19f, .30f to .17f, .045f to .17f,
            )))
            parkingRow(.065f, .28f, .065f, 7)
            parkingRow(.065f, .40f, .115f, 10)
            parkingRow(.065f, .28f, .158f, 7)

            drawLot(lotPath(listOf(
                .535f to .02f, .91f to .02f, .95f to .055f, .95f to .19f,
                .535f to .19f, .535f to .145f, .58f to .145f, .58f to .07f,
                .535f to .07f,
            )))
            parkingRow(.60f, .91f, .06f, 8)
            parkingRow(.55f, .92f, .115f, 10)
            parkingRow(.55f, .92f, .17f, 10)

            drawLot(lotPath(listOf(
                .04f to .605f, .39f to .605f, .39f to .72f, .365f to .74f,
                .04f to .735f,
            )))
            parkingRow(.065f, .35f, .64f, 8)
            parkingRow(.065f, .35f, .695f, 8)

            drawLot(lotPath(listOf(
                .54f to .605f, .95f to .605f, .95f to .695f, .91f to .735f,
                .54f to .735f, .565f to .69f,
            )))
            parkingRow(.58f, .91f, .64f, 9)
            parkingRow(.58f, .90f, .695f, 9)

            drawLot(lotPath(listOf(
                .03f to .775f, .38f to .775f, .38f to .965f, .03f to .965f,
                .03f to .91f, .055f to .89f, .055f to .82f,
            )))
            parkingRow(.075f, .34f, .82f, 7)
            parkingRow(.075f, .34f, .875f, 7)
            parkingRow(.075f, .34f, .935f, 7)

            drawLot(lotPath(listOf(
                .54f to .78f, .94f to .78f, .965f to .82f, .965f to .965f,
                .72f to .965f, .72f to .925f, .62f to .925f, .62f to .965f,
                .53f to .965f, .53f to .88f, .57f to .85f,
            )))
            parkingRow(.58f, .91f, .815f, 9)
            parkingRow(.59f, .92f, .865f, 9)
            parkingRow(.66f, .92f, .915f, 7)
            parkingRow(.76f, .92f, .95f, 4)

            val building = Path().apply {
                moveTo(size.width * .13f, size.height * .25f)
                lineTo(size.width * .42f, size.height * .25f)
                lineTo(size.width * .42f, size.height * .29f)
                lineTo(size.width * .58f, size.height * .29f)
                lineTo(size.width * .58f, size.height * .25f)
                lineTo(size.width * .86f, size.height * .25f)
                lineTo(size.width * .86f, size.height * .54f)
                lineTo(size.width * .62f, size.height * .54f)
                lineTo(size.width * .62f, size.height * .49f)
                lineTo(size.width * .38f, size.height * .49f)
                lineTo(size.width * .38f, size.height * .54f)
                lineTo(size.width * .13f, size.height * .54f)
                close()
            }
            drawPath(building, Color(0xFFE3E5E9))
            drawPath(building, Color(0xFFBFC4CB), style = Stroke(1.dp.toPx()))
        }

        CampusLot("A", availability["A"], maxWidth * .07f, 18.dp, maxWidth * .34f, 64.dp, selectedLotId == "corporate-hq-a") { onLotSelected("corporate-hq-a") }
        CampusLot("B", availability["B"], maxWidth * .59f, 18.dp, maxWidth * .30f, 64.dp, selectedLotId == "corporate-hq-b") { onLotSelected("corporate-hq-b") }
        CampusLot("C", availability["C"], maxWidth * .08f, 276.dp, maxWidth * .28f, 64.dp, selectedLotId == "corporate-hq-c") { onLotSelected("corporate-hq-c") }
        CampusLot("D", availability["D"], maxWidth * .60f, 276.dp, maxWidth * .28f, 64.dp, selectedLotId == "corporate-hq-d") { onLotSelected("corporate-hq-d") }
        CampusLot("E", availability["E"], maxWidth * .07f, 374.dp, maxWidth * .28f, 64.dp, selectedLotId == "corporate-hq-e") { onLotSelected("corporate-hq-e") }
        CampusLot("F", availability["F"], maxWidth * .61f, 374.dp, maxWidth * .28f, 64.dp, selectedLotId == "corporate-hq-f") { onLotSelected("corporate-hq-f") }

        Column(
            Modifier.width(maxWidth * .62f).offset(x = maxWidth * .19f, y = 158.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("STATE FARM", color = BrandRed, fontWeight = FontWeight.Bold)
            Text("CORPORATE HEADQUARTERS", color = Ink, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("Bloomington, Illinois", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CampusLot(
    letter: String,
    openCount: Int?,
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.width(width).height(height).offset(x, y).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.background(
                if (selected) BrandRed else Color.White.copy(alpha = .92f),
                RoundedCornerShape(10.dp),
            ).border(
                1.dp,
                if (selected) Color.White else Color(0xFFB9C8BE),
                RoundedCornerShape(10.dp),
            ).padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(letter, color = if (selected) Color.White else BrandRed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                openCount?.let { "$it OPEN" } ?: "LOT",
                color = if (selected) Color.White else Success,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
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
