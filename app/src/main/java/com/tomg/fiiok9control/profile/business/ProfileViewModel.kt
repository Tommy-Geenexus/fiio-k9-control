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

package com.tomg.fiiok9control.profile.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import com.tomg.fiiok9control.gaia.data.GaiaGattRepository
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Command
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9PacketFactory
import com.tomg.fiiok9control.gaia.ui.GaiaGattService
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.profile.data.ProfileRepository
import com.tomg.fiiok9control.profile.ui.ProfileFragmentArgs
import com.tomg.fiiok9control.state.InputSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val gaiaGattRepository: GaiaGattRepository,
    private val profileRepository: ProfileRepository
) : ViewModel(),
    ContainerHost<ProfileState, ProfileSideEffect> {

    override val container = container<ProfileState, ProfileSideEffect>(
        initialState = ProfileState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            loadProfiles(ProfileFragmentArgs.fromSavedStateHandle(savedStateHandle).profile)
        }
    )

    fun addProfileShortcut(profile: Profile) = intent {
        reduce {
            state.copy(isAddingProfileShortcut = true)
        }
        val success = profileRepository.addProfileShortcut(profile)
        reduce {
            state.copy(isAddingProfileShortcut = false)
        }
        postSideEffect(
            if (success) {
                ProfileSideEffect.Shortcut.Add.Success
            } else {
                ProfileSideEffect.Shortcut.Add.Failure
            }
        )
    }

    fun deleteProfileShortcut(profile: Profile) = intent {
        reduce {
            state.copy(isDeletingProfileShortcut = true)
        }
        val success = profileRepository.deleteProfileShortcut(profile)
        reduce {
            state.copy(isDeletingProfileShortcut = false)
        }
        postSideEffect(
            if (success) {
                ProfileSideEffect.Shortcut.Delete.Success
            } else {
                ProfileSideEffect.Shortcut.Delete.Failure
            }
        )
    }

    fun deleteProfile(profile: Profile) = intent {
        reduce {
            state.copy(isDeletingProfile = true)
        }
        val success = profileRepository.deleteProfile(profile)
        val profiles = state.profiles.toMutableList().apply {
            remove(profile)
        }
        if (success) {
            profileRepository.deleteProfileShortcut(profile)
        }
        reduce {
            state.copy(
                profiles = if (success) profiles else state.profiles,
                isDeletingProfile = false
            )
        }
        postSideEffect(
            if (success) {
                ProfileSideEffect.Delete.Success
            } else {
                ProfileSideEffect.Delete.Failure
            }
        )
    }

    fun handleCharacteristicWriteResult(packet: GaiaPacketBLE) = intent {
        reduce {
            state.copy(pendingCommands = state.pendingCommands.minus(packet.command))
        }
    }

    fun handleServiceConnectionStateChanged(connected: Boolean) = intent {
        reduce {
            state.copy(isServiceConnected = connected)
        }
    }

    private fun loadProfiles(profile: Profile?) = intent {
        reduce {
            state.copy(isLoadingProfiles = true)
        }
        val profiles = profileRepository.getProfiles()
        reduce {
            state.copy(
                profiles = profiles,
                isLoadingProfiles = false
            )
        }
        if (profile != null) {
            postSideEffect(ProfileSideEffect.Select(profile))
        }
    }

    fun sendGaiaPacketsForProfile(
        service: GaiaGattService,
        profile: Profile
    ) = intent {
        val commandIds = if (profile.inputSource == InputSource.Bluetooth) {
            listOf(
                FiioK9Command.Set.InputSource.commandId,
                FiioK9Command.Get.Volume.commandId,
                FiioK9Command.Get.CodecBit.commandId
            )
        } else {
            listOf(
                FiioK9Command.Set.InputSource.commandId,
                FiioK9Command.Get.Volume.commandId
            )
        }
        reduce {
            state.copy(pendingCommands = state.pendingCommands.plus(commandIds))
        }
        val packets = mutableListOf(
            FiioK9PacketFactory.createGaiaPacketSetInputSource(profile.inputSource),
            FiioK9PacketFactory.createGaiaPacketGetVolume()
        )
        if (profile.inputSource == InputSource.Bluetooth) {
            packets.add(FiioK9PacketFactory.createGaiaPacketGetCodecBit())
        }
        packets.add(
            FiioK9PacketFactory.createGaiaPacketSetIndicatorStateAndBrightness(
                indicatorState = profile.indicatorState,
                indicatorBrightness = profile.indicatorBrightness
            )
        )
        if (IntRange(
                FiioK9Defaults.VOLUME_LEVEL_MIN,
                FiioK9Defaults.VOLUME_LEVEL_MAX
            ).contains(profile.volume)
        ) {
            packets.add(FiioK9PacketFactory.createGaiaPacketSetVolume(profile.volume))
        }
        val success = gaiaGattRepository.sendGaiaPackets(service, packets)
        if (!success) {
            reduce {
                state.copy(pendingCommands = state.pendingCommands.minus(commandIds.toSet()))
            }
        }
    }
}
