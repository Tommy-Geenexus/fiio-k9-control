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

package com.tomg.fiiok9control.state.business

import android.os.Parcelable
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class StateState(
    val fwVersion: String = "1.0",
    val audioFmt: String = "44.1kHz",
    val volume: Int = 0,
    val volumePercent: String = "0%",
    val inputSource: InputSource = InputSource.Usb,
    val indicatorState: IndicatorState = IndicatorState.EnabledDefault,
    val indicatorBrightness: Int = 5,
    val volumeStepSize: Int = 4,
    val isMuted: Boolean = false,
    val isMqaEnabled: Boolean = true,
    val isProfileExporting: Boolean = false,
    val isHpPreSimultaneously: Boolean = false
) : Parcelable
