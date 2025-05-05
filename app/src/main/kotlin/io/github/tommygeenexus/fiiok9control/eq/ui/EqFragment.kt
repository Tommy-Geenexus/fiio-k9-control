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

package io.github.tommygeenexus.fiiok9control.eq.ui

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
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqPreSet
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.EqValue
import io.github.tommygeenexus.fiiok9control.core.ui.BaseFragment
import io.github.tommygeenexus.fiiok9control.core.ui.RecyclerViewItemDecoration
import io.github.tommygeenexus.fiiok9control.core.util.WINDOW_SIZE_EXPANDED_COLUMNS
import io.github.tommygeenexus.fiiok9control.databinding.FragmentEqBinding
import io.github.tommygeenexus.fiiok9control.eq.business.EqState
import io.github.tommygeenexus.fiiok9control.eq.business.EqViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EqFragment :
    BaseFragment<FragmentEqBinding>(R.layout.fragment_eq),
    EqAdapter.Listener {

    private val eqViewModel: EqViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            FiioK9Defaults.DISPLAY_NAME
        val menuProvider = EqMenuProvider(
            hasCustomEqValues = {
                eqViewModel
                    .container
                    .stateFlow
                    .value
                    .eqValues
                    .any { eqValue -> eqValue.value != 0f }
            },
            onRestoreDefault = {
                if (eqViewModel.container.stateFlow.value.isServiceConnected) {
                    eqViewModel.sendGaiaPacketsEqValue(requireGaiaGattService())
                }
            }
        )
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        binding.progress.setVisibilityAfterHide(View.GONE)
        binding.eq.apply {
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
            adapter = EqAdapter(
                listener = this@EqFragment,
                currentEqEnabled = {
                    with(eqViewModel.container.stateFlow.value) {
                        pendingEqEnabled ?: eqEnabled
                    }
                },
                currentEqPreSet = {
                    with(eqViewModel.container.stateFlow.value) {
                        pendingEqPreSet ?: eqPreSet
                    }
                },
                currentEqValues = {
                    with(eqViewModel.container.stateFlow.value) {
                        pendingEqValues ?: eqValues
                    }
                },
                currentIsLoading = {
                    eqViewModel.container.stateFlow.value.pendingCommands.isNotEmpty()
                },
                currentIsServiceConnected = {
                    eqViewModel.container.stateFlow.value.isServiceConnected
                }
            )
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
                gaiaGattSideEffects.collect { sideEffect ->
                    handleGaiaGattSideEffect(sideEffect)
                }
            }
        }
    }

    override fun bindLayout(view: View) = FragmentEqBinding.bind(view)

    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        if (!isEnabled) {
            if (eqViewModel.container.stateFlow.value.isServiceConnected) {
                requireGaiaGattService().reset()
            }
            navigateToSetup()
        }
    }

    override fun onProfileShortcutSelected(profile: Profile) {
        findNavController().navigate(EqFragmentDirections.eqToProfile(profile))
    }

    override fun onServiceConnectionStateChanged(isConnected: Boolean) {
        eqViewModel.handleServiceConnectionStateChanged(isConnected)
        if (isConnected) {
            if (shouldConsumeIntent(requireActivity().intent)) {
                consumeIntent(requireActivity().intent)
            } else {
                eqViewModel.sendGaiaPacketsDelayed(requireGaiaGattService())
            }
        }
    }

    override fun onEqEnabled(isEnabled: Boolean) {
        if (eqViewModel.container.stateFlow.value.isServiceConnected) {
            eqViewModel.sendGaiaPacketEqEnabled(requireGaiaGattService(), isEnabled)
        }
    }

    override fun onEqPreSetRequested(eqPreSet: EqPreSet) {
        if (eqViewModel.container.stateFlow.value.isServiceConnected) {
            eqViewModel.sendGaiaPacketEqPreSet(requireGaiaGattService(), eqPreSet)
        }
    }

    override fun onEqValueChanged(value: EqValue) {
        if (eqViewModel.container.stateFlow.value.isServiceConnected) {
            eqViewModel.sendGaiaPacketEqValue(requireGaiaGattService(), value)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun renderState(state: EqState) {
        requireActivity().invalidateOptionsMenu()
        val isLoading = state.pendingCommands.isNotEmpty()
        binding.progress.isVisible = isLoading
        binding.eq.adapter?.notifyDataSetChanged()
    }

    private fun handleGaiaGattSideEffect(sideEffect: GaiaGattSideEffect) {
        when (sideEffect) {
            GaiaGattSideEffect.Gatt.Disconnected -> {
                navigateToSetup()
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Failure -> {
                eqViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = false)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Write.Success -> {
                eqViewModel.handleCharacteristicWriteResult(sideEffect.packet, isSuccess = true)
            }
            is GaiaGattSideEffect.Gatt.Characteristic.Changed -> {
                eqViewModel.handleCharacteristicChanged(sideEffect.packet)
            }
            else -> {
            }
        }
    }
}
