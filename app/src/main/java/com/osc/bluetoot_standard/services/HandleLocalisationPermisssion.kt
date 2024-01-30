package com.osc.bluetoot_standard.services

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HandleLocalisationPermisssion(callback: () -> Unit = {}) {
    /*
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }*/

    var permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    ) {
        if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true || it[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        ) {
            callback()
        }
    }




    LaunchedEffect(Unit) {
        delay(1700L)
        permissionState.launchMultiplePermissionRequest()
    }
}