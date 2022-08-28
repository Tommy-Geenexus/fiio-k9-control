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

package com.tomg.fiiok9control.profile.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentProfileBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import com.tomg.fiiok9control.profile.business.ProfileSideEffect
import com.tomg.fiiok9control.profile.business.ProfileState
import com.tomg.fiiok9control.profile.business.ProfileViewModel
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment :
    BaseFragment<FragmentProfileBinding>(R.layout.fragment_profile),
    ProfileAdapter.Listener {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.progress2.setVisibilityAfterHide(View.GONE)
        binding.profile.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ProfileAdapter(listener = this@ProfileFragment)
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.container.sideEffectFlow.collect { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                gaiaGattSideEffectFlow.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileViewModel.clearGaiaPacketResponses()
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        profileViewModel.sendGaiaPacketsForProfile(
            lifecycleScope,
            gaiaGattService(),
            profile
        )
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onReconnectToDevice() {
        profileViewModel.reconnectToDevice(gaiaGattService())
    }

    override fun bindLayout(view: View) = FragmentProfileBinding.bind(view)

    override fun onProfileShortcutAdd(position: Int) {
        val profile = (binding.profile.adapter as? ProfileAdapter)?.currentList?.getOrNull(position)
        if (profile != null) {
            profileViewModel.addProfileShortcut(profile)
        }
    }

    override fun onProfileShortcutRemove(position: Int) {
        val profile = (binding.profile.adapter as? ProfileAdapter)?.currentList?.getOrNull(position)
        if (profile != null) {
            profileViewModel.removeProfileShortcut(profile)
        }
    }

    override fun onProfileApply(position: Int) {
        val profile = (binding.profile.adapter as? ProfileAdapter)?.currentList?.getOrNull(position)
        if (profile != null) {
            profileViewModel.sendGaiaPacketsForProfile(
                lifecycleScope,
                gaiaGattService(),
                profile
            )
        }
    }

    override fun onProfileDelete(position: Int) {
        val profile = (binding.profile.adapter as? ProfileAdapter)?.currentList?.getOrNull(position)
        if (profile != null) {
            profileViewModel.deleteProfile(profile)
        }
    }

    private fun renderState(state: ProfileState) {
        if (state.areProfilesLoading) {
            binding.progress2.show()
        } else {
            binding.progress2.hide()
        }
        (binding.profile.adapter as? ProfileAdapter)?.submitList(state.profiles)
    }

    private fun handleSideEffect(sideEffect: ProfileSideEffect) {
        when (sideEffect) {
            ProfileSideEffect.Characteristic.Changed -> {
                binding.progress.hide()
            }
            ProfileSideEffect.Characteristic.Write -> {
                binding.progress.show()
            }
            ProfileSideEffect.Reconnect.Failure -> {
                binding.progress.hide()
                onBluetoothStateChanged(false)
            }
            ProfileSideEffect.Reconnect.Initiated -> {
                binding.progress.show()
            }
            ProfileSideEffect.Reconnect.Success -> {
                binding.progress.hide()
            }
            is ProfileSideEffect.Request.Failure -> {
                binding.progress.hide()
                if (sideEffect.disconnected) {
                    onBluetoothStateChanged(false)
                }
            }
            ProfileSideEffect.Shortcut.Added -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav),
                    msgRes = R.string.shortcut_profile_added
                )
            }
            ProfileSideEffect.Shortcut.Removed -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav),
                    msgRes = R.string.shortcut_profile_removed
                )
            }
            is ProfileSideEffect.Shortcut.Selected -> {
                onProfileShortcutSelected(sideEffect.profile)
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect?) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                onBluetoothStateChanged(false)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure -> {
                profileViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Success -> {
                profileViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gaia.Packet -> {
                profileViewModel.handleGaiaPacket(sideEffect.data)
            }
            else -> {
            }
        }
    }
}
