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

package com.tomg.fiiok9control.state.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.GAIA_CMD_DELAY_MS
import com.tomg.fiiok9control.VOLUME_MAX
import com.tomg.fiiok9control.VOLUME_MIN
import com.tomg.fiiok9control.VOLUME_STEP_SIZE
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.gaia.GaiaPacketFactory
import com.tomg.fiiok9control.gaia.fiio.K9Command
import com.tomg.fiiok9control.gaia.fiio.K9PacketDecoder
import com.tomg.fiiok9control.gaia.fiio.K9PacketFactory
import com.tomg.fiiok9control.gaia.isFiioPacket
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.profile.data.ProfileRepository
import com.tomg.fiiok9control.setup.data.SetupRepository
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
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
    private val profileRepository: ProfileRepository,
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

    fun disconnect(service: GaiaGattService?) = intent {
        if (service != null) {
            postSideEffect(StateSideEffect.Disconnect)
            service.disconnectAndReset()
        }
    }

    fun exportStateProfile(
        profileName: String?,
        exportVolume: Boolean
    ) = intent {
        if (profileName.isNullOrEmpty()) {
            postSideEffect(StateSideEffect.ExportProfile.Failure)
            return@intent
        }
        reduce {
            state.copy(isProfileExporting = true)
        }
        val success = profileRepository.insertProfile(
            Profile(
                name = profileName,
                inputSource = state.inputSource,
                indicatorState = state.indicatorState,
                indicatorBrightness = state.indicatorBrightness,
                volume = if (exportVolume) state.volume else Int.MIN_VALUE
            )
        )
        reduce {
            state.copy(isProfileExporting = false)
        }
        postSideEffect(
            if (success) {
                StateSideEffect.ExportProfile.Success
            } else {
                StateSideEffect.ExportProfile.Failure
            }
        )
    }

    fun handleGaiaPacket(data: ByteArray) = intent {
        val packet = GaiaPacketBLE(data)
        if (packet.isFiioPacket()) {
            gaiaPacketResponses.remove(packet.command)
            when (packet.command) {
                K9Command.Get.Version.commandId -> {
                    val version = K9PacketDecoder.decodePayloadGetVersion(packet.payload)
                    if (!version.isNullOrEmpty()) {
                        reduce {
                            state.copy(fwVersion = version)
                        }
                    }
                }
                K9Command.Get.AudioFormat.commandId -> {
                    val audioFormat = K9PacketDecoder.decodePayloadGetAudioFormat(packet.payload)
                    if (!audioFormat.isNullOrEmpty()) {
                        reduce {
                            state.copy(audioFmt = audioFormat)
                        }
                    }
                }
                K9Command.Get.Volume.commandId -> {
                    val volume = K9PacketDecoder.decodePayloadGetVolume(packet.payload)
                    reduce {
                        state.copy(
                            volume = volume.first,
                            volumePercent = volume.second
                        )
                    }
                }
                K9Command.Get.MuteEnabled.commandId -> {
                    val isMuted = K9PacketDecoder.decodePayloadGetMuteEnabled(packet.payload)
                    reduce {
                        state.copy(isMuted = isMuted)
                    }
                }
                K9Command.Get.MqaEnabled.commandId -> {
                    val isMqaEnabled = K9PacketDecoder.decodePayloadGetMqaEnabled(packet.payload)
                    reduce {
                        state.copy(isMqaEnabled = isMqaEnabled)
                    }
                }
                K9Command.Get.InputSource.commandId -> {
                    val inputSource = K9PacketDecoder.decodePayloadGetInputSource(packet.payload)
                    if (inputSource != null) {
                        reduce {
                            state.copy(inputSource = inputSource)
                        }
                    }
                }
                K9Command.Get.IndicatorRgbLighting.commandId -> {
                    val indicatorRgbLighting =
                        K9PacketDecoder.decodePayloadGetIndicatorRgbLighting(packet.payload)
                    if (indicatorRgbLighting != null) {
                        reduce {
                            state.copy(
                                indicatorState = indicatorRgbLighting.first,
                                indicatorBrightness = indicatorRgbLighting.second
                            )
                        }
                    }
                }
                K9Command.Get.Simultaneously.commandId -> {
                    val isHpPreSimultaneously =
                        K9PacketDecoder.decodePayloadGetHpPreSimultaneously(packet.payload)
                    reduce {
                        state.copy(isHpPreSimultaneously = isHpPreSimultaneously)
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
            val success = service.connect(setupRepository.getBondedDeviceAddressOrEmpty())
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

    fun sendGaiaPacketHpPreSimultaneously(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.Simultaneously.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetHpPreSimultaneously(
                        !state.isHpPreSimultaneously
                    )
                )
                if (success == true) {
                    reduce {
                        state.copy(isHpPreSimultaneously = !state.isHpPreSimultaneously)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.Simultaneously.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketVolume(
        scope: CoroutineScope,
        service: GaiaGattService?,
        volume: Int
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.Volume.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = K9PacketFactory.createGaiaPacketSetVolume(
                    maxOf(minOf(volume, VOLUME_MAX), VOLUME_MIN)
                )
                val success = service.sendGaiaPacket(packet)
                if (success == null || !success) {
                    gaiaPacketResponses.remove(K9Command.Set.Volume.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketVolume(
        scope: CoroutineScope,
        service: GaiaGattService?,
        volumeUp: Boolean
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.Volume.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                var volume = if (volumeUp) {
                    state.volume + VOLUME_STEP_SIZE
                } else {
                    state.volume - VOLUME_STEP_SIZE
                }
                volume = maxOf(minOf(volume, VOLUME_MAX), VOLUME_MIN)
                val packet = K9PacketFactory.createGaiaPacketSetVolume(volume)
                val success = service.sendGaiaPacket(packet)
                if (success == null || !success) {
                    gaiaPacketResponses.remove(K9Command.Set.Volume.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketIndicatorBrightness(
        scope: CoroutineScope,
        service: GaiaGattService?,
        indicatorBrightness: Int
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.IndicatorRgbLighting.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
                        indicatorState = state.indicatorState,
                        indicatorBrightness = indicatorBrightness
                    )
                )
                if (success == true) {
                    reduce {
                        state.copy(indicatorBrightness = indicatorBrightness)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.IndicatorRgbLighting.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
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
            gaiaPacketResponses.add(K9Command.Set.IndicatorRgbLighting.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
                        indicatorState = indicatorState,
                        indicatorBrightness = state.indicatorBrightness
                    )
                )
                if (success == true) {
                    reduce {
                        state.copy(indicatorState = indicatorState)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.IndicatorRgbLighting.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketMqa(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.MqaEnabled.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetMqa(state.isMqaEnabled)
                )
                if (success == true) {
                    reduce {
                        state.copy(isMqaEnabled = !state.isMqaEnabled)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.MqaEnabled.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketMuteEnabled(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.MuteEnabled.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetMuteEnabled(state.isMuted)
                )
                if (success == true) {
                    reduce {
                        state.copy(isMuted = !state.isMuted)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.MuteEnabled.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketRestore(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.Restore.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = K9PacketFactory.createGaiaPacketSetRestore()
                val success = service.sendGaiaPacket(packet)
                if (success == null || !success) {
                    gaiaPacketResponses.remove(K9Command.Set.Restore.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketStandby(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.Standby.commandId)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packet = K9PacketFactory.createGaiaPacketSetStandby()
                val success = service.sendGaiaPacket(packet)
                if (success == null || !success) {
                    gaiaPacketResponses.remove(K9Command.Set.Standby.commandId)
                    postSideEffect(StateSideEffect.Request.Failure(disconnected = success == null))
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
                K9Command.Get.MuteEnabled.commandId,
                K9Command.Get.MqaEnabled.commandId,
                K9Command.Get.Version.commandId,
                K9Command.Get.AudioFormat.commandId,
                K9Command.Get.Volume.commandId,
                K9Command.Get.Simultaneously.commandId,
                K9Command.Get.InputSource.commandId,
                K9Command.Get.IndicatorRgbLighting.commandId
            )
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                commandIds.forEachIndexed { index, commandId ->
                    val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                    val success = service.sendGaiaPacket(packet)
                    if (success == null || !success) {
                        gaiaPacketResponses.removeAll(commandIds.toSet())
                        postSideEffect(
                            StateSideEffect.Request.Failure(disconnected = success == null)
                        )
                        return@launch
                    }
                    if (index < commandIds.lastIndex) {
                        delay(GAIA_CMD_DELAY_MS)
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
                K9Command.Set.InputSource.commandId,
                K9Command.Get.Volume.commandId
            )
            if (inputSource == InputSource.Bluetooth) {
                commandIds.add(K9Command.Get.CodecBit.commandId)
            }
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(StateSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packets = mutableListOf(
                    K9PacketFactory.createGaiaPacketSetInputSource(inputSource),
                    K9PacketFactory.createGaiaPacketGetVolume()
                )
                if (inputSource == InputSource.Bluetooth) {
                    packets.add(K9PacketFactory.createGaiaPacketGetCodecBit())
                }
                packets.forEach { packet ->
                    val success = service.sendGaiaPacket(packet)
                    if (success == null || !success) {
                        gaiaPacketResponses.removeAll(commandIds.toSet())
                        postSideEffect(
                            StateSideEffect.Request.Failure(disconnected = success == null)
                        )
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
