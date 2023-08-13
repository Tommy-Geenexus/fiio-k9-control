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

package com.tomg.fiiok9control.state.ui

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tomg.fiiok9control.BaseFragment
import com.tomg.fiiok9control.Empty
import com.tomg.fiiok9control.ID_NOTIFICATION
import com.tomg.fiiok9control.ID_NOTIFICATION_CHANNEL
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_DOWN
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_MUTE
import com.tomg.fiiok9control.INTENT_ACTION_VOLUME_UP
import com.tomg.fiiok9control.KEY_PROFILE_NAME
import com.tomg.fiiok9control.KEY_PROFILE_VOLUME_EXPORT
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.REQUEST_CODE
import com.tomg.fiiok9control.RecyclerViewItemDecoration
import com.tomg.fiiok9control.WINDOW_SIZE_EXPANDED_COLUMNS
import com.tomg.fiiok9control.WindowSizeClass
import com.tomg.fiiok9control.databinding.FragmentStateBinding
import com.tomg.fiiok9control.gaia.business.GaiaGattSideEffect
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.showSnackbar
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.state.VolumeBroadcastReceiver
import com.tomg.fiiok9control.state.business.StateSideEffect
import com.tomg.fiiok9control.state.business.StateState
import com.tomg.fiiok9control.state.business.StateViewModel
import com.tomg.fiiok9control.toVolumePercent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StateFragment :
    BaseFragment<FragmentStateBinding>(R.layout.fragment_state),
    StateAdapter.Listener {

    private val stateViewModel: StateViewModel by viewModels()
    private val volumeReceiver: VolumeBroadcastReceiver = VolumeBroadcastReceiver(
        onVolumeUp = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), volumeUp = true)
            }
        },
        onVolumeDown = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), volumeUp = false)
            }
        },
        onVolumeMute = {
            if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                stateViewModel.sendGaiaPacketMuteEnabled(requireGaiaGattService())
            }
        }
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
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
                navigate(StateFragmentDirections.stateToExportProfile())
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
                    stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), volumeUp = true)
                }
            },
            onVolumeDown = {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    stateViewModel.sendGaiaPacketVolume(requireGaiaGattService(), volumeUp = false)
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
            layoutManager = if (getWindowSizeClass() == WindowSizeClass.Expanded) {
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
                profileName = args.getString(KEY_PROFILE_NAME, String.Empty),
                exportVolume = args.getBoolean(KEY_PROFILE_VOLUME_EXPORT, false)
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

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        navigate(StateFragmentDirections.stateToProfile(profile))
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

    override fun onUpdatePendingIndicatorBrightness(indicatorBrightness: Int) {
        stateViewModel.updatePendingIndicatorBrightness(indicatorBrightness)
    }

    override fun onIndicatorBrightnessRequested(indicatorBrightness: Int) {
        if (stateViewModel.container.stateFlow.value.isServiceConnected) {
            stateViewModel.sendGaiaPacketIndicatorBrightness(
                requireGaiaGattService(),
                indicatorBrightness
            )
        }
    }

    override fun onUpdatePendingVolumeLevel(volume: Int) {
        stateViewModel.updatePendingVolumeLevel(volume)
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
                navigateToStartDestination()
            }
            StateSideEffect.ExportProfile.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_export_failure
                )
            }

            StateSideEffect.ExportProfile.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav_view),
                    msgRes = R.string.profile_export_success
                )
            }

            is StateSideEffect.NotifyVolume -> {
                if (stateViewModel.container.stateFlow.value.isServiceConnected) {
                    createNotificationChannelIfNotExists()
                    requireGaiaGattService().startForeground(
                        ID_NOTIFICATION,
                        buildVolumeNotification(
                            volumePercent = sideEffect.volume.toVolumePercent(),
                            mute = getString(
                                if (sideEffect.isMuteEnabled) {
                                    R.string.volume_unmute
                                } else {
                                    R.string.volume_mute
                                }
                            )
                        )
                    )
                }
            }
        }
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToStartDestination()
            }

            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                stateViewModel.handleCharacteristicWriteResult(sideEffect.packet, success = false)
            }

            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                stateViewModel.handleCharacteristicWriteResult(sideEffect.packet, success = true)
            }

            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                stateViewModel.handleCharacteristicChanged(sideEffect.packet)
            }

            else -> {
            }
        }
    }

    private fun createNotificationChannelIfNotExists(
        id: String = ID_NOTIFICATION_CHANNEL,
        name: CharSequence = requireContext().getString(R.string.app_name),
        importance: Int = NotificationManager.IMPORTANCE_HIGH
    ) = kotlin.runCatching {
        val nm =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildVolumeNotification(
        volumePercent: String,
        mute: String
    ): Notification {
        val context = requireContext()
        return Notification
            .Builder(context, ID_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_k9)
            .setContentTitle(getString(R.string.fiio_k9))
            .setContentText(getString(R.string.volume_level, volumePercent))
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.volume_up),
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_UP),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.volume_down),
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_DOWN),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    mute,
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_MUTE),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }
}
