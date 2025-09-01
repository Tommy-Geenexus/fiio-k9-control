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

package io.github.tommygeenexus.fiiok9control.core.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import androidx.window.core.layout.WindowSizeClass
import io.github.tommygeenexus.fiiok9control.FiioK9ControlActivity
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattService
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_SHORTCUT_PROFILE
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_EXTRA_CONSUMED
import io.github.tommygeenexus.fiiok9control.core.util.KEY_BLUETOOTH_ENABLED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment<B : ViewBinding>(@LayoutRes layoutRes: Int) : Fragment(layoutRes) {

    private var _binding: B? = null
    internal val binding: B get() = _binding!!

    private lateinit var serviceConnection: Flow<Boolean>
    internal lateinit var gaiaGattSideEffects: Flow<GaiaGattSideEffect>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        serviceConnection = (requireActivity() as FiioK9ControlActivity).serviceConnection
        gaiaGattSideEffects = (requireActivity() as FiioK9ControlActivity).gaiaGattSideEffects
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = bindLayout(view)
        requireActivity().supportFragmentManager.setFragmentResultListener(
            KEY_BLUETOOTH_ENABLED,
            viewLifecycleOwner
        ) { _, args ->
            onBluetoothStateChanged(args.getBoolean(KEY_BLUETOOTH_ENABLED, false))
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                serviceConnection.collect { isConnected ->
                    onServiceConnectionStateChanged(isConnected)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun bindLayout(view: View): B

    abstract fun onBluetoothStateChanged(isEnabled: Boolean)

    abstract fun onProfileShortcutSelected(profile: Profile)

    abstract fun onServiceConnectionStateChanged(isConnected: Boolean)

    internal fun shouldConsumeIntent(intent: Intent): Boolean =
        intent.hasExtra(INTENT_ACTION_SHORTCUT_PROFILE) &&
            !intent.hasExtra(INTENT_EXTRA_CONSUMED)

    internal fun consumeIntent(intent: Intent) {
        val profile = IntentCompat.getParcelableExtra(
            intent,
            INTENT_ACTION_SHORTCUT_PROFILE,
            PersistableBundle::class.java
        )
        if (profile != null) {
            requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
            onProfileShortcutSelected(Profile.createFromPersistableBundle(profile))
        }
    }

    internal fun isWidthAtLeastBreakpointExpandedLowerBound() =
        (requireActivity() as FiioK9ControlActivity)
            .windowSizeClass
            .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    internal fun navigateToSetup() {
        with(findNavController()) {
            // Workaround for issue introduced in androidx.navigation:navigation-*:2.7.5
            // (setGraph does not clear backstack and navigate to start destination)
            setGraph(R.navigation.nav_graph_2)
            setGraph(R.navigation.nav_graph)
        }
    }

    internal fun requireGaiaGattService(): GaiaGattService =
        checkNotNull((requireActivity() as? FiioK9ControlActivity)?.gaiaGattService)
}
