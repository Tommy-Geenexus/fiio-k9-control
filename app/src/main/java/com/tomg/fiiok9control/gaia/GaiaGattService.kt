/*
 * Copyright (c) 2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tomg.fiiok9control.gaia

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.IBinder
import com.qualcomm.qti.libraries.ble.BLEService
import com.qualcomm.qti.libraries.ble.Characteristics
import com.qualcomm.qti.libraries.ble.ErrorStatus
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import kotlinx.coroutines.channels.Channel
import java.lang.ref.WeakReference
import java.util.UUID

class GaiaGattService : BLEService() {

    private val notificationCharacteristics: MutableList<UUID> = mutableListOf()
    val gaiaGattSideEffectChannel = Channel<GaiaGattSideEffect>(Channel.UNLIMITED)

    private var binder: GaiaGattBinder? = null
    private var gattService: BluetoothGattService? = null
    private var gaiaResponseCharacteristic: BluetoothGattCharacteristic? = null
    private var gaiaCommandCharacteristic: BluetoothGattCharacteristic? = null
    private var gaiaDataCharacteristic: BluetoothGattCharacteristic? = null
    private var isGattReady = false
    private var isGaiaReady = false

    override fun onCreate() {
        super.onCreate()
        initialize()
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (binder == null) {
            binder = GaiaGattBinder(WeakReference(this))
        }
        return binder
    }

    public override fun connectToDevice(address: String) = super.connectToDevice(address)

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (characteristic != null) {
            if (!isGattReady && characteristic.uuid == UUID_CHARACTERISTIC_GAIA_DATA_ENDPOINT) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        onGattReady()
                    }
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION,
                    BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION,
                    ErrorStatus.ATT.INSUFFICIENT_AUTHORIZATION,
                    ErrorStatus.GattApi.GATT_AUTH_FAIL,
                    ErrorStatus.GattApi.GATT_ERROR,
                    ErrorStatus.HCI.INSUFFICIENT_SECURITY -> {
                        disconnectDeviceAndReset()
                        gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gatt.Error)
                    }
                }
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (status != BluetoothGatt.GATT_SUCCESS && characteristic?.value != null) {
            gaiaGattSideEffectChannel.trySend(
                GaiaGattSideEffect.Gatt.WriteCharacteristic.Success(
                    commandId = GaiaPacketBLE(characteristic.value).command
                )
            )
        } else if (status != BluetoothGatt.GATT_SUCCESS && characteristic?.value != null) {
            gaiaGattSideEffectChannel.trySend(
                GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure(
                    commandId = GaiaPacketBLE(characteristic.value).command
                )
            )
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        if (gatt != null && status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gatt.ServiceDiscovery)
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gatt.Disconnected)
            }
        }
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        val uuid = descriptor?.characteristic?.uuid
        if (uuid != null && status == BluetoothGatt.GATT_SUCCESS) {
            notificationCharacteristics.add(uuid)
        }
        if (descriptor != null && status == BluetoothGatt.GATT_SUCCESS) {
            if (descriptor.uuid == Characteristics.CLIENT_CHARACTERISTIC_CONFIG &&
                uuid == UUID_CHARACTERISTIC_GAIA_RESPONSE
            ) {
                val ready = isGaiaReady
                isGaiaReady = true
                if (!ready) {
                    gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gaia.Ready)
                }
            }
        }
    }

    override fun onMtuChanged(
        gatt: BluetoothGatt?,
        mtu: Int,
        status: Int
    ) {
    }

    override fun onReceivedCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (characteristic != null && characteristic.uuid == UUID_CHARACTERISTIC_GAIA_RESPONSE) {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gaia.Packet(data))
            }
        }
    }

    override fun onRemoteRssiRead(
        gatt: BluetoothGatt?,
        rssi: Int,
        status: Int
    ) {
    }

    override fun onServicesDiscovered(
        gatt: BluetoothGatt?,
        status: Int
    ) {
        if (gatt != null && status == BluetoothGatt.GATT_SUCCESS) {
            gatt.services.forEach { service ->
                if (checkService(service)) {
                    return@forEach
                }
            }
            if (gaiaDataCharacteristic != null) {
                requestReadCharacteristicForPairing(gaiaDataCharacteristic)
            } else {
                onGattReady()
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        disconnectDeviceAndReset()
        binder = null
        return false
    }

    private fun checkService(gattService: BluetoothGattService): Boolean {
        return if (gattService.uuid == UUID_SERVICE_GAIA) {
            this.gattService = gattService
            gattService.characteristics.forEach { characteristic ->
                when (characteristic.uuid) {
                    UUID_CHARACTERISTIC_GAIA_RESPONSE -> {
                        gaiaResponseCharacteristic = characteristic
                    }
                    UUID_CHARACTERISTIC_GAIA_COMMAND -> {
                        if ((
                            characteristic.properties and
                                BluetoothGattCharacteristic.PROPERTY_WRITE
                            ) > 0
                        ) {
                            gaiaCommandCharacteristic = characteristic
                        }
                    }
                    UUID_CHARACTERISTIC_GAIA_DATA_ENDPOINT -> {
                        if ((
                            characteristic.properties and
                                BluetoothGattCharacteristic.PROPERTY_READ
                            ) > 0
                        ) {
                            gaiaDataCharacteristic = characteristic
                        }
                    }
                }
            }
            true
        } else {
            false
        }
    }

    private fun disconnectDeviceAndReset() {
        notificationCharacteristics.forEach { characteristic ->
            requestCharacteristicNotification(characteristic, false)
        }
        disconnectFromDevice()
        reset()
    }

    fun isConnected() = connectionState == State.CONNECTED

    private fun onGattReady() {
        val ready = isGattReady
        isGattReady = true
        if (!ready) {
            gaiaGattSideEffectChannel.trySend(GaiaGattSideEffect.Gatt.Ready)
        }
        requestCharacteristicNotification(gaiaResponseCharacteristic, true)
    }

    private fun reset() {
        gattService = null
        gaiaResponseCharacteristic = null
        gaiaCommandCharacteristic = null
        gaiaDataCharacteristic = null
        isGattReady = false
        isGaiaReady = false
        notificationCharacteristics.clear()
    }

    fun sendGaiaPacket(packet: GaiaPacket): Boolean {
        return if (gaiaCommandCharacteristic != null) {
            requestWriteCharacteristic(gaiaCommandCharacteristic, packet.bytes)
        } else {
            false
        }
    }
}
