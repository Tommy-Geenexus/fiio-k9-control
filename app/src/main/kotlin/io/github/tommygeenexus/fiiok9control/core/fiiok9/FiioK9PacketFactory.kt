/*
 * Copyright (c) 2021-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import androidx.annotation.IntRange
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.BluetoothCodec
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqPreSet
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqValue
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.IndicatorState
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.InputSource
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.LowPassFilter
import io.github.tommygeenexus.fiiok9control.core.gaia.GaiaPacketFactory

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

    fun createGaiaPacketSetCodecEnabled(codecsEnabled: List<BluetoothCodec>) =
        GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.CodecEnabled.commandId,
            payload = byteArrayOf(BluetoothCodec.toByte(codecsEnabled))
        )

    fun createGaiaPacketSetLowPassFilter(lowPassFilter: LowPassFilter) =
        GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.LowPassFilter.commandId,
            payload = byteArrayOf(lowPassFilter.id.toByte())
        )

    fun createGaiaPacketSetChannelBalance(channelBalance: Int) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.ChannelBalance.commandId,
        payload = byteArrayOf(
            if (channelBalance < 0) 1.toByte() else 2.toByte(),
            channelBalance.coerceIn(
                minimumValue = FiioK9Defaults.CHANNEL_BALANCE_MIN,
                maximumValue = FiioK9Defaults.CHANNEL_BALANCE_MAX
            ).toByte()
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

    fun createGaiaPacketSetEqValue(eqValue: EqValue): GaiaPacketBLE {
        val bytes = Integer.toHexString((eqValue.value * 60f).toInt()).toByteArray()
        return GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.EqValue.commandId,
            payload = byteArrayOf(
                eqValue.id.toByte(),
                bytes[0],
                bytes[1]
            )
        )
    }

    fun createGaiaPacketSetVolume(volume: Int) = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Set.Volume.commandId,
        payload = byteArrayOf(
            volume.coerceIn(
                minimumValue = FiioK9Defaults.VOLUME_LEVEL_MIN,
                maximumValue = FiioK9Defaults.VOLUME_LEVEL_MAX
            ).toByte()
        )
    )

    fun createGaiaPacketGetVolume() =
        GaiaPacketFactory.createGaiaPacket(commandId = FiioK9Command.Get.Volume.commandId)

    fun createGaiaPacketGetCodecBit() = GaiaPacketFactory.createGaiaPacket(
        commandId = FiioK9Command.Get.CodecBit.commandId
    )

    fun createGaiaPacketSetHpPreSimultaneously(isSimultaneously: Boolean) =
        GaiaPacketFactory.createGaiaPacket(
            commandId = FiioK9Command.Set.Simultaneously.commandId,
            payload = byteArrayOf(if (isSimultaneously) 1 else 0)
        )
}
