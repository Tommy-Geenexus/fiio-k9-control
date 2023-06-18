/*
 * Copyright (c) 2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings

class BleScanCallback(
    private val onScanResult: (BleScanResult) -> Unit,
    private val arePermissionsGranted: () -> Boolean
) : ScanCallback() {

    override fun onScanFailed(errorCode: Int) {
        onScanResult(BleScanResult(scanFailed = true))
    }

    @SuppressLint("MissingPermission")
    override fun onScanResult(
        callbackType: Int,
        result: ScanResult?
    ) {
        if (!arePermissionsGranted()) {
            return
        }
        onScanResult(
            BleScanResult(
                deviceAddress = result?.device?.address.orEmpty(),
                bonded = result?.device?.bondState == BluetoothDevice.BOND_BONDED,
                matchLost = callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST
            )
        )
    }
}
