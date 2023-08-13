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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.gaia.data.GaiaGattRepository
import com.tomg.fiiok9control.gaia.data.GaiaPacketFactory
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Command
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9PacketDecoder
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9PacketFactory
import com.tomg.fiiok9control.gaia.ui.GaiaGattService
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.profile.data.ProfileRepository
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class StateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val gaiaGattRepository: GaiaGattRepository
) : ViewModel(),
    ContainerHost<StateState, StateSideEffect> {

    override val container = container<StateState, StateSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = StateState()
    )

    fun disconnect(service: GaiaGattService) = intent {
        reduce {
            state.copy(isDisconnecting = true)
        }
        if (!service.isDeviceConnected()) {
            service.reset()
            postSideEffect(StateSideEffect.Disconnected)
        } else {
            service.disconnectAndReset()
        }
        reduce {
            state.copy(isDisconnecting = false)
        }
    }

    fun exportStateProfile(
        profileName: String,
        exportVolume: Boolean
    ) = intent {
        reduce {
            state.copy(isExportingProfile = true)
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
            state.copy(isExportingProfile = false)
        }
        postSideEffect(
            if (success) {
                StateSideEffect.ExportProfile.Success
            } else {
                StateSideEffect.ExportProfile.Failure
            }
        )
    }

    fun handleCharacteristicChanged(packet: GaiaPacket) = intent {
        when (packet.command) {
            FiioK9Command.Get.AudioFormat.commandId -> {
                val audioFormat = FiioK9PacketDecoder.decodePayloadAudioFormat(packet.payload)
                reduce {
                    state.copy(
                        audioFmt = audioFormat ?: state.audioFmt,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.IndicatorRgbLighting.commandId -> {
                val indicatorRgbLighting =
                    FiioK9PacketDecoder.decodePayloadIndicatorRgbLighting(packet.payload)
                reduce {
                    state.copy(
                        indicatorState = indicatorRgbLighting?.first ?: state.indicatorState,
                        indicatorBrightness = indicatorRgbLighting?.second
                            ?: state.indicatorBrightness,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.InputSource.commandId -> {
                val inputSource = FiioK9PacketDecoder.decodePayloadInputSource(packet.payload)
                reduce {
                    state.copy(
                        inputSource = inputSource ?: state.inputSource,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.MqaEnabled.commandId -> {
                val isMqaEnabled = FiioK9PacketDecoder.decodePayloadMqaEnabled(packet.payload)
                reduce {
                    state.copy(
                        isMqaEnabled = isMqaEnabled ?: state.isMqaEnabled,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.MuteEnabled.commandId -> {
                val isMuteEnabled = FiioK9PacketDecoder.decodePayloadMuteEnabled(packet.payload)
                reduce {
                    state.copy(
                        isMuteEnabled = isMuteEnabled ?: state.isMuteEnabled,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
                if (isMuteEnabled != null) {
                    postSideEffect(
                        StateSideEffect.NotifyVolume(
                            volume = state.volume,
                            isMuteEnabled = state.isMuteEnabled
                        )
                    )
                }
            }
            FiioK9Command.Get.Simultaneously.commandId -> {
                val isHpPreSimultaneouslyEnabled =
                    FiioK9PacketDecoder.decodePayloadHpPreSimultaneously(packet.payload)
                reduce {
                    state.copy(
                        isHpPreSimultaneouslyEnabled = isHpPreSimultaneouslyEnabled
                            ?: state.isHpPreSimultaneouslyEnabled,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.Version.commandId -> {
                val fwVersion = FiioK9PacketDecoder.decodePayloadVersion(packet.payload)
                reduce {
                    state.copy(
                        fwVersion = fwVersion ?: state.fwVersion,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.Volume.commandId -> {
                val volume = FiioK9PacketDecoder.decodePayloadVolume(packet.payload)
                reduce {
                    state.copy(
                        volume = volume ?: state.volume,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
                if (volume != null) {
                    postSideEffect(
                        StateSideEffect.NotifyVolume(
                            volume = state.volume,
                            isMuteEnabled = state.isMuteEnabled
                        )
                    )
                }
            }
        }
    }

    fun handleCharacteristicWriteResult(
        packet: GaiaPacketBLE,
        success: Boolean
    ) = intent {
        when (packet.command) {
            FiioK9Command.Set.IndicatorRgbLighting.commandId -> {
                reduce {
                    state.copy(
                        indicatorBrightness = if (success) {
                            state.pendingIndicatorBrightness ?: state.indicatorBrightness
                        } else {
                            state.indicatorBrightness
                        },
                        indicatorState = if (success) {
                            state.pendingIndicatorState ?: state.indicatorState
                        } else {
                            state.indicatorState
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingIndicatorBrightness = null,
                        pendingIndicatorState = null
                    )
                }
            }
            FiioK9Command.Set.InputSource.commandId -> {
                reduce {
                    state.copy(
                        inputSource = if (success) {
                            state.pendingInputSource ?: state.inputSource
                        } else {
                            state.inputSource
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingInputSource = null
                    )
                }
            }
            FiioK9Command.Set.MqaEnabled.commandId -> {
                reduce {
                    state.copy(
                        isMqaEnabled = if (success) {
                            state.pendingIsMqaEnabled ?: state.isMqaEnabled
                        } else {
                            state.isMqaEnabled
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingIsMqaEnabled = null
                    )
                }
            }
            FiioK9Command.Set.MuteEnabled.commandId -> {
                reduce {
                    state.copy(
                        isMuteEnabled = if (success) {
                            state.pendingIsMuteEnabled ?: state.isMuteEnabled
                        } else {
                            state.isMuteEnabled
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingIsMuteEnabled = null
                    )
                }
                if (success) {
                    postSideEffect(
                        StateSideEffect.NotifyVolume(
                            volume = state.volume,
                            isMuteEnabled = state.isMuteEnabled
                        )
                    )
                }
            }
            FiioK9Command.Set.Simultaneously.commandId -> {
                reduce {
                    state.copy(
                        isHpPreSimultaneouslyEnabled = if (success) {
                            state.pendingIsHpPreSimultaneouslyEnabled
                                ?: state.isHpPreSimultaneouslyEnabled
                        } else {
                            state.isHpPreSimultaneouslyEnabled
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingIsHpPreSimultaneouslyEnabled = null
                    )
                }
            }
            FiioK9Command.Set.Volume.commandId -> {
                val volume = if (success) {
                    state.pendingVolume ?: state.volume
                } else {
                    state.volume
                }
                reduce {
                    state.copy(
                        volume = volume,
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingVolume = null
                    )
                }
                if (success) {
                    postSideEffect(
                        StateSideEffect.NotifyVolume(
                            volume = state.volume,
                            isMuteEnabled = state.isMuteEnabled
                        )
                    )
                }
            }
        }
    }

    fun handleServiceConnectionStateChanged(connected: Boolean) = intent {
        reduce {
            state.copy(isServiceConnected = connected)
        }
    }

    fun handleVolumeStepSize(volumeStepSize: Int) = intent {
        reduce {
            state.copy(volumeStepSize = volumeStepSize)
        }
    }

    fun sendGaiaPacketHpPreSimultaneously(service: GaiaGattService) = intent {
        val isHpPreSimultaneouslyEnabled = !state.isHpPreSimultaneouslyEnabled
        val packet =
            FiioK9PacketFactory.createGaiaPacketSetHpPreSimultaneously(isHpPreSimultaneouslyEnabled)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingIsHpPreSimultaneouslyEnabled = isHpPreSimultaneouslyEnabled
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingIsHpPreSimultaneouslyEnabled = null
                )
            }
        }
    }

    fun sendGaiaPacketVolume(
        service: GaiaGattService,
        volume: Int
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetVolume(volume)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingVolume = volume
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingVolume = null
                )
            }
        }
    }

    fun sendGaiaPacketVolume(
        service: GaiaGattService,
        volumeUp: Boolean
    ) = intent {
        sendGaiaPacketVolume(
            service = service,
            volume = if (volumeUp) {
                state.volume + state.volumeStepSize
            } else {
                state.volume - state.volumeStepSize
            }
        )
    }

    fun sendGaiaPacketIndicatorBrightness(
        service: GaiaGattService,
        indicatorBrightness: Int
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
            indicatorState = state.indicatorState,
            indicatorBrightness = indicatorBrightness
        )
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingIndicatorBrightness = indicatorBrightness
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingIndicatorBrightness = null
                )
            }
        }
    }

    fun sendGaiaPacketIndicatorState(
        service: GaiaGattService,
        indicatorState: IndicatorState
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
            indicatorState = indicatorState,
            indicatorBrightness = state.indicatorBrightness
        )
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingIndicatorState = indicatorState
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingIndicatorState = null
                )
            }
        }
    }

    fun sendGaiaPacketInputSource(
        service: GaiaGattService,
        inputSource: InputSource
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetInputSource(inputSource)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingInputSource = inputSource
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingInputSource = null
                )
            }
        }
    }

    fun sendGaiaPacketMqa(service: GaiaGattService) = intent {
        val isMqaEnabled = !state.isMqaEnabled
        val packet = FiioK9PacketFactory.createGaiaPacketSetMqaEnabled(isMqaEnabled)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingIsMqaEnabled = isMqaEnabled
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingIsMqaEnabled = null
                )
            }
        }
    }

    fun sendGaiaPacketMuteEnabled(service: GaiaGattService) = intent {
        val isMuteEnabled = !state.isMuteEnabled
        val packet = FiioK9PacketFactory.createGaiaPacketSetMuteEnabled(isMuteEnabled)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingIsMuteEnabled = isMuteEnabled
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingIsMuteEnabled = null
                )
            }
        }
    }

    fun sendGaiaPacketRestore(service: GaiaGattService) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetRestore()
        reduce {
            state.copy(pendingCommands = state.pendingCommands.plus(packet.command))
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(pendingCommands = state.pendingCommands.minus(packet.command))
            }
        }
    }

    fun sendGaiaPacketStandby(service: GaiaGattService) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetStandby()
        reduce {
            state.copy(pendingCommands = state.pendingCommands.plus(packet.command))
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(pendingCommands = state.pendingCommands.minus(packet.command))
            }
        }
    }

    fun sendGaiaPacketsDelayed(service: GaiaGattService) = intent {
        val commandIds = listOf(
            FiioK9Command.Get.MuteEnabled.commandId,
            FiioK9Command.Get.MqaEnabled.commandId,
            FiioK9Command.Get.Version.commandId,
            FiioK9Command.Get.AudioFormat.commandId,
            FiioK9Command.Get.Volume.commandId,
            FiioK9Command.Get.Simultaneously.commandId,
            FiioK9Command.Get.InputSource.commandId,
            FiioK9Command.Get.IndicatorRgbLighting.commandId
        )
        val packets = commandIds.map { commandId ->
            GaiaPacketFactory.createGaiaPacket(commandId = commandId)
        }
        reduce {
            state.copy(pendingCommands = state.pendingCommands.plus(commandIds))
        }
        val success = gaiaGattRepository.sendGaiaPackets(service, packets)
        if (!success) {
            reduce {
                state.copy(pendingCommands = state.pendingCommands.minus(commandIds.toSet()))
            }
        }
    }

    fun updatePendingIndicatorBrightness(indicatorBrightness: Int) = intent {
        reduce {
            state.copy(pendingIndicatorBrightness = indicatorBrightness)
        }
    }

    fun updatePendingVolumeLevel(volume: Int) = intent {
        reduce {
            state.copy(pendingVolume = volume)
        }
    }
}
