package com.ireddragonicy.qiblaku

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ireddragonicy.qiblaku.data.CampusPreset
import com.ireddragonicy.qiblaku.data.PosePreset
import com.ireddragonicy.qiblaku.data.SessionRecorder
import com.ireddragonicy.qiblaku.location.LocationTracker
import com.ireddragonicy.qiblaku.math.QiblaCalculator
import com.ireddragonicy.qiblaku.sensor.SensorRepository
import com.ireddragonicy.qiblaku.ui.components.AppIcons
import com.ireddragonicy.qiblaku.ui.screens.LogsScreen
import com.ireddragonicy.qiblaku.ui.screens.MeasureScreen
import com.ireddragonicy.qiblaku.ui.screens.TelemetryScreen
import com.ireddragonicy.qiblaku.ui.theme.BgDark
import com.ireddragonicy.qiblaku.ui.theme.CardDark
import com.ireddragonicy.qiblaku.ui.theme.Coral
import com.ireddragonicy.qiblaku.ui.theme.Emerald
import com.ireddragonicy.qiblaku.ui.theme.EmeraldBright
import com.ireddragonicy.qiblaku.ui.theme.QiblaKuTheme
import com.ireddragonicy.qiblaku.ui.theme.TextMuted
import com.ireddragonicy.qiblaku.ui.theme.TextPrimary
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var locationTracker: LocationTracker
    private lateinit var sensorRepository: SensorRepository
    private lateinit var sessionRecorder: SessionRecorder

    private var hasLocationPermission by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            locationTracker.start()
        } else {
            Toast.makeText(
                this,
                "Izin lokasi diperlukan untuk GPS & Deklinasi Magnetik",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        locationTracker = LocationTracker(this)
        sensorRepository = SensorRepository(this)
        sessionRecorder = SessionRecorder(this)

        sensorRepository.onRawSampleListener = { snap ->
            sessionRecorder.onSensorSample(snap)
        }

        hasLocationPermission = checkLocationPermission()
        if (!hasLocationPermission) {
            requestLocationPermission()
        }

        setContent {
            QiblaKuTheme {
                QiblaKuApp(
                    locationTracker = locationTracker,
                    sensorRepository = sensorRepository,
                    sessionRecorder = sessionRecorder,
                    hasLocationPermission = hasLocationPermission,
                    onRequestLocationPermission = ::requestLocationPermission,
                    onShareFile = ::shareCsvFile
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorRepository.start()
        hasLocationPermission = checkLocationPermission()
        if (hasLocationPermission) {
            locationTracker.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (sessionRecorder.recordingState.value.isRecording) {
            sessionRecorder.stopRecording()
        }
        sensorRepository.stop()
        locationTracker.stop()
    }

    private fun checkLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun shareCsvFile(file: File, chooserTitle: String) {
        if (!file.exists()) {
            Toast.makeText(this, "File tidak ditemukan: ${file.name}", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, chooserTitle))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QiblaKuApp(
    locationTracker: LocationTracker,
    sensorRepository: SensorRepository,
    sessionRecorder: SessionRecorder,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onShareFile: (File, String) -> Unit
) {
    val location by locationTracker.state.collectAsState()
    val sensor by sensorRepository.state.collectAsState()
    val recordingState by sessionRecorder.recordingState.collectAsState()
    val sessions by sessionRecorder.sessions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCampus by remember { mutableStateOf(CampusPreset.KAMPUS_1) }
    var selectedPose by remember { mutableStateOf(PosePreset.SHAF_UTAMA) }
    var selectedDurationSec by remember { mutableStateOf<Int?>(15) }

    val theory = remember(location.latitude, location.longitude, location.altitudeM) {
        val lat = location.latitude
        val lon = location.longitude
        if (lat != null && lon != null) {
            QiblaCalculator.computeAll(lat, lon, location.altitudeM)
        } else null
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardDark,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Explore,
                            contentDescription = null,
                            tint = EmeraldBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "QiblaKu",
                            fontWeight = FontWeight.Bold,
                            color = EmeraldBright,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "IMU & WGS84",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    if (recordingState.isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 14.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.RecordDot,
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "REC",
                                color = Coral,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldBright,
                selectedTextColor = EmeraldBright,
                indicatorColor = Emerald.copy(alpha = 0.16f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
            NavigationBar(containerColor = CardDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = navColors,
                    icon = {
                        Icon(
                            imageVector = AppIcons.Explore,
                            contentDescription = "Ukur",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Ukur", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = navColors,
                    icon = {
                        Icon(
                            imageVector = AppIcons.ShowChart,
                            contentDescription = "IMU / Gyro",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("IMU / Gyro", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = navColors,
                    icon = {
                        Icon(
                            imageVector = AppIcons.ListAlt,
                            contentDescription = "Log",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text("Log (${sessions.size})", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> MeasureScreen(
                selectedCampus = selectedCampus,
                onSelectCampus = { selectedCampus = it },
                selectedPose = selectedPose,
                onSelectPose = { selectedPose = it },
                selectedDurationSec = selectedDurationSec,
                onSelectDurationSec = { selectedDurationSec = it },
                location = location,
                theory = theory,
                sensor = sensor,
                recordingState = recordingState,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                onToggleGpsLock = { locationTracker.toggleLock() },
                onSetManualLocation = { lat, lon, alt ->
                    locationTracker.setManualCoordinates(lat, lon, alt)
                },
                onStartRecord = {
                    val t = theory
                    if (t != null) {
                        sessionRecorder.startRecording(
                            campus = selectedCampus,
                            pose = selectedPose,
                            loc = location,
                            theory = t,
                            durationSec = selectedDurationSec
                        )
                    }
                },
                onStopRecord = { sessionRecorder.stopRecording() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            1 -> TelemetryScreen(
                sensor = sensor,
                theory = theory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )

            else -> LogsScreen(
                sessions = sessions,
                onShareFile = onShareFile,
                onExportAllSummary = { sessionRecorder.getSummaryCsvFile() },
                onUpdateComparison = { id, masaObs, nuObs, notes ->
                    sessionRecorder.updateComparison(id, masaObs, nuObs, notes)
                },
                onDeleteSession = { id -> sessionRecorder.deleteSession(id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
