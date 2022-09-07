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

package com.tomg.fiiok9control.setup.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentSetupBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.setup.business.SetupSideEffect
import com.tomg.fiiok9control.setup.business.SetupState
import com.tomg.fiiok9control.setup.business.SetupViewModel
import com.tomg.fiiok9control.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupFragment : BaseFragment<FragmentSetupBinding>(R.layout.fragment_setup) {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        setupViewModel.handlePermissionsGrantResult(result)
    }
    private val openSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }
    private val requiredPermissions = arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT)
    private val setupViewModel: SetupViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(R.menu.menu_setup, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.cancel).isVisible = !binding.action.isEnabled
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.cancel -> {
                        setupViewModel.disconnect(gaiaGattService())
                        true
                    }
                    else -> false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.action.setOnClickListener {
            maybeConnectToDevice()
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupViewModel.container.sideEffectFlow.collect { sideEffect ->
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

    override fun onResume() {
        super.onResume()
        setupViewModel.checkIfPermissionsGrantedAndBluetoothEnabled(requiredPermissions)
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        setupViewModel.shortcutProfile = profile
        maybeConnectToDevice()
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        setupViewModel.handleBluetoothStateChange(enabled)
    }

    override fun onReconnectToDevice() {
    }

    override fun bindLayout(view: View) = FragmentSetupBinding.bind(view)

    private fun renderState(state: SetupState) {
        if (!state.permissionsGranted) {
            binding.action.setText(R.string.grant_permissions)
        } else if (!state.bluetoothEnabled) {
            binding.action.setText(R.string.enable_bluetooth)
        } else if (state.deviceAddress.isEmpty()) {
            binding.action.setText(R.string.pair_device)
        } else {
            binding.action.setText(R.string.connect_device)
        }
    }

    private fun handleSideEffect(sideEffect: SetupSideEffect) {
        when (sideEffect) {
            is SetupSideEffect.Ble.Supported -> {
                requestPermissions.launch(requiredPermissions)
            }
            SetupSideEffect.Ble.Unsupported -> {
                requireActivity().finish()
            }
            SetupSideEffect.Connection.Established -> {
                binding.progress.hide()
                navigate(SetupFragmentDirections.setupToState())
            }
            SetupSideEffect.Connection.Establishing -> {
                binding.progress.show()
                binding.action.isEnabled = false
                requireActivity().invalidateOptionsMenu()
            }
            SetupSideEffect.Connection.EstablishFailed -> {
                binding.progress.hide()
                binding.action.isEnabled = true
                requireActivity().invalidateOptionsMenu()
            }
            is SetupSideEffect.NavigateToProfile -> {
                navigate(SetupFragmentDirections.setupToProfile(sideEffect.profile))
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect?) {
        when (sideEffect) {
            GaiaGattSideEffect.Gaia.Ready -> {
                setupViewModel.handleConnectionEstablished()
            }
            GaiaGattSideEffect.Gatt.Error -> {
                binding.progress.hide()
                binding.action.isEnabled = true
                requireActivity().invalidateOptionsMenu()
            }
            GaiaGattSideEffect.Gatt.Ready -> {
                binding.progress.show()
                binding.action.isEnabled = false
                requireActivity().invalidateOptionsMenu()
                requireView().showSnackbar(msgRes = R.string.gaia_discover)
            }
            GaiaGattSideEffect.Gatt.ServiceDiscovery -> {
                binding.progress.show()
                binding.action.isEnabled = false
                requireActivity().invalidateOptionsMenu()
                requireView().showSnackbar(msgRes = R.string.gatt_discover)
            }
            GaiaGattSideEffect.Gatt.Disconnected -> {
                binding.progress.hide()
                binding.action.isEnabled = true
                requireActivity().invalidateOptionsMenu()
            }
            else -> {
            }
        }
    }

    private fun maybeConnectToDevice() {
        val currentState = setupViewModel.container.stateFlow.value
        if (!currentState.permissionsGranted) {
            openSettings.launch(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
            )
        } else if (!currentState.bluetoothEnabled || currentState.deviceAddress.isEmpty()) {
            openSettings.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } else {
            setupViewModel.connectToDevice(
                lifecycleScope,
                gaiaGattService(),
                currentState.deviceAddress
            )
        }
    }
}
