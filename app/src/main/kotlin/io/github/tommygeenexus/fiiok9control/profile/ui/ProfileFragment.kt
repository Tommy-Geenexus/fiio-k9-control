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

package io.github.tommygeenexus.fiiok9control.profile.ui

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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.ui.BaseFragment
import io.github.tommygeenexus.fiiok9control.core.ui.RecyclerViewItemDecoration
import io.github.tommygeenexus.fiiok9control.core.util.WINDOW_SIZE_EXPANDED_COLUMNS
import io.github.tommygeenexus.fiiok9control.databinding.FragmentProfileBinding
import io.github.tommygeenexus.fiiok9control.profile.business.ProfileSideEffect
import io.github.tommygeenexus.fiiok9control.profile.business.ProfileState
import io.github.tommygeenexus.fiiok9control.profile.business.ProfileViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment :
    BaseFragment<FragmentProfileBinding>(R.layout.fragment_profile),
    ProfileAdapter.Listener {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            FiioK9Defaults.DISPLAY_NAME
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.profile.apply {
            addItemDecoration(
                RecyclerViewItemDecoration(
                    margin = resources.getDimension(R.dimen.spacing_small).toInt(),
                    isLtr = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
                )
            )
            layoutManager = if (isWidthAtLeastBreakpointExpandedLowerBound()) {
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

    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            if (profileViewModel.container.stateFlow.value.isServiceConnected) {
                requireGaiaGattService().reset()
            }
            navigateToSetup()
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
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_apply_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Apply.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_apply_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Delete.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_delete_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Delete.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_delete_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            is ProfileSideEffect.Select -> {
                onProfileShortcutSelected(sideEffect.profile)
            }
            ProfileSideEffect.Shortcut.Add.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.shortcut_profile_add_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Shortcut.Add.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.shortcut_profile_add_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Shortcut.Delete.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.shortcut_profile_delete_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            ProfileSideEffect.Shortcut.Delete.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.shortcut_profile_delete_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToSetup()
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
