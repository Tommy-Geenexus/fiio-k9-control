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

package com.tomg.fiiok9control.audio.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.gaia.GaiaPacketFactory
import com.tomg.fiiok9control.gaia.fiio.K9Command
import com.tomg.fiiok9control.gaia.fiio.K9PacketDecoder
import com.tomg.fiiok9control.gaia.fiio.K9PacketFactory
import com.tomg.fiiok9control.gaia.isFiioPacket
import com.tomg.fiiok9control.setup.data.SetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setupRepository: SetupRepository
) : ViewModel(),
    ContainerHost<AudioState, AudioSideEffect> {

    override val container = container<AudioState, AudioSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = AudioState()
    )

    private val gaiaPacketResponses: MutableList<Int> = mutableListOf()

    fun clearGaiaPacketResponses() {
        gaiaPacketResponses.clear()
    }

    fun handleGaiaPacket(data: ByteArray) = intent {
        val packet = GaiaPacketBLE(data)
        if (packet.isFiioPacket()) {
            gaiaPacketResponses.remove(packet.command)
            when (packet.command) {
                K9Command.Get.CodecEnabled.commandId -> {
                    val codecEnabled = K9PacketDecoder.decodePayloadGetCodecEnabled(packet.payload)
                    if (!codecEnabled.isNullOrEmpty()) {
                        reduce {
                            state.copy(codecsEnabled = codecEnabled)
                        }
                    }
                }
                K9Command.Get.LowPassFilter.commandId -> {
                    val lowPassFilter =
                        K9PacketDecoder.decodePayloadGetLowPassFilter(packet.payload)
                    if (lowPassFilter != null) {
                        reduce {
                            state.copy(lowPassFilter = lowPassFilter)
                        }
                    }
                }
                K9Command.Get.ChannelBalance.commandId -> {
                    val channelBalance =
                        K9PacketDecoder.decodePayloadGetChannelBalance(packet.payload)
                    if (channelBalance != null) {
                        reduce {
                            state.copy(channelBalance = channelBalance)
                        }
                    }
                }
            }
            if (gaiaPacketResponses.isEmpty()) {
                postSideEffect(AudioSideEffect.Characteristic.Changed)
            }
        }
    }

    fun handleGaiaPacketSendResult(commandId: Int) = intent {
        gaiaPacketResponses.remove(commandId)
        if (gaiaPacketResponses.isEmpty()) {
            postSideEffect(AudioSideEffect.Characteristic.Changed)
        }
    }

    fun reconnectToDevice(service: GaiaGattService?) = intent {
        postSideEffect(AudioSideEffect.Reconnect.Initiated)
        if (service != null) {
            val success = service.connectToDevice(setupRepository.getBondedDeviceAddressOrEmpty())
            postSideEffect(
                if (success) {
                    AudioSideEffect.Reconnect.Success
                } else {
                    AudioSideEffect.Reconnect.Failure
                }
            )
        } else {
            postSideEffect(AudioSideEffect.Reconnect.Failure)
        }
    }

    fun sendGaiaPacketCodecEnabled(
        scope: CoroutineScope,
        service: GaiaGattService?,
        codec: BluetoothCodec,
        enabled: Boolean
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.CodecEnabled.commandId)
            postSideEffect(AudioSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val codecsEnabled = if (enabled) {
                    state.codecsEnabled.plus(codec)
                } else {
                    state.codecsEnabled.minus(codec)
                }
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetCodecEnabled(codecsEnabled)
                )
                if (success == true) {
                    reduce {
                        state.copy(codecsEnabled = codecsEnabled)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.CodecEnabled.commandId)
                    postSideEffect(AudioSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketLowPassFilter(
        scope: CoroutineScope,
        service: GaiaGattService?,
        lowPassFilter: LowPassFilter
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.LowPassFilter.commandId)
            postSideEffect(AudioSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetLowPassFilter(lowPassFilter)
                )
                if (success == true) {
                    reduce {
                        state.copy(lowPassFilter = lowPassFilter)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.LowPassFilter.commandId)
                    postSideEffect(AudioSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketChannelBalance(
        scope: CoroutineScope,
        service: GaiaGattService?,
        channelBalance: Int
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.ChannelBalance.commandId)
            postSideEffect(AudioSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetChannelBalance(channelBalance)
                )
                if (success == true) {
                    reduce {
                        state.copy(channelBalance = channelBalance)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.ChannelBalance.commandId)
                    postSideEffect(AudioSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketsDelayed(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandIds = listOf(
                K9Command.Get.CodecEnabled.commandId,
                K9Command.Get.LowPassFilter.commandId,
                K9Command.Get.ChannelBalance.commandId
            )
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(AudioSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                commandIds.forEachIndexed { index, commandId ->
                    val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                    val success = service.sendGaiaPacket(packet)
                    if (success == null || !success) {
                        gaiaPacketResponses.removeAll(commandIds)
                        postSideEffect(
                            AudioSideEffect.Request.Failure(disconnected = success == null)
                        )
                        return@launch
                    }
                    if (index < commandIds.lastIndex) {
                        delay(200)
                    }
                }
            }
        }
    }
}
