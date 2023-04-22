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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.tomg.fiiok9control.databinding.FragmentStateBinding
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.showSnackbar
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.state.VolumeBroadcastReceiver
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
    private val volumeReceiver: VolumeBroadcastReceiver = VolumeBroadcastReceiver(
        onVolumeUp = {
            stateViewModel.sendGaiaPacketVolume(
                lifecycleScope,
                gaiaGattService(),
                volumeUp = true
            )
        },
        onVolumeDown = {
            stateViewModel.sendGaiaPacketVolume(
                lifecycleScope,
                gaiaGattService(),
                volumeUp = false
            )
        },
        onVolumeMute = {
            stateViewModel.sendGaiaPacketMuteEnabled(
                lifecycleScope,
                gaiaGattService()
            )
        }
    )

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
                menuInflater.inflate(R.menu.menu_state, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                if (stateViewModel.container.stateFlow.value.isMuted) {
                    menu.findItem(R.id.mute_on).isChecked = true
                } else {
                    menu.findItem(R.id.mute_off).isChecked = true
                }
                when (stateViewModel.container.stateFlow.value.volumeStepSize) {
                    1 -> menu.findItem(R.id.volume_step_size_1).isChecked = true
                    2 -> menu.findItem(R.id.volume_step_size_2).isChecked = true
                    3 -> menu.findItem(R.id.volume_step_size_3).isChecked = true
                    else -> menu.findItem(R.id.volume_step_size_4).isChecked = true
                }
                if (stateViewModel.container.stateFlow.value.isMqaEnabled) {
                    menu.findItem(R.id.mqa_on).isChecked = true
                } else {
                    menu.findItem(R.id.mqa_off).isChecked = true
                }
                if (stateViewModel.container.stateFlow.value.isHpPreSimultaneously) {
                    menu.findItem(R.id.hp_pre_simultaneously_on).isChecked = true
                } else {
                    menu.findItem(R.id.hp_pre_simultaneously_off).isChecked = true
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.disconnect -> {
                        stateViewModel.disconnect(gaiaGattService())
                        true
                    }
                    R.id.export -> {
                        navigate(StateFragmentDirections.stateToExportProfile())
                        true
                    }
                    R.id.mute_on,
                    R.id.mute_off -> {
                        stateViewModel.sendGaiaPacketMuteEnabled(
                            lifecycleScope,
                            gaiaGattService()
                        )
                        true
                    }
                    R.id.mqa_on,
                    R.id.mqa_off -> {
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
                        stateViewModel.sendGaiaPacketRestore(
                            lifecycleScope,
                            gaiaGattService()
                        )
                        true
                    }
                    R.id.volume_up -> {
                        stateViewModel.sendGaiaPacketVolume(
                            lifecycleScope,
                            gaiaGattService(),
                            volumeUp = true
                        )
                        true
                    }
                    R.id.volume_down -> {
                        stateViewModel.sendGaiaPacketVolume(
                            lifecycleScope,
                            gaiaGattService(),
                            volumeUp = false
                        )
                        true
                    }
                    R.id.volume_step_size_1 -> {
                        stateViewModel.handleVolumeStepSize(volumeStepSize = 1)
                        true
                    }
                    R.id.volume_step_size_2 -> {
                        stateViewModel.handleVolumeStepSize(volumeStepSize = 2)
                        true
                    }
                    R.id.volume_step_size_3 -> {
                        stateViewModel.handleVolumeStepSize(volumeStepSize = 3)
                        true
                    }
                    R.id.volume_step_size_4 -> {
                        stateViewModel.handleVolumeStepSize(volumeStepSize = 4)
                        true
                    }
                    R.id.hp_pre_simultaneously_on,
                    R.id.hp_pre_simultaneously_off -> {
                        stateViewModel.sendGaiaPacketHpPreSimultaneously(
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
        binding.progress2.setVisibilityAfterHide(View.GONE)
        binding.state.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = StateAdapter(listener = this@StateFragment)
            itemAnimator = null
        }
        setFragmentResultListener(KEY_PROFILE_NAME) { _, args ->
            val profileName = args.getString(KEY_PROFILE_NAME, String.Empty)
            val exportVolume = args.getBoolean(KEY_PROFILE_VOLUME_EXPORT, false)
            stateViewModel.exportStateProfile(profileName, exportVolume)
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
                gaiaGattSideEffectFlow.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
        requireActivity().registerReceiver(
            volumeReceiver,
            IntentFilter(INTENT_ACTION_VOLUME_UP).apply {
                addAction(INTENT_ACTION_VOLUME_DOWN)
                addAction(INTENT_ACTION_VOLUME_MUTE)
            }
        )
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
        requireActivity().unregisterReceiver(volumeReceiver)
        stateViewModel.clearGaiaPacketResponses()
        gaiaGattService()?.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        navigate(StateFragmentDirections.stateToProfile(profile))
    }

    override fun onBluetoothStateChanged(enabled: Boolean) {
        if (!enabled) {
            navigateToStartDestination()
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

    override fun onVolumeRequested(volume: Int) {
        stateViewModel.sendGaiaPacketVolume(
            lifecycleScope,
            gaiaGattService(),
            volume
        )
    }

    private fun renderState(state: StateState) {
        requireActivity().invalidateOptionsMenu()
        if (state.isProfileExporting) {
            binding.progress2.show()
        } else {
            binding.progress2.hide()
        }
        (binding.state.adapter as? StateAdapter)?.submitList(
            listOf(
                Pair(state.fwVersion, state.audioFmt),
                Pair(state.volume, state.volumePercent),
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
            StateSideEffect.ExportProfile.Failure -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav),
                    msgRes = R.string.profile_export_failure
                )
            }
            StateSideEffect.ExportProfile.Success -> {
                requireView().showSnackbar(
                    anchor = requireActivity().findViewById(R.id.nav),
                    msgRes = R.string.profile_export_success
                )
            }
            StateSideEffect.Reconnect.Failure -> {
                binding.progress.hide()
                onBluetoothStateChanged(false)
            }
            StateSideEffect.Reconnect.Initiated -> {
                binding.progress.show()
            }
            StateSideEffect.Disconnect -> {
                binding.progress.show()
            }
            StateSideEffect.Reconnect.Success -> {
                binding.progress.hide()
                stateViewModel.sendGaiaPacketsDelayed(
                    lifecycleScope,
                    gaiaGattService()
                )
            }
            is StateSideEffect.NotifyVolume -> {
                startGaiaGattForegroundService(
                    volumePercent = sideEffect.volumePercent,
                    mute = if (sideEffect.isMuted) {
                        getString(R.string.volume_unmute)
                    } else {
                        getString(R.string.volume_mute)
                    }
                )
            }
            is StateSideEffect.Request.Failure -> {
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

    private fun startGaiaGattForegroundService(
        volumePercent: String,
        mute: String
    ) {
        val nm =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        kotlin.runCatching {
            if (nm.getNotificationChannel(ID_NOTIFICATION_CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        ID_NOTIFICATION_CHANNEL,
                        requireContext().getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setShowBadge(false)
                    }
                )
            }
            gaiaGattService()?.startForeground(
                ID_NOTIFICATION,
                buildNotification(volumePercent, mute)
            )
        }
    }

    private fun buildNotification(
        volumePercent: String,
        mute: String
    ): Notification {
        val context = requireContext()
        return Notification
            .Builder(context, ID_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_k9)
            .setContentTitle(getString(R.string.app_name))
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
