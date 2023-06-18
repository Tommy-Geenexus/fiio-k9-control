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

package com.tomg.fiiok9control.profile.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.WINDOW_SIZE_EXPANDED_COLUMNS
import com.tomg.fiiok9control.WindowSizeClass
import com.tomg.fiiok9control.databinding.FragmentProfileBinding
import com.tomg.fiiok9control.gaia.business.GaiaGattSideEffect
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults
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
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            FiioK9Defaults.DISPLAY_NAME
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.profile.apply {
            layoutManager = if (getWindowSizeClass() == WindowSizeClass.Expanded) {
                StaggeredGridLayoutManager(WINDOW_SIZE_EXPANDED_COLUMNS, RecyclerView.VERTICAL)
            } else {
                LinearLayoutManager(context)
            }
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
                gaiaGattSideEffects.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
    }

    override fun bindLayout(view: View) = FragmentProfileBinding.bind(view)

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        if (profileViewModel.container.stateFlow.value.isServiceConnected) {
            profileViewModel.sendGaiaPacketsForProfile(requireGaiaGattService(), profile)
        }
    }

    override fun onServiceConnectionStateChanged(isConnected: Boolean) {
        profileViewModel.handleServiceConnectionStateChanged(isConnected)
    }

    override fun onProfileShortcutAdd(position: Int) {
        profileViewModel.container.stateFlow.value.profiles.getOrNull(position)?.let { profile ->
            profileViewModel.addProfileShortcut(profile)
        }
    }

    override fun onProfileShortcutRemove(position: Int) {
        profileViewModel.container.stateFlow.value.profiles.getOrNull(position)?.let { profile ->
            profileViewModel.deleteProfileShortcut(profile)
        }
    }

    override fun onProfileApply(position: Int) {
        profileViewModel.container.stateFlow.value.profiles.getOrNull(position)?.let { profile ->
            onProfileShortcutSelected(profile)
        }
    }

    override fun onProfileDelete(position: Int) {
        profileViewModel.container.stateFlow.value.profiles.getOrNull(position)?.let { profile ->
            profileViewModel.deleteProfile(profile)
        }
    }

    private fun renderState(state: ProfileState) {
        val isLoading =
            state.isAddingProfileShortcut ||
                state.isDeletingProfile ||
                state.isDeletingProfileShortcut ||
                state.isLoadingProfiles ||
                state.pendingCommands.isNotEmpty()
        binding.progress.isVisible = isLoading
        (binding.profile.adapter as? ProfileAdapter)?.submitList(state.profiles)
    }

    private fun handleSideEffect(sideEffect: ProfileSideEffect) {
        when (sideEffect) {
            ProfileSideEffect.Apply.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_apply_failure
                )
            }
            ProfileSideEffect.Apply.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_apply_success
                )
            }
            ProfileSideEffect.Delete.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_delete_failure
                )
            }
            ProfileSideEffect.Delete.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_delete_success
                )
            }
            is ProfileSideEffect.Select -> {
                onProfileShortcutSelected(sideEffect.profile)
            }
            ProfileSideEffect.Shortcut.Add.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.shortcut_profile_add_failure
                )
            }
            ProfileSideEffect.Shortcut.Add.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.shortcut_profile_add_success
                )
            }
            ProfileSideEffect.Shortcut.Delete.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.shortcut_profile_delete_failure
                )
            }
            ProfileSideEffect.Shortcut.Delete.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.shortcut_profile_delete_success
                )
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToStartDestination()
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                profileViewModel.handleCharacteristicWriteResult(sideEffect.packet)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                profileViewModel.handleCharacteristicWriteResult(sideEffect.packet)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                profileViewModel.handleCharacteristicWriteResult(sideEffect.packet)
            }
            else -> {
            }
        }
    }
}
