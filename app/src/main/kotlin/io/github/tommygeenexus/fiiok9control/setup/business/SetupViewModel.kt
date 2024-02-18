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

package io.github.tommygeenexus.fiiok9control.setup.business

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tommygeenexus.fiiok9control.core.ble.BleScanResult
import io.github.tommygeenexus.fiiok9control.core.data.GaiaGattRepository
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattService
import io.github.tommygeenexus.fiiok9control.setup.data.SetupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.asSharedFlow
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val gaiaGattRepository: GaiaGattRepository,
    private val setupRepository: SetupRepository
) : AndroidViewModel(context as Application),
    ContainerHost<SetupState, SetupSideEffect> {

    override val container = container<SetupState, SetupSideEffect>(
        initialState = SetupState(),
        savedStateHandle = savedStateHandle,
        onCreate = { verifyBleSupportedAndPermissionsGranted() }
    )

    val bleScanResults = setupRepository.bleScanResults.asSharedFlow()

    fun connect(service: GaiaGattService) = intent {
        reduce {
            state.copy(isConnecting = true)
        }
        val success = gaiaGattRepository.connect(service, state.deviceAddress)
        reduce {
            state.copy(isConnecting = success)
        }
    }

    fun disconnect(service: GaiaGattService) = intent {
        reduce {
            state.copy(isDisconnecting = true)
        }
        service.disconnectAndReset()
        reduce {
            state.copy(isDisconnecting = false)
        }
    }

    fun handleBluetoothStateChange(isEnabled: Boolean) = intent {
        reduce {
            state.copy(
                deviceAddress = "",
                isBluetoothEnabled = isEnabled,
                isConnecting = if (isEnabled) state.isConnecting else false,
                isDeviceBonded = false,
                isScanning = if (isEnabled) state.isScanning else false
            )
        }
    }

    fun handleBluetoothBondStateChange(isBonded: Boolean) = intent {
        reduce {
            state.copy(isDeviceBonded = isBonded)
        }
    }

    fun handleConnectionEstablishInProgress(deviceAddress: String) = intent {
        reduce {
            state.copy(isConnecting = true)
        }
        val bonded = setupRepository.isDeviceBonded(deviceAddress)
        reduce {
            state.copy(
                deviceAddress = deviceAddress,
                isDeviceBonded = bonded
            )
        }
    }

    fun handleConnectionEstablished() = intent {
        val profile = state.shortcutProfile
        postSideEffect(
            if (profile != null) {
                SetupSideEffect.NavigateToProfile(profile)
            } else {
                SetupSideEffect.NavigateToState
            }
        )
    }

    fun handleConnectionEstablishFailed() = intent {
        reduce {
            state.copy(
                deviceAddress = "",
                isDeviceBonded = false,
                isConnecting = false
            )
        }
    }

    fun handleScanResult(bleScanResult: BleScanResult) = intent {
        val deviceAddress = if (!bleScanResult.isMatchLost) {
            bleScanResult.deviceAddress
        } else {
            ""
        }
        reduce {
            state.copy(
                deviceAddress = deviceAddress,
                isDeviceBonded = bleScanResult.isBonded
            )
        }
    }

    fun handlePermissionsGrantResult(result: Map<String, Boolean>) = intent {
        val permissionsGranted = !result.containsValue(false)
        reduce {
            state.copy(isPermissionsGranted = permissionsGranted)
        }
    }

    fun handleServiceConnectionStateChanged(isConnected: Boolean) = intent {
        val isBluetoothEnabled = setupRepository.isBluetoothEnabled()
        reduce {
            state.copy(
                isBluetoothEnabled = isBluetoothEnabled,
                isPermissionsGranted = setupRepository.arePermissionsGranted(),
                isServiceConnected = isConnected
            )
        }
    }

    fun handleShortcutProfileSelected(profile: Profile) = intent {
        reduce {
            state.copy(shortcutProfile = profile)
        }
    }

    fun startBleScan() = intent {
        val success = setupRepository.startBleScan()
        reduce {
            state.copy(isScanning = success)
        }
    }

    fun stopBleScan() = intent {
        setupRepository.stopBleScan()
        reduce {
            state.copy(isScanning = false)
        }
    }

    private fun verifyBleSupportedAndPermissionsGranted() = intent {
        if (!setupRepository.isBleSupported()) {
            postSideEffect(SetupSideEffect.Ble.Unsupported)
        } else if (!setupRepository.arePermissionsGranted()) {
            postSideEffect(SetupSideEffect.GrantPermissions(setupRepository.requiredPermissions))
        }
    }
}
