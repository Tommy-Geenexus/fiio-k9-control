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

package com.tomg.fiiok9control.profile.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.GAIA_CMD_DELAY_MS
import com.tomg.fiiok9control.VOLUME_MAX
import com.tomg.fiiok9control.VOLUME_MIN
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.gaia.fiio.K9Command
import com.tomg.fiiok9control.gaia.fiio.K9PacketFactory
import com.tomg.fiiok9control.gaia.isFiioPacket
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.profile.data.ProfileRepository
import com.tomg.fiiok9control.profile.ui.ProfileFragmentArgs
import com.tomg.fiiok9control.setup.data.SetupRepository
import com.tomg.fiiok9control.state.InputSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val setupRepository: SetupRepository
) : ViewModel(),
    ContainerHost<ProfileState, ProfileSideEffect> {

    override val container = container<ProfileState, ProfileSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = ProfileState(),
        onCreate = {
            loadProfiles(ProfileFragmentArgs.fromSavedStateHandle(savedStateHandle).profile)
        }
    )

    private val gaiaPacketResponses: MutableList<Int> = mutableListOf()

    fun clearGaiaPacketResponses() {
        gaiaPacketResponses.clear()
    }

    fun deleteProfile(profile: Profile) = intent {
        reduce {
            state.copy(areProfilesLoading = true)
        }
        val success = profileRepository.deleteProfile(profile)
        val profiles = state.profiles.toMutableList().apply {
            remove(profile)
        }
        if (success) {
            profileRepository.removeProfileShortcut(profile)
        }
        reduce {
            state.copy(
                profiles = if (success) profiles else state.profiles,
                areProfilesLoading = false
            )
        }
    }

    fun handleGaiaPacket(data: ByteArray) = intent {
        val packet = GaiaPacketBLE(data)
        if (packet.isFiioPacket()) {
            gaiaPacketResponses.remove(packet.command)
            if (gaiaPacketResponses.isEmpty()) {
                postSideEffect(ProfileSideEffect.Characteristic.Changed)
            }
        }
    }

    fun handleGaiaPacketSendResult(commandId: Int) = intent {
        gaiaPacketResponses.remove(commandId)
        if (gaiaPacketResponses.isEmpty()) {
            postSideEffect(ProfileSideEffect.Characteristic.Changed)
        }
    }

    private fun loadProfiles(profile: Profile?) = intent {
        reduce {
            state.copy(areProfilesLoading = true)
        }
        val profiles = profileRepository.getProfiles().firstOrNull()
        reduce {
            state.copy(
                profiles = profiles ?: emptyList(),
                areProfilesLoading = false
            )
        }
        if (profile != null) {
            postSideEffect(ProfileSideEffect.Shortcut.Selected(profile))
        }
    }

    fun reconnectToDevice(service: GaiaGattService?) = intent {
        postSideEffect(ProfileSideEffect.Reconnect.Initiated)
        val device = service?.device
        if (service != null && device != null) {
            val success = if (setupRepository.isDeviceBonded(device.address)) {
                service.connect(device.address)
            } else {
                false
            }
            postSideEffect(
                if (success) {
                    ProfileSideEffect.Reconnect.Success
                } else {
                    ProfileSideEffect.Reconnect.Failure
                }
            )
        } else {
            postSideEffect(ProfileSideEffect.Reconnect.Failure)
        }
    }

    fun sendGaiaPacketsForProfile(
        scope: CoroutineScope,
        service: GaiaGattService?,
        profile: Profile
    ) = intent {
        if (service != null) {
            val commandIds = mutableListOf(
                K9Command.Set.InputSource.commandId,
                K9Command.Get.Volume.commandId
            )
            if (profile.inputSource == InputSource.Bluetooth) {
                commandIds.add(K9Command.Get.CodecBit.commandId)
            }
            commandIds.add(K9Command.Set.IndicatorRgbLighting.commandId)
            gaiaPacketResponses.addAll(commandIds)
            postSideEffect(ProfileSideEffect.Characteristic.Write)
            scope.launch(context = Dispatchers.IO) {
                val packets = mutableListOf(
                    K9PacketFactory.createGaiaPacketSetInputSource(profile.inputSource),
                    K9PacketFactory.createGaiaPacketGetVolume()
                )
                if (profile.inputSource == InputSource.Bluetooth) {
                    packets.add(K9PacketFactory.createGaiaPacketGetCodecBit())
                }
                packets.add(
                    K9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
                        indicatorState = profile.indicatorState,
                        indicatorBrightness = profile.indicatorBrightness
                    )
                )
                if (IntRange(VOLUME_MIN, VOLUME_MAX).contains(profile.volume)) {
                    packets.add(K9PacketFactory.createGaiaPacketSetVolume(profile.volume))
                }
                packets.forEachIndexed { index, packet ->
                    val success = service.sendGaiaPacket(packet)
                    if (success == null || !success) {
                        gaiaPacketResponses.removeAll(commandIds.toSet())
                        postSideEffect(
                            ProfileSideEffect.Request.Failure(disconnected = success == null)
                        )
                        return@launch
                    }
                    if (index < packets.lastIndex) {
                        delay(GAIA_CMD_DELAY_MS)
                    }
                }
            }
        }
    }

    fun addProfileShortcut(profile: Profile) = intent {
        val success = profileRepository.addProfileShortcut(profile)
        if (success) {
            postSideEffect(ProfileSideEffect.Shortcut.Added)
        }
    }

    fun removeProfileShortcut(profile: Profile) = intent {
        val success = profileRepository.removeProfileShortcut(profile)
        if (success) {
            postSideEffect(ProfileSideEffect.Shortcut.Removed)
        }
    }
}
