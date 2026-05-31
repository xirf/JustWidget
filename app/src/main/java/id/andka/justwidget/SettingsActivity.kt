package id.andka.justwidget

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.andka.justwidget.data.LocationProvider
import id.andka.justwidget.widget.WeatherWorker

/**
 * Small settings screen. Also acts as the launcher entry point (via the
 * LauncherAlias activity-alias), and is what the widget opens when tapped — so
 * the user can always re-enable the icon even after hiding it.
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen()
                }
            }
        }
    }
}

private const val ALIAS = "id.andka.justwidget.LauncherAlias"

private fun isIconHidden(context: Context): Boolean {
    val alias = ComponentName(context.packageName, ALIAS)
    return context.packageManager.getComponentEnabledSetting(alias) ==
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

private fun setIconHidden(context: Context, hidden: Boolean) {
    val alias = ComponentName(context.packageName, ALIAS)
    val newState = if (hidden) {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
    context.packageManager.setComponentEnabledSetting(
        alias, newState, PackageManager.DONT_KILL_APP
    )
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    var hidden by remember { mutableStateOf(isIconHidden(context)) }
    var hasLocation by remember { mutableStateOf(LocationProvider.hasPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocation = LocationProvider.hasPermission(context)
        WeatherWorker.refreshNow(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.hide_icon_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = hidden,
                onCheckedChange = {
                    hidden = it
                    setIconHidden(context, it)
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.hide_icon_hint),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(32.dp))

        if (!hasLocation) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                },
            ) {
                Text(stringResource(R.string.grant_location))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.location_rationale),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { WeatherWorker.refreshNow(context) },
        ) {
            Text(stringResource(R.string.refresh_now))
        }
    }
}
