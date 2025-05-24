/*
 * Copyright (c) 2021-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.state.ui

import android.annotation.SuppressLint
import android.app.Service
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.IndicatorState
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.InputSource
import io.github.tommygeenexus.fiiok9control.core.receiver.VolumeBroadcastReceiver
import io.github.tommygeenexus.fiiok9control.core.ui.BaseFragment
import io.github.tommygeenexus.fiiok9control.core.ui.RecyclerViewItemDecoration
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattServiceNotification
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattServiceNotification.startForeground
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_DOWN
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_MUTE
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_UP
import io.github.tommygeenexus.fiiok9control.core.util.KEY_PROFILE_NAME
import io.github.tommygeenexus.fiiok9control.core.util.KEY_PROFILE_VOLUME_EXPORT
import io.github.tommygeenexus.fiiok9control.core.util.WINDOW_SIZE_EXPANDED_COLUMNS
import io.github.tommygeenexus.fiiok9control.databinding.FragmentStateBinding
import io.github.tommygeenexus.fiiok9control.state.business.StateSideEffect
import io.github.tommygeenexus.fiiok9control.state.business.StateState
import io.github.tommygeenexus.fiiok9control.state.business.StateViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StateFragment :
    BaseFragment<FragmentStateBinding>(R.layout.fragment_state),
    StateAdapter.Listener {

    private val stateViewModel: StateViewModel by viewModels()
    private val volumeReceiver: VolumeBroadcastReceiver = VolumeBroadcastReceiver(
        onVolumeUp = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), isVolumeUp = true)
            }
        },
        onVolumeDown = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), isVolumeUp = false)
            }
        },
        onVolumeMute = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketMuteEnabled(requireGaiaGattService())
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            FiioK9Defaults.DISPLAY_NAME
        val menuProvider = StateMenuProvider(
            isHpPreSimultaneouslyEnabled = {
                stateViewModel.container.stateFlow.value.isHpPreSimultaneouslyEnabled
            },
            isLoading = {
                with(stateViewModel.container.stateFlow.value) {
                    isExportingProfile || isDisconnecting || pendingCommands.isNotEmpty()
                }
            },
            isMqaEnabled = {
                stateViewModel.container.stateFlow.value.isMqaEnabled
            },
            isMuteEnabled = {
                stateViewModel.container.stateFlow.value.isMuteEnabled
            },
            isServiceConnected = {
                stateViewModel.container.stateFlow.value.isServiceConnected
            },
            volumeStepSize = {
                stateViewModel.container.stateFlow.value.volumeStepSize
            },
            onDisconnect = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.disconnect(requireGaiaGattService())
                }
            },
            onExportProfile = {
                findNavController().navigate(StateFragmentDirections.stateToExportProfile())
            },
            onToggleHpPreSimultaneously = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketHpPreSimultaneously(requireGaiaGattService())
                }
            },
            onToggleMqaEnabled = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketMqa(requireGaiaGattService())
                }
            },
            onToggleMuteEnabled = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketMuteEnabled(requireGaiaGattService())
                }
            },
            onStandby = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketStandby(requireGaiaGattService())
                }
            },
            onReset = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketRestore(requireGaiaGattService())
                }
            },
            onVolumeUp = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), isVolumeUp = true)
                }
            },
            onVolumeDown = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketVolume(
                        requireGaiaGattService(),
                        isVolumeUp = false
                    )
                }
            },
            onVolumeStepSizeChanged = { volumeStepSize ->
                stateViewModel.handleVolumeStepSize(volumeStepSize)
            }
        )
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.state.apply {
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
            adapter = StateAdapter(
                listener = this@StateFragment,
                currentAudioFormat = {
                    stateViewModel.container.stateFlow.value.audioFmt
                },
                currentFirmwareVersion = {
                    stateViewModel.container.stateFlow.value.fwVersion
                },
                currentIndicatorBrightness = {
                    with(stateViewModel.container.stateFlow.value) {
                        pendingIndicatorBrightness ?: indicatorBrightness
                    }
                },
                currentIndicatorState = {
                    with(stateViewModel.container.stateFlow.value) {
                        pendingIndicatorState ?: indicatorState
                    }
                },
                currentInputSource = {
                    with(stateViewModel.container.stateFlow.value) {
                        pendingInputSource ?: inputSource
                    }
                },
                currentVolume = {
                    with(stateViewModel.container.stateFlow.value) {
                        pendingVolume ?: volume
                    }
                },
                currentVolumeRelative = {
                    with(stateViewModel.container.stateFlow.value) {
                        pendingVolumeRelative ?: volumeRelative
                    }
                },
                currentIsLoading = {
                    with(stateViewModel.container.stateFlow.value) {
                        isExportingProfile || isDisconnecting || pendingCommands.isNotEmpty()
                    }
                },
                currentIsServiceConnected = {
                    stateViewModel.container.stateFlow.value.isServiceConnected
                }
            )
        }
        setFragmentResultListener(KEY_PROFILE_NAME) { _, args ->
            stateViewModel.exportStateProfile(
                profileName = args.getString(KEY_PROFILE_NAME, ""),
                isExportVolumeEnabled = args.getBoolean(KEY_PROFILE_VOLUME_EXPORT, false)
            )
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateViewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                stateViewModel.container.sideEffectFlow.collect { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                gaiaGattSideEffects.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
        ContextCompat.registerReceiver(
            requireContext(),
            volumeReceiver,
            IntentFilter(INTENT_ACTION_VOLUME_UP).apply {
                addAction(INTENT_ACTION_VOLUME_DOWN)
                addAction(INTENT_ACTION_VOLUME_MUTE)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(volumeReceiver)
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            requireGaiaGattService().stopForeground(Service.STOP_FOREGROUND_REMOVE)
        }
    }

    override fun bindLayout(view: View) = FragmentStateBinding.bind(view)

    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                requireGaiaGattService().reset()
            }
            navigateToSetup()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        findNavController().navigate(StateFragmentDirections.stateToProfile(profile))
    }

    override fun onServiceConnectionStateChanged(isConnected: Boolean) {
        stateViewModel.handleServiceConnectionStateChanged(isConnected)
        if (isConnected) {
            if (shouldConsumeIntent(requireActivity().intent)) {
                consumeIntent(requireActivity().intent)
            } else {
                stateViewModel.sendGaiaPacketsDelayed(requireGaiaGattService())
            }
        }
    }

    override fun onInputSourceRequested(inputSource: InputSource) {
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            stateViewModel.sendGaiaPacketInputSource(requireGaiaGattService(), inputSource)
        }
    }

    override fun onIndicatorStateRequested(indicatorState: IndicatorState) {
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            stateViewModel.sendGaiaPacketIndicatorState(
                requireGaiaGattService(),
                indicatorState
            )
        }
    }

    override fun onIndicatorBrightnessRequested(indicatorBrightness: Int) {
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            stateViewModel.sendGaiaPacketIndicatorBrightness(
                requireGaiaGattService(),
                indicatorBrightness
            )
        }
    }

    override fun onVolumeLevelRequested(volume: Int) {
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), volume)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun renderState(state: StateState) {
        requireActivity().invalidateOptionsMenu()
        binding.progress.isVisible = with(state) {
            isExportingProfile || isDisconnecting || pendingCommands.isNotEmpty()
        }
        binding.state.adapter?.notifyDataSetChanged()
    }

    private fun handleSideEffect(sideEffect: StateSideEffect) {
        when (sideEffect) {
            StateSideEffect.Disconnected -> {
                navigateToSetup()
            }
            StateSideEffect.ExportProfile.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_export_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            StateSideEffect.ExportProfile.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.profile_export_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(requireActivity().findViewById<View>(R.id.nav_view))
                    .show()
            }
            is StateSideEffect.NotifyVolume -> {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    val result =
                        GaiaGattServiceNotification.createNotificationChannel(requireContext())
                    if (result.isSuccess) {
                        requireGaiaGattService().startForeground(
                            context = requireContext(),
                            volume = sideEffect.volume,
                            volumeRelative = sideEffect.volumeRelative,
                            isMuteEnabled = sideEffect.isMuteEnabled
                        )
                    }
                }
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToSetup()
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                stateViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = false)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                stateViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = true)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                stateViewModel.handleCharacteristicChanged(sideEffect.packet)
            }
            else -> {
            }
        }
    }
}
