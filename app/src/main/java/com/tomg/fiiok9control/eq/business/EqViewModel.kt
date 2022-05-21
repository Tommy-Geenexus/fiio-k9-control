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

package com.tomg.fiiok9control.eq.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.eq.EqPreSet
import com.tomg.fiiok9control.eq.EqValue
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
class EqViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setupRepository: SetupRepository
) : ViewModel(),
    ContainerHost<EqState, EqSideEffect> {

    override val container = container<EqState, EqSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = EqState()
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
                K9Command.Get.EqEnabled.commandId -> {
                    val eqEnabled = K9PacketDecoder.decodePayloadGetEqEnabled(packet.payload)
                    if (eqEnabled != null) {
                        reduce {
                            state.copy(eqEnabled = eqEnabled)
                        }
                    }
                }
                K9Command.Get.EqPreSet.commandId -> {
                    val eqPreSet = K9PacketDecoder.decodePayloadGetEqPreSet(packet.payload)
                    if (eqPreSet != null) {
                        reduce {
                            state.copy(eqPreSet = eqPreSet)
                        }
                    }
                }
            }
            if (gaiaPacketResponses.isEmpty()) {
                postSideEffect(EqSideEffect.Characteristic.Changed)
            }
        }
    }

    fun handleGaiaPacketSendResult(commandId: Int) = intent {
        gaiaPacketResponses.remove(commandId)
        if (gaiaPacketResponses.isEmpty()) {
            postSideEffect(EqSideEffect.Characteristic.Changed)
        }
    }

    fun hasCustomEqValues(): Boolean {
        return container.stateFlow.value.eqValues.any { eqValue -> eqValue.value != 0f }
    }

    fun reconnectToDevice(service: GaiaGattService?) = intent {
        postSideEffect(EqSideEffect.Reconnect.Initiated)
        if (service != null) {
            val success = service.connect(setupRepository.getBondedDeviceAddressOrEmpty())
            postSideEffect(
                if (success) {
                    EqSideEffect.Reconnect.Success
                } else {
                    EqSideEffect.Reconnect.Failure
                }
            )
        } else {
            postSideEffect(EqSideEffect.Reconnect.Failure)
        }
    }

    fun sendGaiaPacketEqEnabled(
        scope: CoroutineScope,
        service: GaiaGattService?,
        eqEnabled: Boolean
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.EqEnabled.commandId)
            postSideEffect(EqSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetEqEnabled(eqEnabled)
                )
                if (success == true) {
                    reduce {
                        state.copy(eqEnabled = eqEnabled)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.EqEnabled.commandId)
                    postSideEffect(EqSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketEqPreSet(
        scope: CoroutineScope,
        service: GaiaGattService?,
        eqPreSet: EqPreSet
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.EqPreSet.commandId)
            postSideEffect(EqSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetEqPreSet(eqPreSet)
                )
                if (success == true) {
                    reduce {
                        state.copy(eqPreSet = eqPreSet)
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.EqEnabled.commandId)
                    postSideEffect(EqSideEffect.Request.Failure(disconnected = success == null))
                }
            }
        }
    }

    fun sendGaiaPacketEqValue(
        scope: CoroutineScope,
        service: GaiaGattService?,
        eqValue: EqValue
    ) = intent {
        if (service != null) {
            gaiaPacketResponses.add(K9Command.Set.EqValue.commandId)
            postSideEffect(EqSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val success = service.sendGaiaPacket(
                    K9PacketFactory.createGaiaPacketSetEqValue(eqValue)
                )
                if (success == true) {
                    reduce {
                        state.copy(
                            eqValues = state.eqValues.map { v ->
                                if (v.id == eqValue.id) eqValue else v
                            }
                        )
                    }
                } else {
                    gaiaPacketResponses.remove(K9Command.Set.EqEnabled.commandId)
                    postSideEffect(EqSideEffect.Request.Failure(disconnected = success == null))
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
                K9Command.Get.EqEnabled.commandId,
                K9Command.Get.EqPreSet.commandId
            )
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(EqSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                commandIds.forEachIndexed { index, commandId ->
                    val packet = GaiaPacketFactory.createGaiaPacket(commandId = commandId)
                    val success = service.sendGaiaPacket(packet)
                    if (success == null || !success) {
                        gaiaPacketResponses.removeAll(commandIds)
                        postSideEffect(EqSideEffect.Request.Failure(disconnected = success == null))
                        return@launch
                    }
                    if (index < commandIds.lastIndex) {
                        delay(200)
                    }
                }
            }
        }
    }

    fun sendGaiaPacketsEqValue(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            val eqValues = state.eqValues.map { eqValue -> eqValue.copy(value = 0f) }
            val commandId = K9Command.Set.EqValue.commandId
            val commandIds = List(eqValues.size) { commandId }
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(EqSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                eqValues.forEach { eqValue ->
                    val success = service.sendGaiaPacket(
                        K9PacketFactory.createGaiaPacketSetEqValue(eqValue)
                    )
                    if (success == true) {
                        reduce {
                            state.copy(
                                eqValues = state.eqValues.map { v ->
                                    if (v.id == eqValue.id) eqValue.copy(value = 0f) else v
                                }
                            )
                        }
                    } else {
                        gaiaPacketResponses.removeAll(commandIds)
                        postSideEffect(EqSideEffect.Request.Failure(disconnected = success == null))
                        return@launch
                    }
                }
            }
        }
    }
}
