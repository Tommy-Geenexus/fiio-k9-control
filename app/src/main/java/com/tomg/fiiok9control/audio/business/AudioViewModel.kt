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

package com.tomg.fiiok9control.audio.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.gaia.data.GaiaGattRepository
import com.tomg.fiiok9control.gaia.data.GaiaPacketFactory
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Command
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9PacketDecoder
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9PacketFactory
import com.tomg.fiiok9control.gaia.ui.GaiaGattService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class AudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gaiaGattRepository: GaiaGattRepository
) : ViewModel(),
    ContainerHost<AudioState, AudioSideEffect> {

    override val container = container<AudioState, AudioSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = AudioState()
    )

    fun handleCharacteristicChanged(packet: GaiaPacket) = intent {
        when (packet.command) {
            FiioK9Command.Get.CodecEnabled.commandId -> {
                val codecsEnabled = FiioK9PacketDecoder.decodePayloadCodecEnabled(packet.payload)
                reduce {
                    state.copy(
                        codecsEnabled = codecsEnabled ?: state.codecsEnabled,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.LowPassFilter.commandId -> {
                val lowPassFilter = FiioK9PacketDecoder.decodePayloadLowPassFilter(packet.payload)
                reduce {
                    state.copy(
                        lowPassFilter = lowPassFilter ?: state.lowPassFilter,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.ChannelBalance.commandId -> {
                val channelBalance = FiioK9PacketDecoder.decodePayloadChannelBalance(packet.payload)
                reduce {
                    state.copy(
                        channelBalance = channelBalance ?: state.channelBalance,
                        pendingCommands = state.pendingCommands.minus(packet.command)
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
            FiioK9Command.Set.CodecEnabled.commandId -> {
                reduce {
                    state.copy(
                        codecsEnabled = if (success) {
                            state.pendingCodecsEnabled ?: state.codecsEnabled
                        } else {
                            state.codecsEnabled
                        },
                        pendingCodecsEnabled = null,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Set.LowPassFilter.commandId -> {
                reduce {
                    state.copy(
                        lowPassFilter = if (success) {
                            state.pendingLowPassFilter ?: state.lowPassFilter
                        } else {
                            state.lowPassFilter
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingLowPassFilter = null
                    )
                }
            }
            FiioK9Command.Set.ChannelBalance.commandId -> {
                reduce {
                    state.copy(
                        channelBalance = if (success) {
                            state.pendingChannelBalance ?: state.channelBalance
                        } else {
                            state.channelBalance
                        },
                        pendingChannelBalance = null,
                        pendingCommands = state.pendingCommands.minus(packet.command)
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

    fun sendGaiaPacketCodecsEnabled(
        service: GaiaGattService,
        codec: BluetoothCodec,
        enabled: Boolean
    ) = intent {
        val codecsEnabled = if (enabled) {
            state.codecsEnabled.plus(codec)
        } else {
            state.codecsEnabled.minus(codec)
        }
        val packet = FiioK9PacketFactory.createGaiaPacketSetCodecEnabled(codecsEnabled)
        reduce {
            state.copy(
                pendingCodecsEnabled = codecsEnabled,
                pendingCommands = state.pendingCommands.plus(packet.command)
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCodecsEnabled = null,
                    pendingCommands = state.pendingCommands.minus(packet.command)
                )
            }
        }
    }

    fun sendGaiaPacketLowPassFilter(
        service: GaiaGattService,
        lowPassFilter: LowPassFilter
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetLowPassFilter(lowPassFilter)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingLowPassFilter = lowPassFilter
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingLowPassFilter = null
                )
            }
        }
    }

    fun sendGaiaPacketChannelBalance(
        service: GaiaGattService,
        channelBalance: Int
    ) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetChannelBalance(channelBalance)
        reduce {
            state.copy(
                pendingChannelBalance = channelBalance,
                pendingCommands = state.pendingCommands.plus(packet.command)
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingChannelBalance = null,
                    pendingCommands = state.pendingCommands.minus(packet.command)
                )
            }
        }
    }

    fun sendGaiaPacketsDelayed(service: GaiaGattService) = intent {
        val commandIds = listOf(
            FiioK9Command.Get.CodecEnabled.commandId,
            FiioK9Command.Get.LowPassFilter.commandId,
            FiioK9Command.Get.ChannelBalance.commandId
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
}
