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

package com.tomg.fiiok9control

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.elevation.SurfaceColors
import com.tomg.fiiok9control.databinding.ActivityFiioK9ControlBinding
import com.tomg.fiiok9control.gaia.business.GaiaGattSideEffect
import com.tomg.fiiok9control.gaia.ui.GaiaGattService
import com.tomg.fiiok9control.gaia.ui.GaiaGattServiceConnection
import com.tomg.fiiok9control.setup.BluetoothStateBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FiioK9ControlActivity : AppCompatActivity() {

    private val bluetoothStateReceiver = BluetoothStateBroadcastReceiver(
        onBluetoothStateChanged = { enabled ->
            supportFragmentManager.setFragmentResult(
                KEY_BLUETOOTH_ENABLED,
                bundleOf(KEY_BLUETOOTH_ENABLED to enabled)
            )
        }
    )

    private val connection = GaiaGattServiceConnection(
        onServiceConnected = { service ->
            gaiaGattService = service
            _serviceConnection.compareAndSet(expect = false, update = true)
            lifecycleScope.launch {
                _gaiaGattSideEffects.emitAll(service.gaiaGattSideEffects.receiveAsFlow())
            }
        },
        onServiceDisconnected = {
            gaiaGattService = null
            _serviceConnection.compareAndSet(expect = true, update = false)
            lifecycleScope.launch {
                _serviceConnection.emit(false)
            }
        }
    )

    private val _serviceConnection = MutableStateFlow(false)
    val serviceConnection = _serviceConnection.asStateFlow()
    private val _gaiaGattSideEffects = MutableSharedFlow<GaiaGattSideEffect>()
    val gaiaGattSideEffects = _gaiaGattSideEffects.asSharedFlow()
    var gaiaGattService: GaiaGattService? = null

    internal var windowSizeClass: WindowSizeClass = WindowSizeClass.Unspecified

    init {
        addOnNewIntentListener { intent -> setIntent(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivityFiioK9ControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        window.statusBarColor = getColor(android.R.color.transparent)
        val host = supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment
        setupNavigation(binding, host.navController)
        binding.layout.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClass()
            }
        })
        computeWindowSizeClass()
        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        val intent = Intent(this, GaiaGattService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        unbindService(connection)
        if (!isChangingConfigurations) {
            stopService(Intent(this, GaiaGattService::class.java))
        }
    }

    private fun setupNavigation(
        binding: ActivityFiioK9ControlBinding,
        navController: NavController
    ) {
        binding.navView?.setupWithNavController(navController)
        binding.navRail?.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, navDestination, _ ->
            val isSetup = navDestination.id == R.id.fragment_setup
            binding.navRail?.isVisible = !isSetup
            binding.navView?.isInvisible = isSetup
            window.navigationBarColor = SurfaceColors.getColorForElevation(
                this,
                if (binding.navView?.isVisible == true) {
                    binding.navView.elevation
                } else if (binding.navRail?.isVisible == true) {
                    binding.navRail.elevation
                } else {
                    0f
                }
            )
        }
    }

    private fun computeWindowSizeClass() {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        windowSizeClass = WindowSizeClass.calculate(widthDp, height = false)
    }
}
