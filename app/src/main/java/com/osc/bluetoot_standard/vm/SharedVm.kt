package com.osc.bluetoot_standard.vm

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.osc.bluetoot_standard.services.MyBluetoothService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedVm @Inject constructor(val bl: MyBluetoothService) : ViewModel() {
    val discoveredDevices = mutableStateOf<List<BluetoothDevice>>(emptyList())
    val bondedDevices = mutableStateOf<List<BluetoothDevice>>(emptyList())


    init {
        collectdiscoveredDevicesData()
        collectBondedDevicesData()
    }

    fun collectdiscoveredDevicesData() {
        viewModelScope.launch {
            bl.discoveredDevices.collect {
                discoveredDevices.value = it
            }
        }
    }

    fun collectBondedDevicesData() {
        viewModelScope.launch {
            bl.bondedDevices.collect {
                bondedDevices.value = it
            }
        }
    }

}