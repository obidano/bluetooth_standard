package com.osc.bluetoot_standard.pages

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
import com.osc.bluetoot_standard.services.HandleLocalisationPermisssion
import com.osc.bluetoot_standard.services.MyBluetoothService
import com.osc.bluetoot_standard.ui.theme.Bluetoot_standardTheme
import com.osc.bluetoot_standard.vm.SharedVm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


@Composable
fun HomePage2(sharedVm: SharedVm, activity: ComponentActivity) {
    val bl = sharedVm.bl
    var bondedDevices by remember {
        mutableStateOf<List<BluetoothDevice>>(emptyList())
    }
    var discoveredDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val context = LocalContext.current

    val refreshBondedDevices = {
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bondedDevices = bl.adapter?.bondedDevices?.toList() ?: emptyList()

        }

    }


    val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: return
            val existing = discoveredDevices.toMutableList()
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Timber.d("Found device ${device?.name} ${device?.type}")
                    device?.let {
                        if (it.name == null) return@let
                        existing.add(it)
                        discoveredDevices = existing.toSet().toList()
                    }
                }
            }
        }
    }

    val mPairingRequestRecevier: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_PAIRING_REQUEST == intent.action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val type =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)
                Timber.d("mPairingRequestRecevier $type")
                if (type == BluetoothDevice.PAIRING_VARIANT_PIN) {

//                    device!!.setPin(Util.IntToPasskey(pinCode()))
//                    abortBroadcast()
                } else {
                    Timber.d("Unexpected pairing type: $type")
                }
            }
        }
    }

    val startDiscovery = {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        // Register the receiver
        activity.registerReceiver(discoveryReceiver, filter)
        // Start Bluetooth discovery
        bl.adapter?.startDiscovery()
    }

    val scope = rememberCoroutineScope()
    val refreshDiscovery: () -> Unit = {
        refreshBondedDevices()
        scope.launch {
            discoveredDevices = emptyList()
            bl.adapter?.cancelDiscovery()
            delay(800)
            bl.adapter?.startDiscovery()
        }

    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        HandleLocalisationPermisssion() {

        }
    }

    val bluetoothScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Bluetooth Scan permission granted")
            startDiscovery()
        }
    }

    var pairingReceiver: BroadcastReceiver? = null

    LaunchedEffect(Unit) {
        refreshBondedDevices()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            startDiscovery()
        } else {
            bluetoothScanLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
        }

        pairingReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)

                    if (device != null && bondState != -1) {
                        Timber.d("bond state $bondState $device")
                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            val pairingRequestFilter =
                                IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
                            pairingRequestFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY - 1;
                            activity.applicationContext.registerReceiver(
                                mPairingRequestRecevier,
                                pairingRequestFilter
                            );

//                            val handler = Handler(Looper.getMainLooper())
//                            handler.postDelayed(Runnable {
//                            bl.connectToDevice(device)
                            val connectThread = object : Thread() {
                                override fun run() {
//                                    super.run()
//                                    bl.connectToGattBluetoothDevice(context, device)
                                    bl.socketConnectToDevice(device)
                                }
                            }
                            connectThread.start()
//
//                            }, 3000)


                        }
                        //onBondStateChanged.invoke(device, bondState)
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(pairingReceiver, filter)

    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                context.unregisterReceiver(discoveryReceiver)
                bl.adapter?.cancelDiscovery()
                context.unregisterReceiver(pairingReceiver)
            } catch (e: Exception) {
                Timber.d("Failed to unregister")
                Timber.e(e)
            }


        }
    }

    val connectToOneDevice: (BluetoothDevice) -> Unit = {
        scope.launch {
//            activity.unregisterReceiver(discoveryReceiver)
            bl.adapter?.cancelDiscovery()
//            bl.bindDevice(context, it)

        }

    }



    Timber.d("bondedDevices ${bondedDevices.size}")
    Timber.d("discoveredDevices ${discoveredDevices.size}")

    HomeBodyPage2(
        activity,
        bl,
        refreshBondedDevices,
        bondedDevices,
        discoveredDevices,
        refreshDiscovery,
        connectToOneDevice
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeBodyPage2(
    activity: ComponentActivity = ComponentActivity(),
    bl: MyBluetoothService? = null,
    refreshBondedDevices: () -> Unit,
    bondedDevices: List<BluetoothDevice>,
    discoveredDevices: List<BluetoothDevice>,
    refreshDiscovery: () -> Unit,
    connectToOneDevice: (BluetoothDevice) -> Unit = { }
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
                BluetoothEnabler(activity, adapter = bl?.adapter, refreshBondedDevices)
                Button(onClick = { refreshDiscovery() }) {
                    Icon(Icons.Default.Refresh, "")
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            ListPairedDevices2(bondedDevices, bl, "Bonded devices", connectToOneDevice)
            Divider(modifier = Modifier.fillMaxWidth())
            ListPairedDevices2(discoveredDevices, bl, "Available devices", connectToOneDevice)

        }
    }

}

@SuppressLint("MissingPermission")
@Composable
fun ColumnScope.ListPairedDevices2(
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

@Preview(showBackground = true, device = Devices.PIXEL_2)
@Preview(showBackground = true, device = Devices.PIXEL_2, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewHomePage() {
    Bluetoot_standardTheme {
        HomeBodyPage2(
            refreshBondedDevices = {},
            bondedDevices = emptyList(),
            discoveredDevices = emptyList(),
            refreshDiscovery = {},
        )
    }
}
