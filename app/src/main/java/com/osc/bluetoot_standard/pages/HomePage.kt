package com.osc.bluetoot_standard.pages

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osc.bluetoot_standard.services.HandleBluetoothScanPermisssion
import com.osc.bluetoot_standard.services.HandleLocalisationPermisssion
import com.osc.bluetoot_standard.services.MyBluetoothService
import com.osc.bluetoot_standard.ui.theme.Bluetoot_standardTheme
import com.osc.bluetoot_standard.vm.SharedVm
import timber.log.Timber


@Composable
fun HomePage(sharedVm: SharedVm, activity: ComponentActivity) {
    val bl = sharedVm.bl
    val bondedDevices = sharedVm.bondedDevices.value
    val discoveredDevices = sharedVm.discoveredDevices.value
    val context = LocalContext.current


    val scope = rememberCoroutineScope()

    HandleLocalisationPermisssion()

    HandleBluetoothScanPermisssion() {
        bl.startDiscovery()
    }


    LaunchedEffect(Unit) {
        bl.refreshBondedDevices()
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//        bl.startDiscovery()
//        }

        bl.registerPairingReceiver()

    }

    DisposableEffect(Unit) {
        onDispose {
            bl.unregisterAllReceiver()
        }
    }



    Timber.d("bondedDevices ${bondedDevices.size}")
    Timber.d("discoveredDevices ${discoveredDevices.size}")

    HomeBodyPage(
        activity,
        bl,
        bondedDevices,
        discoveredDevices
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeBodyPage(
    activity: ComponentActivity = ComponentActivity(),
    bl: MyBluetoothService,
    bondedDevices: List<BluetoothDevice> = emptyList(),
    discoveredDevices: List<BluetoothDevice> = emptyList(),
) {


    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(25.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BluetoothEnabler(activity, adapter = bl.adapter, { bl.refreshBondedDevices() })
                Button(onClick = { bl.refreshDiscovery() }) {
                    Icon(Icons.Default.Refresh, "")
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            ListPairedDevices(
                bondedDevices,
                bl,
                "Bonded devices",
                { bl.initConnectionToOneDevice(it) })
            Divider(modifier = Modifier.fillMaxWidth())
            ListPairedDevices(
                discoveredDevices,
                bl,
                "Available devices",
                { bl.initConnectionToOneDevice(it) })

        }
    }

}

@SuppressLint("MissingPermission")
@Composable
fun ColumnScope.ListPairedDevices(
    devices: List<BluetoothDevice>,
    bl: MyBluetoothService?,
    title: String,
    connectToOneDevice: (BluetoothDevice) -> Unit
) {
    LazyColumn(Modifier.weight(1f)) {
        item {
            Spacer(modifier = Modifier.size(25.dp))
            Text(
                "$title (${devices.size})",
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 25.dp)
            )
            Spacer(modifier = Modifier.size(25.dp))
        }
        itemsIndexed(devices) { i, data ->
            var isConnected by remember {
                mutableStateOf<BluetoothSocket?>(null)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 20.dp)
                    .clickable {
                        connectToOneDevice(data)
                    }
            ) {
                Text("${i + 1}")
                Spacer(modifier = Modifier.width(15.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text("Device: ")
                        Text(data.name)
                    }
                    Text(data.address)
                    Row {
                        Text("Type: ")
                        Text(bl?.getDeviceTypeName(data) ?: "Non defini")
                    }
                }
                AnimatedVisibility(visible = isConnected != null) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Check, "")
                    }
                }
            }

        }
    }
}

@Composable
fun BluetoothEnabler(
    activity: ComponentActivity,
    adapter: BluetoothAdapter?,
    refreshBondedDevices: () -> Unit
) {
    var isEnabled = remember {
        mutableStateOf(adapter?.isEnabled ?: false)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != ComponentActivity.RESULT_OK) {
            Timber.d("Bluetooth not activated")
            isEnabled.value = false
            // Bluetooth enabling was not successful, handle accordingly
        } else {
            isEnabled.value = true
            refreshBondedDevices()
        }
    }

    val disableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != ComponentActivity.RESULT_OK) {
            Timber.d("Bluetooth not activated")
            isEnabled.value = true
            // Bluetooth enabling was not successful, handle accordingly
        } else {
            isEnabled.value = false
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            refreshBondedDevices()
            Timber.d("Bluetooth Connect permission granted")
            //  onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    Button(onClick = {
        if (adapter != null && !adapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBluetoothIntent)
        } else {

            val disableBluetoothLauncherInt =
                Intent("android.bluetooth.adapter.action.REQUEST_DISABLE")
            disableBluetoothLauncher.launch(disableBluetoothLauncherInt)


        }
    }) {
        Text(text = "${if (!isEnabled.value) " Enable" else "Disable"} Bluetooth")
    }
}


@Composable

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Preview(showBackground = true, device = Devices.PIXEL_2, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewHomePage() {
    Bluetoot_standardTheme {
        val context = LocalContext.current
        HomeBodyPage(
            bl = MyBluetoothService(context)
        )
    }
}
