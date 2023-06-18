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

package com.tomg.fiiok9control.audio.business

import android.os.Parcelable
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults.CHANNEL_BALANCE
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioState(
    val channelBalance: Int = CHANNEL_BALANCE,
    val codecsEnabled: List<BluetoothCodec> = FiioK9Defaults.codecsEnabled,
    val lowPassFilter: LowPassFilter = LowPassFilter.Sharp,
    val pendingChannelBalance: Int? = null,
    val pendingCodecsEnabled: List<BluetoothCodec>? = null,
    val pendingCommands: List<Int> = emptyList(),
    val pendingLowPassFilter: LowPassFilter? = null,
    val isServiceConnected: Boolean = false
) : Parcelable
