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

package com.tomg.fiiok9control

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.google.android.material.transition.MaterialSharedAxis
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import kotlinx.coroutines.flow.Flow

abstract class BaseFragment<B : ViewBinding>(
    @LayoutRes layoutRes: Int
) : Fragment(layoutRes) {

    private var _binding: B? = null
    internal val binding: B get() = _binding!!

    lateinit var gaiaGattSideEffectFlow: Flow<GaiaGattSideEffect>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        gaiaGattSideEffectFlow = (requireActivity() as FiioK9ControlActivity).gaiaGattSideEffectFlow
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions(
            transitionEnter = MaterialSharedAxis(MaterialSharedAxis.Z, true),
            transitionExit = MaterialSharedAxis(MaterialSharedAxis.Z, true),
            transitionReturn = MaterialSharedAxis(MaterialSharedAxis.Z, false),
            transitionReenter = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        _binding = bindLayout(view)
        binding.root.applyInsetMargins()
        requireActivity().supportFragmentManager.apply {
            setFragmentResultListener(KEY_BLUETOOTH_ENABLED, viewLifecycleOwner) { _, args ->
                val enabled = args.getBoolean(KEY_BLUETOOTH_ENABLED, false)
                onBluetoothStateChanged(enabled)
            }
            setFragmentResultListener(KEY_SERVICE_CONNECTED, viewLifecycleOwner) { _, args ->
                val connected = args.getBoolean(KEY_SERVICE_CONNECTED, false)
                if (connected && gaiaGattService()?.isConnected() == false) {
                    onReconnectToDevice()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val service = gaiaGattService()
        if (service != null && !service.isConnected()) {
            onReconnectToDevice()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun onBluetoothStateChanged(enabled: Boolean)

    abstract fun onReconnectToDevice()

    abstract fun bindLayout(view: View): B

    fun gaiaGattService() = (requireActivity() as? FiioK9ControlActivity)?.gaiaGattService

    internal fun setupToolbar(
        toolbar: Toolbar,
        @StringRes titleRes: Int = 0
    ) {
        if (titleRes != 0) {
            toolbar.setTitle(titleRes)
        }
        (requireActivity() as? AppCompatActivity)?.setSupportActionBar(toolbar)
    }

    private fun setTransitions(
        transitionEnter: MaterialSharedAxis? = null,
        transitionExit: MaterialSharedAxis? = null,
        transitionReturn: MaterialSharedAxis? = null,
        transitionReenter: MaterialSharedAxis? = null
    ) {
        enterTransition = transitionEnter?.apply {
            duration = resources.getInteger(
                com.google.android.material.R.integer.material_motion_duration_long_1
            ).toLong()
        }
        exitTransition = transitionExit?.apply {
            duration = resources.getInteger(
                com.google.android.material.R.integer.material_motion_duration_long_1
            ).toLong()
        }
        returnTransition = transitionReturn?.apply {
            duration = resources.getInteger(
                com.google.android.material.R.integer.material_motion_duration_long_1
            ).toLong()
        }
        reenterTransition = transitionReenter?.apply {
            duration = resources.getInteger(
                com.google.android.material.R.integer.material_motion_duration_long_1
            ).toLong()
        }
    }

    internal fun navigate(navDirections: NavDirections) {
        val navController = findNavController()
        val action = navController.currentDestination?.getAction(navDirections.actionId)
        if (action != null && action.destinationId != 0) {
            navController.navigate(navDirections)
        }
    }
}
