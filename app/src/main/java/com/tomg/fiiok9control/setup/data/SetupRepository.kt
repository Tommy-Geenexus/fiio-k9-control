/*
 * Copyright (c) 2021-2022, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.tomg.fiiok9control.Empty
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SetupRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private companion object {

        const val DEVICE_K9_PRO_NAME = "FiiO K9 Pro"
    }

    private val bm = context.getSystemService(BluetoothManager::class.java)

    fun isBleSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled() = bm?.adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getBondedDeviceAddressOrEmpty(): String {
        return runCatching {
            bm
                ?.adapter
                ?.bondedDevices
                ?.find { device -> device.name == DEVICE_K9_PRO_NAME }
                ?.address
                .orEmpty()
        }.getOrElse {
            String.Empty
        }
    }
}
