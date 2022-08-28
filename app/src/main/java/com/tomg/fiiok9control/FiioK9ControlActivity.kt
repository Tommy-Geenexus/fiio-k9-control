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

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.elevation.SurfaceColors
import com.tomg.fiiok9control.databinding.ActivityFiioK9ControlBinding
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.gaia.GaiaGattServiceConnection
import com.tomg.fiiok9control.gaia.GaiaGattSideEffect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FiioK9ControlActivity : AppCompatActivity() {

    private var bluetoothStateReceiver: BluetoothStateReceiver = BluetoothStateReceiver(
        onBluetoothDisabled = {
            supportFragmentManager.setFragmentResult(
                KEY_BLUETOOTH_ENABLED,
                bundleOf(KEY_BLUETOOTH_ENABLED to false)
            )
        },
        onBluetoothEnabled = {
            supportFragmentManager.setFragmentResult(
                KEY_BLUETOOTH_ENABLED,
                bundleOf(KEY_BLUETOOTH_ENABLED to true)
            )
        }
    )

    private val connection = GaiaGattServiceConnection(
        onServiceConnected = { gaiaGattService ->
            this.gaiaGattService = gaiaGattService
        },
        onServiceDisconnected = {
            gaiaGattService = null
        }
    )

    var gaiaGattService: GaiaGattService? = null
        set(value) {
            field = value
            if (value != null) {
                lifecycleScope.launch {
                    _gaiaGattSideEffectFlow.emitAll(
                        value.gaiaGattSideEffectChannel.consumeAsFlow()
                    )
                }
            }
            supportFragmentManager.setFragmentResult(
                KEY_SERVICE_CONNECTED,
                bundleOf(KEY_SERVICE_CONNECTED to (value != null))
            )
            consumeProfileShortcutIntent()
        }
    private val _gaiaGattSideEffectFlow: MutableSharedFlow<GaiaGattSideEffect> = MutableSharedFlow()
    val gaiaGattSideEffectFlow: Flow<GaiaGattSideEffect> = _gaiaGattSideEffectFlow

    init {
        addOnNewIntentListener { intent ->
            setIntent(intent)
            consumeProfileShortcutIntent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivityFiioK9ControlBinding.inflate(layoutInflater)
        window.statusBarColor =
            SurfaceColors.getColorForElevation(this, binding.toolbar.elevation)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.applyInsetMargins()
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment)
                .navController
        navController.addOnDestinationChangedListener { _, navDestination, _ ->
            if (navDestination.id == R.id.fragment_setup) {
                window.navigationBarColor = getColor(android.R.color.transparent)
                binding.nav.isInvisible = true
            } else if (binding.nav.isInvisible) {
                window.navigationBarColor =
                    SurfaceColors.getColorForElevation(this, binding.nav.elevation)
                binding.nav.isInvisible = false
            }
        }
        binding.nav.setupWithNavController(navController)
        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        bindService(Intent(this, GaiaGattService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        unbindService(connection)
        gaiaGattService = null
    }

    @Suppress("DEPRECATION")
    private fun consumeProfileShortcutIntent() {
        val profile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(INTENT_ACTION_SHORTCUT_PROFILE, PersistableBundle::class.java)
        } else {
            intent.getParcelableExtra(INTENT_ACTION_SHORTCUT_PROFILE)
        }
        if (profile != null) {
            intent.putExtra(INTENT_ACTION_SHORTCUT_PROFILE, (null as Parcelable?))
            supportFragmentManager.setFragmentResult(
                KEY_SHORTCUT_PROFILE,
                bundleOf(KEY_SHORTCUT_PROFILE to profile)
            )
        }
    }
}
