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

package com.tomg.fiiok9control.gaia.data.fiio

import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.eq.EqPreSet
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.toHexString

object FiioK9PacketDecoder {

    fun decodePayloadVersion(payload: ByteArray): String? {
        val version = payload.toHexString()
        if (version.length < 6) {
            return null
        }
        val versionMajor = version.substring(0, 4).toIntOrNull(radix = 16)
        val versionMinor = version.substring(4).toIntOrNull(radix = 16)
        return if (versionMajor != null && versionMinor != null) {
            "$versionMajor.$versionMinor"
        } else {
            null
        }
    }

    fun decodePayloadAudioFormat(payload: ByteArray): String? {
        val audioFormat = payload.toHexString()
        if (audioFormat.length < 6) {
            return null
        }
        val id = audioFormat.substring(0, 4).toIntOrNull(radix = 16)
        val sampleRate = audioFormat.substring(4).toIntOrNull(radix = 16)
        return if (id != null && sampleRate != null) {
            when (id) {
                1 -> String.format(null, "%.1fkHz", sampleRate.toFloat() * 44.1f)
                2 -> String.format(null, "%dkHz", sampleRate * 48)
                3 -> String.format(null, "DSD%d", sampleRate * 64)
                4 -> "MQA"
                else -> "0kHz"
            }
        } else {
            null
        }
    }

    fun decodePayloadVolume(payload: ByteArray): Int? {
        return payload.toHexString().toIntOrNull(radix = 16)
    }

    fun decodePayloadMuteEnabled(payload: ByteArray): Boolean? {
        return payload.toHexString().toIntOrNull(radix = 16)?.equals(1)
    }

    fun decodePayloadMqaEnabled(payload: ByteArray): Boolean? {
        return payload.toHexString().toIntOrNull(radix = 16)?.equals(1)
    }

    fun decodePayloadInputSource(payload: ByteArray): InputSource? {
        val id = payload.toHexString().toIntOrNull(radix = 16)
        return if (id != null) {
            InputSource.findById(id)
        } else {
            null
        }
    }

    fun decodePayloadIndicatorRgbLighting(payload: ByteArray): Pair<IndicatorState, Int>? {
        val indicatorRgbLighting = payload.toHexString()
        if (indicatorRgbLighting.length < 6) {
            return null
        }
        val id = indicatorRgbLighting.substring(0, 4).toIntOrNull(radix = 16) ?: -1
        val indicatorState = IndicatorState.findById(id)
        val indicatorBrightness = indicatorRgbLighting.substring(4).toIntOrNull(radix = 16)
        return if (indicatorState == null || indicatorBrightness == null ||
            indicatorBrightness < FiioK9Defaults.INDICATOR_BRIGHTNESS_MIN ||
            indicatorBrightness > FiioK9Defaults.INDICATOR_BRIGHTNESS_MAX
        ) {
            null
        } else {
            indicatorState to indicatorBrightness
        }
    }

    fun decodePayloadCodecEnabled(payload: ByteArray): List<BluetoothCodec>? {
        val id = payload.toHexString().toIntOrNull(radix = 16)
        return if (id != null) {
            BluetoothCodec.findById(id)
        } else {
            null
        }
    }

    fun decodePayloadLowPassFilter(payload: ByteArray): LowPassFilter? {
        val id = payload.toHexString().toIntOrNull(radix = 16) ?: -1
        return LowPassFilter.findById(id)
    }

    fun decodePayloadChannelBalance(payload: ByteArray): Int? {
        val tmp = payload.toHexString()
        if (tmp.length < 6) {
            return null
        }
        val leftChannel = tmp.startsWith("0001")
        val rightChannel = tmp.startsWith("0002")
        val channelBalance = tmp.substring(4).toIntOrNull(radix = 16)
        return if (channelBalance != null && (leftChannel || rightChannel)) {
            channelBalance
        } else {
            null
        }
    }

    fun decodePayloadEqEnabled(payload: ByteArray): Boolean? {
        val eqEnabled = payload.toHexString().toIntOrNull(radix = 16) ?: -1
        return if (eqEnabled >= 0) {
            eqEnabled > 0
        } else {
            null
        }
    }

    fun decodePayloadEqPreSet(payload: ByteArray): EqPreSet? {
        val id = payload.toHexString().toIntOrNull(radix = 16) ?: -1
        return EqPreSet.findById(id)
    }

    fun decodePayloadHpPreSimultaneously(payload: ByteArray): Boolean? {
        return payload.toHexString().toIntOrNull(radix = 16)?.equals(1)
    }
}
