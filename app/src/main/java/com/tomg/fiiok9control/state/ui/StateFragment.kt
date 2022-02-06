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

package com.tomg.fiiok9control.state.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.FragmentStateBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.state.business.StateSideEffect
import com.tomg.fiiok9control.state.business.StateState
import com.tomg.fiiok9control.state.business.StateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StateFragment :
    BaseFragment<FragmentStateBinding>(R.layout.fragment_state),
    StateAdapter.Listener {

    private val stateViewModel: StateViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setupToolbar(
            toolbar = binding.toolbar,
            titleRes = R.string.app_name
        )
        binding.state.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = StateAdapter(listener = this@StateFragment)
            itemAnimator = null
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateViewModel.container.sideEffectFlow.collect { sideEffect ->
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

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_state, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.mqa).setTitle(
            if (stateViewModel.container.stateFlow.value.isMqaEnabled) {
                R.string.mqa_disable
            } else {
                R.string.mqa_enable
            }
        )
        menu.findItem(R.id.mute).setIcon(
            if (stateViewModel.container.stateFlow.value.isMuted) {
                R.drawable.ic_volume_off
            } else {
                R.drawable.ic_volume_up
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mute -> {
                stateViewModel.sendGaiaPacketMute(
                    lifecycleScope,
                    gaiaGattService()
                )
                true
            }
            R.id.mqa -> {
                stateViewModel.sendGaiaPacketMqa(
                    lifecycleScope,
                    gaiaGattService()
                )
                true
            }
            R.id.standby -> {
                stateViewModel.sendGaiaPacketStandby(
                    lifecycleScope,
                    gaiaGattService()
                )
                true
            }
            R.id.reset -> {
                stateViewModel.sendGaiaPacketReset(
                    lifecycleScope,
                    gaiaGattService()
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (gaiaGattService()?.isConnected() == true) {
            stateViewModel.sendGaiaPacketsDelayed(
                lifecycleScope,
                gaiaGattService()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stateViewModel.clearGaiaPacketResponses()
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigate(StateFragmentDirections.stateToSetup())
        }
    }

    override fun onReconnectToDevice() {
        stateViewModel.reconnectToDevice(gaiaGattService())
    }

    override fun bindLayout(view: View) = FragmentStateBinding.bind(view)

    override fun onInputSourceRequested(inputSource: InputSource) {
        stateViewModel.sendGaiaPacketsInputSource(
            lifecycleScope,
            gaiaGattService(),
            inputSource
        )
    }

    override fun onIndicatorStateRequested(indicatorState: IndicatorState) {
        stateViewModel.sendGaiaPacketIndicatorState(
            lifecycleScope,
            gaiaGattService(),
            indicatorState
        )
    }

    override fun onIndicatorBrightnessRequested(indicatorBrightness: Int) {
        stateViewModel.sendGaiaPacketIndicatorBrightness(
            lifecycleScope,
            gaiaGattService(),
            indicatorBrightness
        )
    }

    private fun renderState(state: StateState) {
        requireActivity().invalidateOptionsMenu()
        (binding.state.adapter as? StateAdapter)?.submitList(
            listOf(
                state.fwVersion to state.audioFmt,
                state.inputSource,
                state.indicatorState to state.indicatorBrightness
            )
        )
    }

    private fun handleSideEffect(sideEffect: StateSideEffect) {
        when (sideEffect) {
            StateSideEffect.Characteristic.Changed -> {
                binding.progress.hide()
            }
            StateSideEffect.Characteristic.Write -> {
                binding.progress.show()
            }
            StateSideEffect.Reconnect.Failure -> {
                binding.progress.hide()
                navigate(StateFragmentDirections.stateToSetup())
            }
            StateSideEffect.Reconnect.Initiated -> {
                binding.progress.show()
            }
            StateSideEffect.Reconnect.Success -> {
                binding.progress.hide()
                stateViewModel.sendGaiaPacketsDelayed(
                    lifecycleScope,
                    gaiaGattService()
                )
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect?) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigate(StateFragmentDirections.stateToSetup())
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure -> {
                stateViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Success -> {
                stateViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gaia.Packet -> {
                stateViewModel.handleGaiaPacket(sideEffect.data)
            }
            else -> {
            }
        }
    }
}
