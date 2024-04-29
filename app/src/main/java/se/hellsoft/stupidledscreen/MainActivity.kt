package se.hellsoft.stupidledscreen

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.bluetooth.ScanResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import se.hellsoft.stupidledscreen.ui.theme.StupidLedScreenTheme

const val TAG = "StupidLedScreen"

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StupidLedScreenTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val permissionsState = rememberMultiplePermissionsState(
                            permissions = listOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )

                        if (!permissionsState.allPermissionsGranted) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    permissionsState.launchMultiplePermissionRequest()
                                }) {
                                Text(text = "Request permissions")
                            }
                        } else {
                            val viewModel by viewModels<LedScreenViewModel>()

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.scanAndConnect() }
                            ) {
                                Text(text = "Scan and connect!")
                            }


                            /*
                                                        var textToLed by remember { mutableStateOf("") }
                                                        OutlinedTextField(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            label = { Text(text = "Text to show on the display") },
                                                            placeholder = { Text(text = "Erik!!!") },
                                                            value = textToLed,
                                                            onValueChange = { textToLed = it }
                                                        )

                                                        Button(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            onClick = {
                                                                Log.d(TAG, "onCreate: Send $textToLed to display!")
                                                            }) {
                                                            Text(text = "Send to display!")
                                                        }
                            */
                        }
                    }
                }
            }
        }
    }
}

