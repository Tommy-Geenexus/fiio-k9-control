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

package com.tomg.fiiok9control.setup.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.tomg.fiiok9control.DEVICE_K9_PRO_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class SetupRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    }

    @Volatile
    private var callback: BleScanCallback? = null

    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean {
        val bm = context.getSystemService(BluetoothManager::class.java)
        return bm?.adapter?.isEnabled == true
    }

    fun arePermissionsGranted() = requiredPermissions.none { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun isDeviceBonded(deviceAddress: String): Boolean {
        if (!arePermissionsGranted()) {
            return false
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                context
                    .getSystemService(BluetoothManager::class.java)
                    ?.adapter
                    ?.bondedDevices
                    ?.find { device ->
                        device.address == deviceAddress
                    } != null
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun startBleScan(onScanResult: (Result<BleScanResult>) -> Unit): Boolean {
        if (!arePermissionsGranted()) {
            return false
        }
        return withContext(Dispatchers.IO) {
            val scanner =
                context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
            if (scanner != null) {
                val filter = ScanFilter
                    .Builder()
                    .setDeviceName(DEVICE_K9_PRO_NAME)
                    .build()
                val settings = ScanSettings
                    .Builder()
                    .setCallbackType(
                        ScanSettings.CALLBACK_TYPE_FIRST_MATCH or
                            ScanSettings.CALLBACK_TYPE_MATCH_LOST
                    )
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(
                    listOf(filter),
                    settings,
                    getBleScanCallback(onScanResult)
                )
                true
            } else {
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun stopBleScan() {
        if (!arePermissionsGranted()) {
            return
        }
        withContext(Dispatchers.IO) {
            context
                .getSystemService(BluetoothManager::class.java)
                ?.adapter
                ?.bluetoothLeScanner
                ?.stopScan(getBleScanCallback())
            synchronized(this) {
                callback = null
            }
        }
    }

    private fun getBleScanCallback(
        onScanResult: (Result<BleScanResult>) -> Unit = {}
    ): BleScanCallback {
        val v = callback
        return v ?: synchronized(this) {
            val v2 = BleScanCallback(
                permissionsGranted = ::arePermissionsGranted,
                callback = WeakReference { result ->
                    onScanResult(result)
                }
            )
            callback = v2
            v2
        }
    }
}
