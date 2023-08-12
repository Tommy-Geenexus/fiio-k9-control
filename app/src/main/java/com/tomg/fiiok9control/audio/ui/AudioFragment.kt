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

package com.tomg.fiiok9control.audio.ui

import android.annotation.SuppressLint
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
import com.tomg.fiiok9control.RecyclerViewItemDecoration
import com.tomg.fiiok9control.WINDOW_SIZE_EXPANDED_COLUMNS
import com.tomg.fiiok9control.WindowSizeClass
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.audio.business.AudioState
import com.tomg.fiiok9control.audio.business.AudioViewModel
import com.tomg.fiiok9control.databinding.FragmentAudioBinding
import com.tomg.fiiok9control.gaia.business.GaiaGattSideEffect
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults
import com.tomg.fiiok9control.profile.data.Profile
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
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            FiioK9Defaults.DISPLAY_NAME
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.audio.apply {
            addItemDecoration(
                RecyclerViewItemDecoration(
                    margin = resources.getDimension(R.dimen.spacing_small).toInt(),
                    isLtr = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
                )
            )
            layoutManager = if (getWindowSizeClass() == WindowSizeClass.Expanded) {
                StaggeredGridLayoutManager(WINDOW_SIZE_EXPANDED_COLUMNS, RecyclerView.VERTICAL)
            } else {
                LinearLayoutManager(context)
            }
            adapter = AudioAdapter(
                listener = this@AudioFragment,
                currentCodecsEnabled = {
                    with(audioViewModel.container.stateFlow.value) {
                        pendingCodecsEnabled ?: codecsEnabled
                    }
                },
                currentLowPassFilter = {
                    with(audioViewModel.container.stateFlow.value) {
                        pendingLowPassFilter ?: lowPassFilter
                    }
                },
                currentChannelBalance = {
                    with(audioViewModel.container.stateFlow.value) {
                        pendingChannelBalance ?: channelBalance
                    }
                },
                currentIsLoading = {
                    audioViewModel.container.stateFlow.value.pendingCommands.isNotEmpty()
                },
                currentIsServiceConnected = {
                    audioViewModel.container.stateFlow.value.isServiceConnected
                }
            )
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
                gaiaGattSideEffects.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
    }

    override fun bindLayout(view: View) = FragmentAudioBinding.bind(view)

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        navigate(AudioFragmentDirections.audioToProfile(profile))
    }

    override fun onServiceConnectionStateChanged(isConnected: Boolean) {
        audioViewModel.handleServiceConnectionStateChanged(isConnected)
        if (isConnected) {
            if (shouldConsumeIntent(requireActivity().intent)) {
                consumeIntent(requireActivity().intent)
            } else {
                audioViewModel.sendGaiaPacketsDelayed(requireGaiaGattService())
            }
        }
    }

    override fun onBluetoothCodecChanged(
        codec: BluetoothCodec,
        enabled: Boolean
    ) {
        if (audioViewModel.container.stateFlow.value.isServiceConnected) {
            audioViewModel.sendGaiaPacketCodecsEnabled(requireGaiaGattService(), codec, enabled)
        }
    }

    override fun onChannelBalanceRequested(value: Int) {
        if (audioViewModel.container.stateFlow.value.isServiceConnected) {
            audioViewModel.sendGaiaPacketChannelBalance(requireGaiaGattService(), value)
        }
    }

    override fun onLowPassFilterRequested(lowPassFilter: LowPassFilter) {
        if (audioViewModel.container.stateFlow.value.isServiceConnected) {
            audioViewModel.sendGaiaPacketLowPassFilter(requireGaiaGattService(), lowPassFilter)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun renderState(state: AudioState) {
        val isLoading = state.pendingCommands.isNotEmpty()
        binding.progress.isVisible = isLoading
        binding.audio.adapter?.notifyDataSetChanged()
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToStartDestination()
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                audioViewModel.handleCharacteristicWriteResult(sideEffect.packet, success = false)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                audioViewModel.handleCharacteristicWriteResult(sideEffect.packet, success = true)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                audioViewModel.handleCharacteristicChanged(sideEffect.packet)
            }
            else -> {
            }
        }
    }
}
