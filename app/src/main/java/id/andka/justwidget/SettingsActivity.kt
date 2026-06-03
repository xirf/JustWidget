package id.andka.justwidget

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import id.andka.justwidget.data.DayForecast
import id.andka.justwidget.data.LocationProvider
import id.andka.justwidget.data.ThemeStore
import id.andka.justwidget.data.WeatherData
import id.andka.justwidget.widget.WeatherWidget
import id.andka.justwidget.widget.WeatherWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ALIAS = "id.andka.justwidget.LauncherAlias"

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Settings / home screen. Also the entry point from the widget tap and
 * (when visible) from the launcher via LauncherAlias.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val isDark by ThemeStore.isDarkFlow(this).collectAsState(initial = true)

            val scheme = if (isDark) {
                darkColorScheme(
                    background    = Color.Black,
                    surface       = Color(0xFF0E0E0E),
                    surfaceVariant = Color(0xFF1C1C1C),
                    primary       = Color(0xFF60A5FA),
                    onBackground  = Color.White,
                    onSurface     = Color.White,
                    onSurfaceVariant = Color(0xFFCBD5E1),
                )
            } else {
                lightColorScheme(
                    background    = Color(0xFFF0F4F8),
                    surface       = Color.White,
                    surfaceVariant = Color(0xFFE2E8F0),
                    primary       = Color(0xFF2563EB),
                    onBackground  = Color(0xFF0F172A),
                    onSurface     = Color(0xFF1E293B),
                    onSurfaceVariant = Color(0xFF475569),
                )
            }

            MaterialTheme(colorScheme = scheme) {
                // Keep system bar icons in sync with theme
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        WindowInsetsControllerCompat(window, window.decorView).apply {
                            isAppearanceLightStatusBars     = !isDark
                            isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = scheme.background,
                ) {
                    AppScreen()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App Main Screen with Bottom Navigation
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppScreen() {
    var currentTab by remember { mutableStateOf("weather") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "weather",
                    onClick = { currentTab = "weather" },
                    icon = { Text("🌤️", fontSize = 20.sp) },
                    label = { Text("Weather", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Text("⚙️", fontSize = 20.sp) },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentTab == "weather") {
                WeatherPage(onSearchClick = { currentTab = "settings" })
            } else {
                SettingsPage()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weather Homepage (Rain App Style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeatherPage(onSearchClick: () -> Unit) {
    val context = LocalContext.current
    val weather by WeatherDataStore.flow(context).collectAsState(initial = null)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Top Bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌐", fontSize = 22.sp, modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = weather?.placeName ?: "GPS Location",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "🔍",
                fontSize = 22.sp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onSearchClick() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Main Card (Hero) ─────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val w = weather
                    Text(
                        text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = emojiToCondition(w?.currentEmoji ?: "⛅"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Wind · ${w?.windCompass ?: "--"} ${w?.windSpeedKmh ?: "--"} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "${w?.currentTemp ?: "--"}°C",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 64.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    val todayMax = w?.days?.getOrNull(w.todayIndex)?.maxTemp ?: "--"
                    val todayMin = w?.days?.getOrNull(w.todayIndex)?.minTemp ?: "--"
                    Text(
                        text = "$todayMin°C / $todayMax°C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Big weather emoji
                Text(
                    text = weather?.currentEmoji ?: "⛅",
                    fontSize = 100.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 7-Day Capsule Forecast ───────────────────────────────────────
        Text(
            text = "7-Day Forecast",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        if (weather != null) {
            ForecastStrip(data = weather!!)
        } else {
            Text(
                text = "Tap refresh to load weather data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Wind & Updated details card ──────────────────────────────────
        if (weather != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "💨 Wind",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${weather!!.windCompass} · ${weather!!.windSpeedKmh} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Divider
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏰ Updated",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(4.dp))
                        val updatedText = remember(weather!!.updatedAtMillis) {
                            val diff = System.currentTimeMillis() - weather!!.updatedAtMillis
                            when {
                                diff < 60_000L      -> "just now"
                                diff < 3_600_000L   -> "${diff / 60_000}m ago"
                                else                -> "${diff / 3_600_000}h ago"
                            }
                        }
                        Text(
                            text = updatedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick  = { WeatherWorker.refreshNow(context) },
        ) {
            Text("🔄  Refresh Weather")
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Forecast Pill/Capsule strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ForecastStrip(data: WeatherData) {
    val today = data.todayIndex.coerceIn(0, data.days.size - 1)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding        = PaddingValues(horizontal = 0.dp),
    ) {
        itemsIndexed(data.days) { i, day ->
            DayCard(day = day, isToday = i == today, isPast = i < today)
        }
    }
}

@Composable
private fun DayCard(day: DayForecast, isToday: Boolean, isPast: Boolean) {
    val textAlpha = if (isPast) 0.5f else 1f
    val borderMod = if (isToday)
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
    else Modifier

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = if (isToday)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isPast) 0.4f else 0.8f),
        modifier = Modifier.width(72.dp).then(borderMod),
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Text(
                text  = "${day.dayOfMonth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha * 0.6f),
            )
            Text(
                text       = day.weekdayShort,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color      = if (isToday)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
            )
            Spacer(Modifier.height(12.dp))
            Text(day.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text       = "${day.maxTemp}°",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Settings Tab Page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDark by ThemeStore.isDarkFlow(context).collectAsState(initial = true)
    val accentHex by ThemeStore.accentColorFlow(context).collectAsState(initial = "#C084FC")
    val textDark by ThemeStore.widgetTextDarkFlow(context).collectAsState(initial = false)
    val bgStyle by ThemeStore.widgetBgStyleFlow(context).collectAsState(initial = "dark")
    val customLocJson by ThemeStore.customLocationFlow(context).collectAsState(initial = "")

    var iconHidden by remember { mutableStateOf(isIconHidden(context)) }
    var hasLocation by remember { mutableStateOf(LocationProvider.hasPermission(context)) }
    var locSearchText by remember { mutableStateOf("") }
    var locErrorMsg by remember { mutableStateOf<String?>(null) }
    var customHexText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocation = LocationProvider.hasPermission(context)
        WeatherWorker.refreshNow(context)
    }

    val resolvedCustomLocName = remember(customLocJson) {
        if (customLocJson.isEmpty()) null
        else {
            try {
                org.json.JSONObject(customLocJson).getString("name")
            } catch (e: Exception) {
                null
            }
        }
    }

    val presets = listOf(
        "#C084FC" to "Purple",
        "#E848A6" to "Pink",
        "#3B82F6" to "Blue",
        "#10B981" to "Green",
        "#F97316" to "Orange"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(20.dp))

        // ── Card 1: General Preferences ──────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "General Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                // Theme switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Color Theme",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isDark) "Pure dark (AMOLED mode)" else "Light mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { dark ->
                            scope.launch { ThemeStore.setDark(context, dark) }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Hide from App Drawer switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Launcher Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (iconHidden) "Icon is hidden from app drawer" else "Icon is visible in app drawer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = !iconHidden,
                        onCheckedChange = { visible ->
                            val hide = !visible
                            iconHidden = hide
                            setIconHidden(context, hide)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Card 2: Location Settings ────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📍 Location Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                if (resolvedCustomLocName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Current: $resolvedCustomLocName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ThemeStore.clearCustomLocation(context)
                                    WeatherWorker.refreshNow(context)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Use GPS", fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text(
                        text = "Current: GPS Location (Automatic)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = locSearchText,
                        onValueChange = {
                            locSearchText = it
                            locErrorMsg = null
                        },
                        placeholder = { Text("Search city, e.g. Tokyo", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            if (locSearchText.isNotBlank()) {
                                scope.launch {
                                    val resolved = LocationProvider.resolveLocationName(context, locSearchText)
                                    if (resolved != null) {
                                        ThemeStore.setCustomLocation(context, resolved.name, resolved.lat, resolved.lon)
                                        WeatherWorker.refreshNow(context)
                                        locSearchText = ""
                                        locErrorMsg = null
                                    } else {
                                        locErrorMsg = "City not found"
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Search", fontSize = 12.sp)
                    }
                }

                locErrorMsg?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                if (!hasLocation && resolvedCustomLocName == null) {
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick  = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        },
                    ) {
                        Text("📍  Grant GPS Permission")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Card 3: Widget Customization ─────────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎨 Widget Customize Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(14.dp))

                // Accent Picker
                Text(
                    text = "Highlight Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { (hex, name) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = accentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(36.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    scope.launch {
                                        ThemeStore.setAccentColor(context, hex)
                                        WeatherWidget.updateSettings(context, hex, textDark, bgStyle)
                                    }
                                }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Custom hex input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customHexText,
                        onValueChange = { customHexText = it },
                        placeholder = { Text("Or custom hex, e.g. #C084FC", fontSize = 13.sp) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            val cleaned = customHexText.trim()
                            if (cleaned.startsWith("#") && (cleaned.length == 7 || cleaned.length == 9)) {
                                scope.launch {
                                    ThemeStore.setAccentColor(context, cleaned)
                                    WeatherWidget.updateSettings(context, cleaned, textDark, bgStyle)
                                    customHexText = ""
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Text dark mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Widget Text Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (textDark) "Dark text (for light wallpaper)" else "Light text (for dark wallpaper)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = textDark,
                        onCheckedChange = { dark ->
                            scope.launch {
                                ThemeStore.setWidgetTextDark(context, dark)
                                WeatherWidget.updateSettings(context, accentHex, dark, bgStyle)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Widget background style
                Text(
                    text = "Widget Background Style",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val styles = listOf("dark" to "Glass Dark", "light" to "Glass Light", "transparent" to "Transparent")
                    styles.forEach { (styleKey, label) ->
                        val isSelected = bgStyle == styleKey
                        Surface(
                            onClick = {
                                scope.launch {
                                    ThemeStore.setWidgetBgStyle(context, styleKey)
                                    WeatherWidget.updateSettings(context, accentHex, textDark, styleKey)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun isIconHidden(context: Context): Boolean {
    val alias = ComponentName(context.packageName, ALIAS)
    return context.packageManager.getComponentEnabledSetting(alias) ==
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

private fun setIconHidden(context: Context, hidden: Boolean) {
    val alias    = ComponentName(context.packageName, ALIAS)
    val newState = if (hidden)
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    else
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    context.packageManager.setComponentEnabledSetting(alias, newState, PackageManager.DONT_KILL_APP)
}

private fun emojiToCondition(emoji: String): String = when (emoji) {
    "🌞"  -> "Clear Sky"
    "🌤️" -> "Mainly Clear"
    "⛅"  -> "Partly Cloudy"
    "☁️"  -> "Overcast"
    "🌫️" -> "Foggy"
    "🌦️" -> "Drizzle"
    "🌧️" -> "Rainy"
    "🌨️" -> "Snowy"
    "⛈️"  -> "Thunderstorm"
    "🌙"  -> "Clear Night"
    else  -> "Unknown"
}
