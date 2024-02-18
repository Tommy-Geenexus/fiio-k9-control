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

package io.github.tommygeenexus.fiiok9control.audio.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.window.core.layout.WindowWidthSizeClass
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.audio.business.AudioState
import io.github.tommygeenexus.fiiok9control.audio.business.AudioViewModel
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.BluetoothCodec
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.LowPassFilter
import io.github.tommygeenexus.fiiok9control.core.ui.BaseFragment
import io.github.tommygeenexus.fiiok9control.core.ui.RecyclerViewItemDecoration
import io.github.tommygeenexus.fiiok9control.core.util.WINDOW_SIZE_EXPANDED_COLUMNS
import io.github.tommygeenexus.fiiok9control.databinding.FragmentAudioBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AudioFragment :
    BaseFragment<FragmentAudioBinding>(R.layout.fragment_audio),
    AudioAdapter.Listener {

    private val audioViewModel: AudioViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
            layoutManager = if (getWindowWidthSizeClass() == WindowWidthSizeClass.EXPANDED) {
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

    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            if (audioViewModel.container.stateFlow.value.isServiceConnected) {
                requireGaiaGattService().reset()
            }
            navigateToSetup()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        findNavController().navigate(AudioFragmentDirections.audioToProfile(profile))
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

    override fun onBluetoothCodecChanged(codec: BluetoothCodec, isEnabled: Boolean) {
        if (audioViewModel.container.stateFlow.value.isServiceConnected) {
            audioViewModel.sendGaiaPacketCodecsEnabled(requireGaiaGattService(), codec, isEnabled)
        }
    }

    override fun onChannelBalanceRequested(channelBalance: Int) {
        if (audioViewModel.container.stateFlow.value.isServiceConnected) {
            audioViewModel.sendGaiaPacketChannelBalance(requireGaiaGattService(), channelBalance)
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
                navigateToSetup()
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                audioViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = false)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                audioViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = true)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                audioViewModel.handleCharacteristicChanged(sideEffect.packet)
            }
            else -> {
            }
        }
    }
}
