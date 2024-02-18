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

package io.github.tommygeenexus.fiiok9control.setup.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.receiver.BluetoothBondStateBroadcastReceiver
import io.github.tommygeenexus.fiiok9control.core.ui.BaseFragment
import io.github.tommygeenexus.fiiok9control.databinding.FragmentSetupBinding
import io.github.tommygeenexus.fiiok9control.setup.business.SetupSideEffect
import io.github.tommygeenexus.fiiok9control.setup.business.SetupState
import io.github.tommygeenexus.fiiok9control.setup.business.SetupViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupFragment : BaseFragment<FragmentSetupBinding>(R.layout.fragment_setup) {

    private companion object {

        const val TAG_PERMISSIONS = 0
        const val TAG_BLUETOOTH = 1
        const val TAG_SCAN = 2
        const val TAG_SCAN_IN_PROGRESS = 3
        const val TAG_CONNECTING = 4
    }

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        setupViewModel.handlePermissionsGrantResult(result)
    }
    private val openSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }
    private val enableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    private val setupViewModel: SetupViewModel by viewModels()
    private val bluetoothBondStateReceiver = BluetoothBondStateBroadcastReceiver(
        onDeviceBondStateChanged = { bonded ->
            setupViewModel.handleBluetoothBondStateChange(bonded)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.app_name)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.action.setOnClickListener { v ->
            when (v.tag) {
                TAG_PERMISSIONS -> {
                    openSettings.launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                    )
                }
                TAG_BLUETOOTH -> {
                    enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
                TAG_SCAN -> {
                    setupViewModel.startBleScan()
                }
                TAG_SCAN_IN_PROGRESS -> {
                    setupViewModel.stopBleScan()
                }
                TAG_CONNECTING -> {
                    setupViewModel.disconnect(requireGaiaGattService())
                }
                else -> {
                    setupViewModel.connect(requireGaiaGattService())
                }
            }
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
                gaiaGattSideEffects.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupViewModel.bleScanResults.collect { scanResult ->
                    setupViewModel.stopBleScan()
                    if (!scanResult.isScanFailed) {
                        setupViewModel.handleScanResult(scanResult)
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            requireContext(),
            bluetoothBondStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(bluetoothBondStateReceiver)
    }

    override fun bindLayout(view: View) = FragmentSetupBinding.bind(view)

    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        setupViewModel.handleBluetoothStateChange(isEnabled)
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        setupViewModel.handleShortcutProfileSelected(profile)
    }

    override fun onServiceConnectionStateChanged(isConnected: Boolean) {
        setupViewModel.handleServiceConnectionStateChanged(isConnected)
    }

    private fun renderState(state: SetupState) {
        binding.action.apply {
            if (!state.isPermissionsGranted) {
                tag = TAG_PERMISSIONS
                setText(R.string.grant_permissions)
            } else if (!state.isBluetoothEnabled) {
                tag = TAG_BLUETOOTH
                setText(R.string.enable_bluetooth)
            } else if (state.deviceAddress.isEmpty()) {
                if (state.isScanning) {
                    tag = TAG_SCAN_IN_PROGRESS
                    setText(R.string.device_scan_abort)
                } else {
                    tag = TAG_SCAN
                    setText(R.string.device_scan_start)
                }
            } else if (state.isConnecting) {
                tag = TAG_CONNECTING
                setText(R.string.connection_abort)
            } else {
                tag = null
                text = getString(R.string.connect_to_device, state.deviceAddress)
            }
            isEnabled = state.isServiceConnected
        }
        binding.progress.isVisible = state.isConnecting || state.isDisconnecting || state.isScanning
    }

    private fun handleSideEffect(sideEffect: SetupSideEffect) {
        when (sideEffect) {
            SetupSideEffect.Ble.Unsupported -> {
                requireActivity().finish()
            }
            is SetupSideEffect.GrantPermissions -> {
                requestPermissions.launch(sideEffect.requiredPermissions.toTypedArray())
            }
            is SetupSideEffect.NavigateToProfile -> {
                findNavController().navigate(
                    SetupFragmentDirections.setupToProfile(sideEffect.profile)
                )
            }
            SetupSideEffect.NavigateToState -> {
                findNavController().navigate(SetupFragmentDirections.setupToState())
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gaia.Ready -> {
                setupViewModel.handleConnectionEstablished()
            }

            GaiaGattSideEffect.Gatt.Error -> {
                setupViewModel.handleConnectionEstablishFailed()
            }
            is GaiaGattSideEffect.Gatt.Ready -> {
                setupViewModel.handleConnectionEstablishInProgress(sideEffect.deviceAddress)
                Snackbar
                    .make(requireView(), R.string.gaia_discover, Snackbar.LENGTH_SHORT)
                    .show()
            }
            is GaiaGattSideEffect.Gatt.ServiceDiscovery -> {
                setupViewModel.handleConnectionEstablishInProgress(sideEffect.deviceAddress)
                Snackbar
                    .make(requireView(), R.string.gatt_discover, Snackbar.LENGTH_SHORT)
                    .show()
            }
            GaiaGattSideEffect.Gatt.Disconnected -> {
                setupViewModel.handleConnectionEstablishFailed()
            }
            else -> {
            }
        }
    }
}
