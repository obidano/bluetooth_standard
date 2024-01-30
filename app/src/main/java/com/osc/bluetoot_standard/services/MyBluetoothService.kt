package com.osc.bluetoot_standard.services

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.UUID


class MyBluetoothService(val context: Context) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    var adapter: BluetoothAdapter? = bluetoothManager.adapter
    var socket: BluetoothSocket? = null

    private var pairingReceiver: BroadcastReceiver? = null
    var newDiscoveryReceiver: BroadcastReceiver? = null

    var bondedDevices = MutableSharedFlow<List<BluetoothDevice>>()
    var discoveredDevices = MutableSharedFlow<List<BluetoothDevice>>()

    init {
        handlePairingReceiver()
        handleNewDevicesDiscoveryReceiver()
    }


    private fun handleNewDevicesDiscoveryReceiver() {
        newDiscoveryReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                CoroutineScope(Dispatchers.Default).launch {
                    val action: String = intent.action ?: return@launch
                    val existing = discoveredDevices.first().toMutableList()
                    when (action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice? =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            Timber.d("Found device ${device?.name} ${device?.type}")
                            device?.let {
                                if (it.name == null) return@let
                                existing.add(it)
                                discoveredDevices.emit(existing.toSet().toList())
                            }
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun unregisterAllReceiver() {
        try {
            context.unregisterReceiver(newDiscoveryReceiver)
            adapter?.cancelDiscovery()
            context.unregisterReceiver(pairingReceiver)
        } catch (e: Exception) {
            Timber.d("Failed to unregister")
            Timber.e(e)
        }
    }

    private fun handlePairingReceiver() {
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
                            refreshDiscovery()
                            connectToGattBluetoothDevice(device)

                            /*  val connectThread = object : Thread() {
                                  override fun run() {
                                      // Connect
  //                                    socketConnectToDevice(device)
                                      connectToGattBluetoothDevice(device)
                                  }
                              }
                              connectThread.start()

                             */
                        }
                    }
                }
            }
        }
    }

    fun registerPairingReceiver() {
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(pairingReceiver, filter)
    }

    fun startDiscovery() {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(newDiscoveryReceiver, filter)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            delay(800)
            adapter?.startDiscovery()
        }
    }


    fun refreshBondedDevices() {
        CoroutineScope(Dispatchers.Default).launch {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bondedDevices.emit(adapter?.bondedDevices?.toList() ?: emptyList())
            }
        }
    }

    fun refreshDiscovery() {
        refreshBondedDevices()
        CoroutineScope(Dispatchers.Default).launch {
            discoveredDevices.emit(emptyList())
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@launch
            }
            adapter?.cancelDiscovery()
            delay(1200)
            adapter?.startDiscovery()
        }
    }

    @SuppressLint("MissingPermission")
    fun initConnectionToOneDevice(device: BluetoothDevice) {
      //  adapter?.cancelDiscovery()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(Runnable {
            bindDevice(device)
        }, 800)


    }

    fun checkIfBluetoothSupport(): Boolean {
        return adapter == null
    }

    fun checkIfBluetoothEnabled(): Boolean {
        return adapter?.isEnabled ?: false
    }

    fun initialize() {
        if (!checkIfBluetoothSupport()) {
            Timber.d("Bluetooth  is not supported")
            return
        }

        if (!checkIfBluetoothEnabled()) {
            Timber.d("Bluetooth  is not enabled. opening...")

        }


    }


    @SuppressLint("MissingPermission")
    fun bindDevice(device: BluetoothDevice) {
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
           removeBond(device)
//            connectToGattBluetoothDevice(device)

        } else {
            val dv: BluetoothDevice? = adapter?.getRemoteDevice(device.address)
            val pairingSuccessful = dv?.createBond()
            Timber.d("pairing success status $pairingSuccessful")

        }
    }

    fun removeBond(device: BluetoothDevice) {
        val method = device::class.java.getMethod("removeBond")
        method.invoke(device)
    }

    fun cancelPairing(context: Context) {
        pairingReceiver?.let { context.unregisterReceiver(it) }
    }

    @SuppressLint("MissingPermission")
    fun socketConnectToDevice(device: BluetoothDevice) {

        val uuid: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Use the appropriate UUID for your use case
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            Thread.sleep(5000)
            socket?.connect()
            // Connection successful, perform further operations with the Bluetooth socket
        } catch (e: IOException) {
            // Connection failed, handle the exception
            Timber.d("Connection failed")
            Timber.e(e)
            return
        }
        Timber.d("Connection socket successful $socket")


        //return socket
    }

    fun socketDisconnectFromDevice(socket: BluetoothSocket?): Boolean {
        try {
            socket?.close()
            // Disconnection successful
        } catch (e: IOException) {
            return false
            // Disconnection failed, handle the exception
        }
        return true
    }


    @SuppressLint("MissingPermission")
    fun connectToGattBluetoothDevice(device: BluetoothDevice) {
        lateinit var gattCallback: BluetoothGattCallback
        gattCallback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            @Suppress("DEPRECATION")
            @Deprecated(
                "Used natively in Android 12 and lower",
                ReplaceWith("onCharacteristicChanged(gatt, characteristic, characteristic.value)")
            )

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState);
                Timber.d("onConnectionStateChange $status $newState")
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
//                }
//                if (newState == BluetoothGatt.GATT_SUCCESS) {
                    // Connection established, discover services
                    val handler = Handler(Looper.getMainLooper())
                    handler.post(Runnable {
                        gatt?.discoverServices()
                    })
                }

                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Timber.d("onConnectionStateChange UNBOND")
