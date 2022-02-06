/*
 * Copyright (c) 2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.gaia.GaiaPacketFactory
import com.tomg.fiiok9control.gaia.isFiioPacket
import com.tomg.fiiok9control.setup.data.SetupRepository
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.toHexString
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
class StateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setupRepository: SetupRepository
) : ViewModel(),
    ContainerHost<StateState, StateSideEffect> {

    override val container = container<StateState, StateSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = StateState()
    )

    private val gaiaPacketResponses: MutableList<Int> = mutableListOf()

    fun clearGaiaPacketResponses() {
        gaiaPacketResponses.clear()
    }

    private fun getAudioFmt(
        id: Int,
        sampleRate: Int
    ) = when (id) {
        1 -> String.format(null, "%.1fkHz", sampleRate.toFloat() * 44.1f)
        2 -> String.format(null, "%dkHz", sampleRate * 48)
        3 -> String.format(null, "DSD%d", sampleRate * 64)
        4 -> "MQA"
        else -> "0kHz"
    }

    fun handleGaiaPacket(data: ByteArray) = intent {
        val packet = GaiaPacketBLE(data)
        if (packet.isFiioPacket()) {
            gaiaPacketResponses.remove(packet.command)
            val payload = packet.payload.toHexString()
            when (packet.command) {
                GaiaPacketFactory.CMD_ID_VERSION_GET -> {
                    if (payload.length < 6) return@intent
                    val versionMajor = payload.substring(0, 4).toIntOrNull(radix = 16)
                    val versionMinor = payload.substring(4).toIntOrNull(radix = 16)
                    if (versionMajor != null && versionMinor != null) {
                        reduce {
                            state.copy(fwVersion = "$versionMajor.$versionMinor")
                        }
                    }
                }
                GaiaPacketFactory.CMD_ID_AUDIO_FMT_GET -> {
                    if (payload.length < 6) return@intent
                    val id = payload.substring(0, 4).toIntOrNull(radix = 16)
                    val sampleRate = payload.substring(4).toIntOrNull(radix = 16)
                    if (id != null && sampleRate != null) {
                        reduce {
                            state.copy(audioFmt = getAudioFmt(id, sampleRate))
                        }
                    }
                }
                GaiaPacketFactory.CMD_ID_MUTE_EN_GET -> {
                    val isMuted = payload.toIntOrNull(radix = 16) == 1
                    reduce {
                        state.copy(isMuted = isMuted)
                    }
                }
                GaiaPacketFactory.CMD_ID_MQA_EN_GET -> {
                    val isMqaEnabled = payload.toIntOrNull(radix = 16) ?: 1 == 1
                    reduce {
                        state.copy(isMqaEnabled = isMqaEnabled)
                    }
                }
                GaiaPacketFactory.CMD_ID_INPUT_SRC_GET -> {
                    val id = payload.toIntOrNull(radix = 16) ?: -1
                    val inputSource = InputSource.findById(id)
                    if (inputSource != null) {
                        reduce {
                            state.copy(inputSource = inputSource)
                        }
                    }
                }
                GaiaPacketFactory.CMD_ID_INDICATOR_RGB_LIGHTING_GET -> {
                    if (payload.length < 6) return@intent
                    val id = payload.substring(0, 4).toIntOrNull(radix = 16) ?: -1
                    val indicatorState = IndicatorState.findById(id)
                    val indicatorBrightness = payload.substring(4).toIntOrNull(radix = 16)
                    if (indicatorState != null && indicatorBrightness != null) {
                        reduce {
                            state.copy(
                                indicatorState = indicatorState,
                                indicatorBrightness = indicatorBrightness
                            )
                        }
                    }
                }
            }
            if (gaiaPacketResponses.isEmpty()) {
                postSideEffect(StateSideEffect.Characteristic.Changed)
            }
        }
    }

    fun handleGaiaPacketSendResult(commandId: Int) = intent {
        gaiaPacketResponses.remove(commandId)
        if (gaiaPacketResponses.isEmpty()) {
            postSideEffect(StateSideEffect.Characteristic.Changed)
        }
    }

    fun reconnectToDevice(service: GaiaGattService?) = intent {
        postSideEffect(StateSideEffect.Reconnect.Initiated)
        if (service != null) {
            val success = service.connectToDevice(setupRepository.getBondedDeviceAddressOrEmpty())
            postSideEffect(
                if (success) {
                    StateSideEffect.Reconnect.Success
                } else {
                    StateSideEffect.Reconnect.Failure
                }
            )
        } else {
            postSideEffect(StateSideEffect.Reconnect.Failure)
        }
    }

    fun sendGaiaPacketIndicatorBrightness(
        scope: CoroutineScope,
        service: GaiaGattService?,
        indicatorBrightness: Int
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_INDICATOR_RGB_LIGHTING_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(
                    commandId = commandId,
                    payload = byteArrayOf(
                        state.indicatorState.id.toByte(),
                        indicatorBrightness.toByte()
                    )
                )
                val success = service.sendGaiaPacket(packet)
                if (success) {
                    reduce {
                        state.copy(indicatorBrightness = indicatorBrightness)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketIndicatorState(
        scope: CoroutineScope,
        service: GaiaGattService?,
        indicatorState: IndicatorState
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_INDICATOR_RGB_LIGHTING_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(
                    commandId = commandId,
                    payload = byteArrayOf(
                        indicatorState.id.toByte(),
                        state.indicatorBrightness.toByte()
                    )
                )
                val success = service.sendGaiaPacket(packet)
                if (success) {
                    reduce {
                        state.copy(indicatorState = indicatorState)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketMqa(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_MQA_EN_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(
                    commandId = commandId,
                    payload = byteArrayOf(
                        if (state.isMqaEnabled) {
                            0
                        } else {
                            1
                        }
                    )
                )
                val success = service.sendGaiaPacket(packet)
                if (success) {
                    reduce {
                        state.copy(isMqaEnabled = !state.isMqaEnabled)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketMute(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_MUTE_EN_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(
                    commandId = commandId,
                    payload = byteArrayOf(
                        if (state.isMuted) {
                            0
                        } else {
                            1
                        }
                    )
                )
                val success = service.sendGaiaPacket(packet)
                if (success) {
                    reduce {
                        state.copy(isMuted = !state.isMuted)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketReset(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_RESTORE_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                service.sendGaiaPacket(packet)
            }
        }
    }

    fun sendGaiaPacketStandby(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandId = GaiaPacketFactory.CMD_ID_STANDBY_SET
            gaiaPacketResponses.add(commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                service.sendGaiaPacket(packet)
            }
        }
    }

    fun sendGaiaPacketsDelayed(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val commandIds = listOf(
                GaiaPacketFactory.CMD_ID_MUTE_EN_GET,
                GaiaPacketFactory.CMD_ID_MQA_EN_GET,
                GaiaPacketFactory.CMD_ID_VERSION_GET,
                GaiaPacketFactory.CMD_ID_AUDIO_FMT_GET,
                GaiaPacketFactory.CMD_ID_INPUT_SRC_GET,
                GaiaPacketFactory.CMD_ID_INDICATOR_RGB_LIGHTING_GET
            )
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                commandIds.forEachIndexed { index, commandId ->
                    val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                    service.sendGaiaPacket(packet)
                    if (index < commandIds.lastIndex) {
                        delay(200)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketsInputSource(
        scope: CoroutineScope,
        service: GaiaGattService?,
        inputSource: InputSource
    ) = intent {
        if (service != null) {
            val commandIds = mutableListOf(
                GaiaPacketFactory.CMD_ID_INPUT_SRC_SET,
                GaiaPacketFactory.CMD_ID_VOLUME_GET
            )
            if (inputSource == InputSource.Bluetooth) {
                commandIds.add(GaiaPacketFactory.CMD_ID_CODEC_BIT_GET)
            }
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packets = mutableListOf(
                    GaiaPacketFactory.createGaiaPacket(
                        commandId = GaiaPacketFactory.CMD_ID_INPUT_SRC_SET,
                        payload = byteArrayOf(inputSource.id.toByte())
                    ),
                    GaiaPacketFactory.createGaiaPacket(
                        commandId = GaiaPacketFactory.CMD_ID_VOLUME_GET
                    )
                )
                if (inputSource == InputSource.Bluetooth) {
                    GaiaPacketFactory.createGaiaPacket(
                        commandId = GaiaPacketFactory.CMD_ID_CODEC_BIT_GET
                    )
                }
                packets.forEach { packet ->
                    val success = service.sendGaiaPacket(packet)
                    if (!success) {
                        return@launch
                    }
                }
                reduce {
                    state.copy(inputSource = inputSource)
                }
            }
        }
    }
}
