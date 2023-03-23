/*
 * Copyright (c) 2021-2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import androidx.core.app.ActivityCompat
import com.qualcomm.qti.libraries.ble.BLEService
import com.qualcomm.qti.libraries.ble.Characteristics
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GaiaGattService : BLEService() {

    private val notificationCharacteristics: MutableList<UUID> = mutableListOf()
    val gaiaGattSideEffectChannel = Channel<GaiaGattSideEffect>(Channel.UNLIMITED)

    private lateinit var bm: BluetoothManager

    private var binder: GaiaGattBinder? = null
    private var gattService: BluetoothGattService? = null
    private var gaiaResponseCharacteristic: BluetoothGattCharacteristic? = null
    private var gaiaCommandCharacteristic: BluetoothGattCharacteristic? = null
    private var gaiaDataCharacteristic: BluetoothGattCharacteristic? = null
    private var isGattReady = false
    private var isGaiaReady = false

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler

    public override fun getDevice(): BluetoothDevice? = super.getDevice()

    override fun onBind(intent: Intent?): IBinder? {
        if (binder == null) {
            binder = GaiaGattBinder(WeakReference(this))
        }
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        bm = applicationContext.getSystemService(BluetoothManager::class.java)
        coroutineScope = CoroutineScope(Dispatchers.Main.immediate)
        thread = HandlerThread("GaiaGattService", Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        handler = Handler(thread.looper)
        initialize()
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray?,
        status: Int
    ) {
        if (gatt == null ||
            characteristic == null ||
            characteristic.uuid != UUID_CHARACTERISTIC_GAIA_DATA_ENDPOINT ||
            isGattReady
        ) {
            return
        }
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> {
                onGattReady()
            }
            else -> {
                coroutineScope.launch {
                    gaiaGattSideEffectChannel.send(GaiaGattSideEffect.Gatt.Error)
                    disconnectAndReset()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        val value = characteristic?.value ?: return
        coroutineScope.launch {
            gaiaGattSideEffectChannel.send(
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    GaiaGattSideEffect.Gatt.WriteCharacteristic.Success(
                        commandId = GaiaPacketBLE(value).command
                    )
                } else {
                    GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure(
                        commandId = GaiaPacketBLE(value).command
                    )
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        if (gatt == null) {
            return
        }
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            coroutineScope.launch {
                gaiaGattSideEffectChannel.send(
                    GaiaGattSideEffect.Gatt.ServiceDiscovery(device?.address.orEmpty())
                )
            }
            if (hasBluetoothConnectPermission()) {
                gatt.discoverServices()
            }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            coroutineScope.launch {
                gaiaGattSideEffectChannel.send(GaiaGattSideEffect.Gatt.Disconnected)
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
        if (descriptor == null || status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        val uuid = descriptor.characteristic?.uuid
        if (uuid != null) {
            notificationCharacteristics.add(uuid)
        }
        if (descriptor.uuid == Characteristics.CLIENT_CHARACTERISTIC_CONFIG &&
            uuid == UUID_CHARACTERISTIC_GAIA_RESPONSE
        ) {
            val ready = isGaiaReady
            isGaiaReady = true
            if (!ready) {
                coroutineScope.launch {
                    gaiaGattSideEffectChannel.send(GaiaGattSideEffect.Gaia.Ready)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch {
            disconnectAndReset()
        }.invokeOnCompletion {
            coroutineScope.cancel()
        }
        handler.removeCallbacksAndMessages(null)
        thread.quit()
    }

    override fun onMtuChanged(
        gatt: BluetoothGatt?,
        mtu: Int,
        status: Int
    ) {
    }

    override fun onReceivedCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray?
    ) {
        if (characteristic != null &&
            characteristic.uuid == UUID_CHARACTERISTIC_GAIA_RESPONSE &&
            value != null &&
            value.isNotEmpty()
        ) {
            coroutineScope.launch {
                gaiaGattSideEffectChannel.send(GaiaGattSideEffect.Gaia.Packet(value))
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
        binder = null
        return false
    }

    override fun provideHandler() = handler

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

    suspend fun connect(address: String): Boolean {
        return withContext(Dispatchers.IO) {
            disconnectAndReset()
            super.connectToDevice(address)
        }
    }

    suspend fun disconnectAndReset() {
        withContext(Dispatchers.IO) {
            notificationCharacteristics.forEach { characteristic ->
                requestCharacteristicNotification(characteristic, false)
            }
            disconnectFromDevice()
            reset()
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isConnected() = connectionState == State.CONNECTED

    private fun onGattReady() {
        val ready = isGattReady
        isGattReady = true
        if (!ready) {
            coroutineScope.launch {
                gaiaGattSideEffectChannel.send(
                    GaiaGattSideEffect.Gatt.Ready(device?.address.orEmpty())
                )
            }
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

    @SuppressLint("MissingPermission")
    suspend fun sendGaiaPacket(packet: GaiaPacket): Boolean? {
        return withContext(Dispatchers.IO) {
            if (!hasBluetoothConnectPermission()) {
                return@withContext false
            }
            if (bm.getConnectedDevices(BluetoothProfile.GATT).isNullOrEmpty()) {
                return@withContext null
            }
            if (gaiaCommandCharacteristic != null) {
                requestWriteCharacteristic(gaiaCommandCharacteristic, packet.bytes)
            } else {
                false
            }
        }
    }
}
