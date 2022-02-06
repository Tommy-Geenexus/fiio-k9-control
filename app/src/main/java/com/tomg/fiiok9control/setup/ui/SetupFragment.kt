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
import com.tomg.fiiok9control.KEY_EVENT
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentSetupBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
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
        setupToolbar(
            toolbar = binding.toolbar,
            titleRes = R.string.app_name
        )
        requireActivity().supportFragmentManager.setFragmentResultListener(
            KEY_EVENT,
            viewLifecycleOwner
        ) { _, args: Bundle ->
            handleGaiaGattSideEffect(args.getParcelable(KEY_EVENT))
        }
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
    }

    override fun onResume() {
        super.onResume()
        setupViewModel.checkIfPermissionsGrantedAndBluetoothEnabled(requiredPermissions)
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        setupViewModel.handleBluetoothStateChange(enabled)
    }

    override fun onReconnectToDevice() {
    }

    override fun bindLayout(view: View) = FragmentSetupBinding.bind(view)

    private fun renderState(state: SetupState) {
        if (!state.permissionsGranted) {
            binding.heading.setText(R.string.emoticon_trouble)
            binding.action.setText(R.string.grant_permissions)
        } else if (!state.bluetoothEnabled) {
            binding.heading.setText(R.string.emoticon_trouble)
            binding.action.setText(R.string.enable_bluetooth)
        } else if (state.deviceAddress.isEmpty()) {
            binding.heading.setText(R.string.emoticon_trouble)
            binding.action.setText(R.string.pair_device)
        } else {
            binding.heading.setText(R.string.emoticon_joy)
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
                binding.action.isEnabled = !binding.action.isEnabled
            }
            SetupSideEffect.Connection.EstablishFailed -> {
                binding.progress.hide()
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
                binding.action.isEnabled = !binding.action.isEnabled
            }
            GaiaGattSideEffect.Gatt.Ready -> {
                requireView().showSnackbar(msgRes = R.string.gaia_discover)
            }
            GaiaGattSideEffect.Gatt.ServiceDiscovery -> {
                requireView().showSnackbar(msgRes = R.string.gatt_discover)
            }
            else -> {
            }
        }
    }

    private fun handleActionClick() {
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
