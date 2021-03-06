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

package com.tomg.fiiok9control.eq.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentEqBinding
import com.tomg.fiiok9control.eq.EqPreSet
import com.tomg.fiiok9control.eq.EqValue
import com.tomg.fiiok9control.eq.business.EqSideEffect
import com.tomg.fiiok9control.eq.business.EqState
import com.tomg.fiiok9control.eq.business.EqViewModel
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EqFragment :
    BaseFragment<FragmentEqBinding>(R.layout.fragment_eq),
    EqAdapter.Listener {

    private val eqViewModel: EqViewModel by viewModels()

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
                menuInflater.inflate(R.menu.menu_eq, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.restore_default).isVisible = eqViewModel.hasCustomEqValues()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.restore_default -> {
                        eqViewModel.sendGaiaPacketsEqValue(
                            lifecycleScope,
                            gaiaGattService()
                        )
                        true
                    }
                    else -> false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.eq.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = EqAdapter(listener = this@EqFragment)
            itemAnimator = null
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eqViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                eqViewModel.container.sideEffectFlow.collect { sideEffect ->
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
        if (gaiaGattService()?.isConnected() == true) {
            eqViewModel.sendGaiaPacketsDelayed(
                lifecycleScope,
                gaiaGattService()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        eqViewModel.clearGaiaPacketResponses()
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onReconnectToDevice() {
        eqViewModel.reconnectToDevice(gaiaGattService())
    }

    override fun bindLayout(view: View) = FragmentEqBinding.bind(view)

    override fun onEqEnabled(enabled: Boolean) {
        if (!enabled) {
            onEqPreSetRequested(EqPreSet.Custom)
        }
        eqViewModel.sendGaiaPacketEqEnabled(
            lifecycleScope,
            gaiaGattService(),
            enabled
        )
    }

    override fun onEqPreSetRequested(eqPreSet: EqPreSet) {
        eqViewModel.sendGaiaPacketEqPreSet(
            lifecycleScope,
            gaiaGattService(),
            eqPreSet
        )
    }

    override fun onEqValueChanged(value: EqValue) {
        eqViewModel.sendGaiaPacketEqValue(
            lifecycleScope,
            gaiaGattService(),
            value
        )
    }

    private fun renderState(state: EqState) {
        requireActivity().invalidateOptionsMenu()
        (binding.eq.adapter as? EqAdapter)?.submitList(
            listOf(state.eqEnabled, state.eqPreSet, state.eqValues)
        )
    }

    private fun handleSideEffect(sideEffect: EqSideEffect) {
        when (sideEffect) {
            EqSideEffect.Characteristic.Changed -> {
                binding.progress.hide()
            }
            EqSideEffect.Characteristic.Write -> {
                binding.progress.show()
            }
            EqSideEffect.Reconnect.Failure -> {
                binding.progress.hide()
                onBluetoothStateChanged(false)
            }
            EqSideEffect.Reconnect.Initiated -> {
                binding.progress.show()
            }
            EqSideEffect.Reconnect.Success -> {
                binding.progress.hide()
                eqViewModel.sendGaiaPacketsDelayed(
                    lifecycleScope,
                    gaiaGattService()
                )
            }
            is EqSideEffect.Request.Failure -> {
                binding.progress.hide()
                if (sideEffect.disconnected) {
                    onBluetoothStateChanged(false)
                }
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                onBluetoothStateChanged(false)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure -> {
                eqViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Success -> {
                eqViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gaia.Packet -> {
                eqViewModel.handleGaiaPacket(sideEffect.data)
            }
            else -> {
            }
        }
    }
}
