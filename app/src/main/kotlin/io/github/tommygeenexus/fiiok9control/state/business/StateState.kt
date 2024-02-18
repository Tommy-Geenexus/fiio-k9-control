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

package io.github.tommygeenexus.fiiok9control.state.business

import android.os.Parcelable
import io.github.tommygeenexus.fiiok9control.core.extension.relativeTo
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.IndicatorState
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.InputSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class StateState(
    val audioFmt: String = FiioK9Defaults.AUDIO_FMT,
    val fwVersion: String = FiioK9Defaults.FW_VERSION,
    val indicatorBrightness: Int = FiioK9Defaults.INDICATOR_BRIGHTNESS,
    val indicatorState: IndicatorState = IndicatorState.EnabledDefault,
    val inputSource: InputSource = InputSource.Usb,
    val isMqaEnabled: Boolean = true,
    val isMuteEnabled: Boolean = false,
    val isHpPreSimultaneouslyEnabled: Boolean = false,
    val volume: Int = FiioK9Defaults.VOLUME,
    val volumeRelative: Int = FiioK9Defaults.VOLUME.relativeTo(FiioK9Defaults.VOLUME_LEVEL_MAX),
    val volumeStepSize: Int = FiioK9Defaults.VOLUME_STEP_SIZE,
    val pendingCommands: List<Int> = emptyList(),
    val pendingIndicatorBrightness: Int? = null,
    val pendingIndicatorState: IndicatorState? = null,
    val pendingInputSource: InputSource? = null,
    val pendingIsHpPreSimultaneouslyEnabled: Boolean? = null,
    val pendingIsMqaEnabled: Boolean? = null,
    val pendingIsMuteEnabled: Boolean? = null,
    val pendingVolume: Int? = null,
    val pendingVolumeRelative: Int? = null,
    val isDisconnecting: Boolean = false,
    val isExportingProfile: Boolean = false,
    val isServiceConnected: Boolean = false
) : Parcelable
