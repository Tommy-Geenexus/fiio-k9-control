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

import androidx.annotation.IntRange
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.eq.EqPreSet
import com.tomg.fiiok9control.eq.EqValue
import com.tomg.fiiok9control.gaia.data.GaiaPacketFactory
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.toBytes

object FiioK9PacketFactory {

    fun createGaiaPacketSetIndicatorStateAndBrightness(
        indicatorState: IndicatorState,
        @IntRange(
            from = FiioK9Defaults.INDICATOR_BRIGHTNESS_MIN.toLong(),
            to = FiioK9Defaults.INDICATOR_BRIGHTNESS_MAX.toLong()
        ) indicatorBrightness: Int
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.IndicatorRgbLighting.commandId,
        payload = byteArrayOf(
            indicatorState.id.toByte(),
            indicatorBrightness.toByte()
        )
    )

    fun createGaiaPacketSetMqaEnabled(isMqaEnabled: Boolean) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.MqaEnabled.commandId,
        payload = byteArrayOf(if (isMqaEnabled) 1 else 0)
    )

    fun createGaiaPacketSetMuteEnabled(isMuteEnabled: Boolean) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.MuteEnabled.commandId,
        payload = byteArrayOf(if (isMuteEnabled) 1 else 0)
    )

    fun createGaiaPacketSetRestore() =
        GaiaPacketFactory.createGaiaPacket(commandId = FiioK9Command.Set.Restore.commandId)

    fun createGaiaPacketSetStandby() =
        GaiaPacketFactory.createGaiaPacket(commandId = FiioK9Command.Set.Standby.commandId)

    fun createGaiaPacketSetInputSource(inputSource: InputSource) =
        GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.InputSource.commandId,
            payload = byteArrayOf(inputSource.id.toByte())
        )

    fun createGaiaPacketSetCodecEnabled(
        codecsEnabled: List<BluetoothCodec>
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.CodecEnabled.commandId,
        payload = byteArrayOf(BluetoothCodec.toByte(codecsEnabled))
    )

    fun createGaiaPacketSetLowPassFilter(
        lowPassFilter: LowPassFilter
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.LowPassFilter.commandId,
        payload = byteArrayOf(lowPassFilter.id.toByte())
    )

    fun createGaiaPacketSetChannelBalance(
        @IntRange(
            from = FiioK9Defaults.CHANNEL_BALANCE_MIN.toLong(),
            to = FiioK9Defaults.CHANNEL_BALANCE_MAX.toLong()
        ) channelBalance: Int
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.ChannelBalance.commandId,
        payload = byteArrayOf(
            if (channelBalance < 0) 1.toByte() else 2.toByte(),
            channelBalance.toByte()
        )
    )

    fun createGaiaPacketSetEqEnabled(eqEnabled: Boolean) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.EqEnabled.commandId,
        payload = byteArrayOf(if (eqEnabled) 1 else 0)
    )

    fun createGaiaPacketSetEqPreSet(eqPreSet: EqPreSet) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.EqPreSet.commandId,
        payload = byteArrayOf(eqPreSet.id.toByte())
    )

    fun createGaiaPacketSetEqValue(
        eqValue: EqValue
    ): GaiaPacketBLE {
        val bytes = Integer.toHexString((eqValue.value * 60f).toInt()).toBytes()
        return GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.EqValue.commandId,
            payload = byteArrayOf(
                eqValue.id.toByte(),
                bytes[0],
                bytes[1]
            )
        )
    }

    fun createGaiaPacketSetVolume(
        @IntRange(
            from = FiioK9Defaults.VOLUME_LEVEL_MIN.toLong(),
            to = FiioK9Defaults.VOLUME_LEVEL_MAX.toLong()
        ) volume: Int
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.Volume.commandId,
        payload = byteArrayOf(volume.toByte())
    )

    fun createGaiaPacketGetVolume() =
        GaiaPacketFactory.createGaiaPacket(commandId = FiioK9Command.Get.Volume.commandId)

    fun createGaiaPacketGetCodecBit() = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Get.CodecBit.commandId
    )

    fun createGaiaPacketSetHpPreSimultaneously(
        isSimultaneously: Boolean
    ) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.Simultaneously.commandId,
        payload = byteArrayOf(if (isSimultaneously) 1 else 0)
    )
}
