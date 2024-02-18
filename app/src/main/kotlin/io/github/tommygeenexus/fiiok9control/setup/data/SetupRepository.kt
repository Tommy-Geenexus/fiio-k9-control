/*
 * Copyright (c) 2021-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.setup.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tommygeenexus.fiiok9control.core.ble.BleScanCallback
import io.github.tommygeenexus.fiiok9control.core.ble.BleScanResult
import io.github.tommygeenexus.fiiok9control.core.di.DispatcherIo
import io.github.tommygeenexus.fiiok9control.core.extension.suspendRunCatching
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("BooleanMethodIsAlwaysInverted")
class SetupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
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

    val bleScanResults = MutableSharedFlow<BleScanResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val bleScanCallback = BleScanCallback(
        onScanResult = { bleScanResult -> bleScanResults.tryEmit(bleScanResult) },
        arePermissionsGranted = ::arePermissionsGranted
    )

    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    suspend fun isBluetoothEnabled(): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun arePermissionsGranted() = requiredPermissions.none { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun isDeviceBonded(deviceAddress: String): Boolean {
        if (!arePermissionsGranted()) {
            return false
        }
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                context
                    .getSystemService(BluetoothManager::class.java)
                    ?.adapter
                    ?.bondedDevices
                    ?.find { device -> device.address == deviceAddress } != null
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun startBleScan(): Boolean {
        if (!arePermissionsGranted()) {
            return false
        }
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val scanner = context
                    .getSystemService(BluetoothManager::class.java)
                    ?.adapter
                    ?.bluetoothLeScanner
                if (scanner != null) {
                    val filter = ScanFilter
                        .Builder()
                        .setDeviceName(FiioK9Defaults.DISPLAY_NAME)
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
                    scanner.startScan(listOf(filter), settings, bleScanCallback)
                    true
                } else {
                    false
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun stopBleScan() {
        if (!arePermissionsGranted()) {
            return
        }
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                context
                    .getSystemService(BluetoothManager::class.java)
                    ?.adapter
                    ?.bluetoothLeScanner
                    ?.stopScan(bleScanCallback)
            }.getOrElse { exception ->
                Timber.e(exception)
            }
        }
    }
}
