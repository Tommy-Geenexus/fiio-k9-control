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
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
import com.tomg.fiiok9control.setup.data.BleScanResult
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
    private val setupViewModel: SetupViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.action.setOnClickListener {
            handleActionClick()
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
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupViewModel.scannedDeviceFlow.collect { scanResult ->
                    handleScanResult(scanResult)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupViewModel.synchronizeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setupViewModel.stopBleScan()
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        setupViewModel.handleShortcutProfileSelected(profile)
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
            binding.action.setText(
                if (state.isLoading) R.string.device_scan_abort else R.string.device_scan_start
            )
        } else if (!state.bonded) {
            binding.action.setText(R.string.pair_device)
        } else if (state.isLoading) {
            binding.action.setText(R.string.connection_abort)
        } else {
            binding.action.text = getString(R.string.connect_to_device, state.deviceAddress)
        }
        if (state.isLoading) {
            binding.progress.show()
            binding.action.isEnabled =
                binding.action.text == getString(R.string.connection_abort) ||
                binding.action.text == getString(R.string.device_scan_abort)
        } else {
            binding.progress.hide()
            binding.action.isEnabled = true
        }
    }

    private fun handleSideEffect(sideEffect: SetupSideEffect) {
        when (sideEffect) {
            SetupSideEffect.Ble.StartScan -> {
                setupViewModel.startBleScan()
            }
            SetupSideEffect.Ble.Unsupported -> {
                requireActivity().finish()
            }
            is SetupSideEffect.GrantPermissions -> {
                requestPermissions.launch(sideEffect.requiredPermissions.toTypedArray())
            }
            is SetupSideEffect.NavigateToProfile -> {
                navigate(SetupFragmentDirections.setupToProfile(sideEffect.profile))
            }
            SetupSideEffect.NavigateToState -> {
                navigate(SetupFragmentDirections.setupToState())
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect?) {
        when (sideEffect) {
            GaiaGattSideEffect.Gaia.Ready -> {
                setupViewModel.handleConnectionEstablished()
            }
            GaiaGattSideEffect.Gatt.Error -> {
                setupViewModel.handleConnectionEstablishFailed()
            }
            is GaiaGattSideEffect.Gatt.Ready -> {
                setupViewModel.handleConnectionEstablishInProgress(sideEffect.deviceAddress)
                requireView().showSnackbar(msgRes = R.string.gaia_discover)
            }
            is GaiaGattSideEffect.Gatt.ServiceDiscovery -> {
                setupViewModel.handleConnectionEstablishInProgress(sideEffect.deviceAddress)
                requireView().showSnackbar(msgRes = R.string.gatt_discover)
            }
            GaiaGattSideEffect.Gatt.Disconnected -> {
                setupViewModel.handleConnectionEstablishFailed()
            }
            else -> {
            }
        }
    }

    private fun handleScanResult(scanResult: Result<BleScanResult>) {
        setupViewModel.stopBleScan()
        if (scanResult.isSuccess) {
            setupViewModel.handleDeviceScanned(scanResult.getOrDefault(BleScanResult()))
        }
    }

    private fun handleActionClick() {
        when (binding.action.text) {
            getString(R.string.grant_permissions) -> {
                openSettings.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", requireContext().packageName, null)
                    }
                )
            }
            getString(R.string.enable_bluetooth),
            getString(R.string.pair_device) -> {
                openSettings.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
            getString(R.string.device_scan_start) -> {
                setupViewModel.startBleScan()
            }
            getString(R.string.device_scan_abort) -> {
                setupViewModel.stopBleScan()
            }
            getString(R.string.connection_abort) -> {
                setupViewModel.disconnect(gaiaGattService())
            }
            else -> {
                setupViewModel.connectToDevice(
                    lifecycleScope,
                    gaiaGattService()
                )
            }
        }
    }
}