//                    val method = device::class.java.getMethod("removeBond")
//                    method.invoke(device)
                    gatt?.disconnect()
                    gatt?.close()

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed(Runnable {
                        gatt?.device?.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE )

                    }, 800)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Timber.d("onServicesDiscovered $status")

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Services discovered, find the battery level characteristic
                    val batteryLevelCharacteristic = findBatteryLevelCharacteristic(gatt)
                    // Read the battery level
                    readBatteryLevel(gatt, batteryLevelCharacteristic)
                }
            }


            @Suppress("DEPRECATION")
            @Deprecated(
                "Used natively in Android 12 and lower",
                ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)")
            )
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                Timber.d("onCharacteristicRead $status ${characteristic?.uuid}")

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic?.uuid == BATTERY_LEVEL_UUID) {
                        val batteryLevel = characteristic?.getIntValue(FORMAT_UINT8, 0)
                        Timber.d("Characteristic battery level $batteryLevel")
                        // Use the battery level value
                    }
                }
            }
        }

        val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO)
        gatt.connect()

        val connectThread = object : Thread() {
            override fun run() {
                Thread.sleep(2000)
                gatt.discoverServices()
            }
        }
        connectThread.start()

    }


    private fun findBatteryLevelCharacteristic(gatt: BluetoothGatt?): BluetoothGattCharacteristic? {
        val service = gatt?.getService(BATTERY_SERVICE_UUID)
        return service?.getCharacteristic(BATTERY_LEVEL_UUID)
    }

    @SuppressLint("MissingPermission")
    private fun readBatteryLevel(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        gatt?.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    fun getDeviceTypeName(device: BluetoothDevice): String {
        val deviceClass = device.bluetoothClass
        val deviceType = deviceClass.majorDeviceClass

        return when (deviceType) {
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.PHONE -> "Phone"
//            BluetoothClass.Device.Major.TABLET -> "Tablet"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video Device"
            BluetoothClass.Device.Major.IMAGING -> "Imaging Device"
            BluetoothClass.Device.Major.MISC -> "Miscellaneous Device"
            BluetoothClass.Device.Major.NETWORKING -> "Networking Device"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral Device"
            BluetoothClass.Device.Major.TOY -> "Toy"
            BluetoothClass.Device.Major.HEALTH -> "Health Device"
            BluetoothClass.Device.Major.UNCATEGORIZED -> "Uncategorized Device"
            else -> "Unknown Device"
        }
    }

    // Constants
    private val BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    private val BATTERY_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

}