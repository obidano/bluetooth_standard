
package com.osc.bluetoot_standard.services

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun HandleBluetoothScanPermisssion(callback: () -> Unit = {}) {
    val bluetoothScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Bluetooth Scan permission granted")
            callback()
        }
    }




    LaunchedEffect(Unit) {
        delay(1700L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothScanLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
        }

    }
}