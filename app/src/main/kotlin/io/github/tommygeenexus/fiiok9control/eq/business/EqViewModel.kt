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

package io.github.tommygeenexus.fiiok9control.eq.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.tommygeenexus.fiiok9control.core.data.GaiaGattRepository
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Command
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9PacketDecoder
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9PacketFactory
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqPreSet
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqValue
import io.github.tommygeenexus.fiiok9control.core.gaia.GaiaPacketFactory
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattService
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class EqViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gaiaGattRepository: GaiaGattRepository
) : ViewModel(),
    ContainerHost<EqState, EqSideEffect> {

    override val container = container<EqState, EqSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = EqState()
    )

    fun handleCharacteristicChanged(packet: GaiaPacketBLE) = intent {
        when (packet.command) {
            FiioK9Command.Get.EqEnabled.commandId -> {
                val eqEnabled = FiioK9PacketDecoder.decodePayloadEqEnabled(packet.payload)
                reduce {
                    state.copy(
                        eqEnabled = eqEnabled ?: state.eqEnabled,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
            FiioK9Command.Get.EqPreSet.commandId -> {
                val eqPreSet = FiioK9PacketDecoder.decodePayloadEqPreSet(packet.payload)
                reduce {
                    state.copy(
                        eqPreSet = eqPreSet ?: state.eqPreSet,
                        pendingCommands = state.pendingCommands.minus(packet.command)
                    )
                }
            }
        }
    }

    fun handleCharacteristicWriteResult(packet: GaiaPacketBLE, isSuccess: Boolean) = intent {
        when (packet.command) {
            FiioK9Command.Set.EqEnabled.commandId -> {
                reduce {
                    state.copy(
                        eqEnabled = if (isSuccess) {
                            state.pendingEqEnabled ?: state.eqEnabled
                        } else {
                            state.eqEnabled
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingEqEnabled = null
                    )
                }
            }
            FiioK9Command.Set.EqPreSet.commandId -> {
                reduce {
                    state.copy(
                        eqPreSet = if (isSuccess) {
                            state.pendingEqPreSet ?: state.eqPreSet
                        } else {
                            state.eqPreSet
                        },
                        pendingCommands = state.pendingCommands.minus(packet.command),
                        pendingEqPreSet = null
                    )
                }
            }
        }
    }

    fun handleServiceConnectionStateChanged(isConnected: Boolean) = intent {
        reduce {
            state.copy(isServiceConnected = isConnected)
        }
    }

    fun sendGaiaPacketEqEnabled(service: GaiaGattService, isEqEnabled: Boolean) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetEqEnabled(isEqEnabled)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingEqEnabled = isEqEnabled
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingEqEnabled = null
                )
            }
        }
    }

    fun sendGaiaPacketEqPreSet(service: GaiaGattService, eqPreSet: EqPreSet) = intent {
        val packet = FiioK9PacketFactory.createGaiaPacketSetEqPreSet(eqPreSet)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingEqPreSet = eqPreSet
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        if (!success) {
            reduce {
                state.copy(
                    pendingCommands = state.pendingCommands.minus(packet.command),
                    pendingEqPreSet = null
                )
            }
        }
    }

    fun sendGaiaPacketEqValue(service: GaiaGattService, eqValue: EqValue) = intent {
        val eqValues = state.eqValues.map { v -> if (v.id == eqValue.id) eqValue else v }
        val packet = FiioK9PacketFactory.createGaiaPacketSetEqValue(eqValue)
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(packet.command),
                pendingEqValues = eqValues
            )
        }
        val success = gaiaGattRepository.sendGaiaPacket(service, packet)
        reduce {
            state.copy(
                eqValues = if (success) {
                    state.pendingEqValues ?: state.eqValues
                } else {
                    state.eqValues
                },
                pendingCommands = state.pendingCommands.minus(packet.command),
                pendingEqValues = null
            )
        }
    }

    fun sendGaiaPacketsDelayed(service: GaiaGattService) = intent {
        val commandIds = listOf(
            FiioK9Command.Get.EqEnabled.commandId,
            FiioK9Command.Get.EqPreSet.commandId
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

    fun sendGaiaPacketsEqValue(service: GaiaGattService) = intent {
        val eqValues = state.eqValues.map { eqValue -> eqValue.copy(value = 0f) }
        val commandIds = List(eqValues.size) { FiioK9Command.Set.EqValue.commandId }
        val packets = eqValues.map { eqValue ->
            FiioK9PacketFactory.createGaiaPacketSetEqValue(eqValue)
        }
        reduce {
            state.copy(
                pendingCommands = state.pendingCommands.plus(commandIds),
                pendingEqValues = eqValues
            )
        }
        val success = gaiaGattRepository.sendGaiaPackets(service, packets)
        reduce {
            state.copy(
                eqValues = if (success) {
                    state.pendingEqValues ?: state.eqValues
                } else {
                    state.eqValues
                },
                pendingCommands = state.pendingCommands.minus(commandIds.toSet()),
                pendingEqValues = null
            )
        }
    }
}
