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

package com.tomg.fiiok9control.audio.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.audio.business.AudioSideEffect
import com.tomg.fiiok9control.audio.business.AudioState
import com.tomg.fiiok9control.audio.business.AudioViewModel
import com.tomg.fiiok9control.databinding.FragmentAudioBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AudioFragment :
    BaseFragment<FragmentAudioBinding>(R.layout.fragment_audio),
    AudioAdapter.Listener {

    private val audioViewModel: AudioViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.audio.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AudioAdapter(listener = this@AudioFragment)
            itemAnimator = null
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioViewModel.container.sideEffectFlow.collect { sideEffect ->
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
            audioViewModel.sendGaiaPacketsDelayed(
                lifecycleScope,
                gaiaGattService()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioViewModel.clearGaiaPacketResponses()
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onReconnectToDevice() {
        audioViewModel.reconnectToDevice(gaiaGattService())
    }

    override fun bindLayout(view: View) = FragmentAudioBinding.bind(view)

    override fun onBluetoothCodecChanged(
        codec: BluetoothCodec,
        enabled: Boolean
    ) {
        audioViewModel.sendGaiaPacketCodecEnabled(
            lifecycleScope,
            gaiaGattService(),
            codec,
            enabled
        )
    }

    override fun onChannelBalanceRequested(value: Int) {
        audioViewModel.sendGaiaPacketChannelBalance(
            lifecycleScope,
            gaiaGattService(),
            value
        )
    }

    override fun onLowPassFilterRequested(lowPassFilter: LowPassFilter) {
        audioViewModel.sendGaiaPacketLowPassFilter(
            lifecycleScope,
            gaiaGattService(),
            lowPassFilter
        )
    }

    private fun renderState(state: AudioState) {
        (binding.audio.adapter as? AudioAdapter)?.submitList(
            listOf(state.codecsEnabled, state.lowPassFilter, state.channelBalance)
        )
    }

    private fun handleSideEffect(sideEffect: AudioSideEffect) {
        when (sideEffect) {
            AudioSideEffect.Characteristic.Changed -> {
                binding.progress.hide()
            }
            AudioSideEffect.Characteristic.Write -> {
                binding.progress.show()
            }
            AudioSideEffect.Reconnect.Failure -> {
                binding.progress.hide()
                onBluetoothStateChanged(false)
            }
            AudioSideEffect.Reconnect.Initiated -> {
                binding.progress.show()
            }
            AudioSideEffect.Reconnect.Success -> {
                binding.progress.hide()
                audioViewModel.sendGaiaPacketsDelayed(
                    lifecycleScope,
                    gaiaGattService()
                )
            }
            is AudioSideEffect.Request.Failure -> {
                binding.progress.hide()
                if (sideEffect.disconnected) {
                    onBluetoothStateChanged(false)
                }
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect?) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                onBluetoothStateChanged(false)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Failure -> {
                audioViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gatt.WriteCharacteristic.Success -> {
                audioViewModel.handleGaiaPacketSendResult(sideEffect.commandId)
            }
            is GaiaGattSideEffect.Gaia.Packet -> {
                audioViewModel.handleGaiaPacket(sideEffect.data)
            }
            else -> {
            }
        }
    }
}
