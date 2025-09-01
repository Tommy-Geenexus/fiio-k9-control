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

package io.github.tommygeenexus.fiiok9control

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import dagger.hilt.android.AndroidEntryPoint
import io.github.tommygeenexus.fiiok9control.core.business.GaiaGattSideEffect
import io.github.tommygeenexus.fiiok9control.core.extension.resolveThemeAttribute
import io.github.tommygeenexus.fiiok9control.core.extension.setCutoutForegroundColor
import io.github.tommygeenexus.fiiok9control.core.receiver.BluetoothStateBroadcastReceiver
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattService
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattServiceConnection
import io.github.tommygeenexus.fiiok9control.core.util.KEY_BLUETOOTH_ENABLED
import io.github.tommygeenexus.fiiok9control.databinding.ActivityFiioK9ControlBinding
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
        onBluetoothStateChanged = { isEnabled ->
            supportFragmentManager.setFragmentResult(
                KEY_BLUETOOTH_ENABLED,
                bundleOf(KEY_BLUETOOTH_ENABLED to isEnabled)
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

    lateinit var windowSizeClass: WindowSizeClass

    init {
        addOnNewIntentListener { intent -> setIntent(intent) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        with(ActivityFiioK9ControlBinding.inflate(layoutInflater)) {
            setContentView(root)
            computeWindowSizeClasses()
            setSupportActionBar(toolbar)
            setupNavigation(this, findNavController())
            setCutoutForegroundColor(
                color = resolveThemeAttribute(
                    attrRes = com.google.android.material.R.attr.colorSurfaceContainer
                )
            )
            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsetsCompat ->
                val insets = windowInsetsCompat.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                root.updateLayoutParams<FrameLayout.LayoutParams> {
                    leftMargin = insets.left
                    topMargin = insets.top
                    rightMargin = insets.right
                }
                windowInsetsCompat
            }
            layout.addView(
                object : View(this@FiioK9ControlActivity) {
                    override fun onConfigurationChanged(newConfig: Configuration?) {
                        super.onConfigurationChanged(newConfig)
                        computeWindowSizeClasses()
                    }
                }
            )
        }
        ContextCompat.registerReceiver(
            this,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val intent = Intent(this, GaiaGattService::class.java)
        startService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
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
        }
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        windowSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp, heightDp)
    }

    private fun findNavController(): NavController =
        (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            .navController
}
