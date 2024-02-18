/*
 * Copyright (c) 2023-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.core.fiiok9

import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.BluetoothCodec
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqValue

object FiioK9Defaults {

    const val DISPLAY_NAME = "FiiO K9 Pro"

    const val AUDIO_FMT = "44.1kHz"
    const val CHANNEL_BALANCE_MIN = -12
    const val CHANNEL_BALANCE_MAX = 12
    const val CHANNEL_BALANCE = CHANNEL_BALANCE_MAX + CHANNEL_BALANCE_MIN
    const val INDICATOR_BRIGHTNESS_MIN = 1
    const val INDICATOR_BRIGHTNESS_MAX = 5
    const val INDICATOR_BRIGHTNESS = INDICATOR_BRIGHTNESS_MAX
    const val FW_VERSION = "1.0"
    const val VOLUME_LEVEL_MIN = 0
    const val VOLUME_LEVEL_MAX = 120
    const val VOLUME = VOLUME_LEVEL_MIN
    const val VOLUME_STEP_SIZE_MIN = 1
    const val VOLUME_STEP_SIZE_MAX = 4
    const val VOLUME_STEP_SIZE = VOLUME_STEP_SIZE_MIN

    val codecsEnabled = listOf(
        BluetoothCodec.AptX.Adaptive,
        BluetoothCodec.Aac,
        BluetoothCodec.Ldac,
        BluetoothCodec.AptX.Default,
        BluetoothCodec.AptX.Ll,
        BluetoothCodec.AptX.Hd
    )
    val eqValues = listOf(
        EqValue(id = 1, value = 0f),
        EqValue(id = 2, value = 0f),
        EqValue(id = 3, value = 0f),
        EqValue(id = 4, value = 0f),
        EqValue(id = 5, value = 0f),
        EqValue(id = 6, value = 0f),
        EqValue(id = 7, value = 0f),
        EqValue(id = 8, value = 0f),
        EqValue(id = 9, value = 0f),
        EqValue(id = 10, value = 0f)
    )
}
