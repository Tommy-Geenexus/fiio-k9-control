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

package com.tomg.fiiok9control.setup.business

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tomg.fiiok9control.Empty
import com.tomg.fiiok9control.gaia.GaiaGattService
import com.tomg.fiiok9control.profile.data.Profile
import com.tomg.fiiok9control.setup.data.BleScanResult
import com.tomg.fiiok9control.setup.data.SetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext context: Context,
    private val setupRepository: SetupRepository
) : AndroidViewModel(context as Application),
    ContainerHost<SetupState, SetupSideEffect> {

    override val container = container<SetupState, SetupSideEffect>(
        initialState = SetupState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            requestPermissionsOrStartBleScan()
        }
    )

    private val scannedDeviceChannel = Channel<Result<BleScanResult>>(capacity = Channel.UNLIMITED)
    val scannedDeviceFlow = scannedDeviceChannel.receiveAsFlow()

    fun connectToDevice(
        scope: CoroutineScope,
        service: GaiaGattService?
    ) = intent {
        if (service != null) {
            reduce {
                state.copy(isLoading = true)
            }
            scope.launch(context = Dispatchers.IO) {
                val success = service.connect(state.deviceAddress)
                if (!success) {
                    handleConnectionEstablishFailed()
                }
            }
        } else {
            handleConnectionEstablishFailed()
        }
    }

    fun disconnect(service: GaiaGattService?) = intent {
        service?.disconnectAndReset()
    }

    fun handleBluetoothStateChange(enabled: Boolean) = intent {
        reduce {
            state.copy(
                bluetoothEnabled = enabled,
                deviceAddress = String.Empty,
                bonded = false
            )
        }
    }

    fun handleBluetoothBondStateChange(bonded: Boolean) = intent {
        reduce {
            state.copy(bonded = bonded)
        }
    }

    fun handleConnectionEstablishInProgress(deviceAddress: String) = intent {
        reduce {
            state.copy(isLoading = true)
        }
        val bonded = setupRepository.isDeviceBonded(deviceAddress)
        reduce {
            state.copy(
                deviceAddress = deviceAddress,
                bonded = bonded
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
                deviceAddress = String.Empty,
                bonded = false,
                isLoading = false
            )
        }
    }

    fun handleDeviceScanned(bleScanResult: BleScanResult) = intent {
        val deviceAddress = if (!bleScanResult.matchLost) {
            bleScanResult.deviceAddress
        } else {
            String.Empty
        }
        reduce {
            state.copy(
                deviceAddress = deviceAddress,
                bonded = bleScanResult.bonded,
                isLoading = false
            )
        }
    }

    fun handlePermissionsGrantResult(result: Map<String, Boolean>) = intent {
        val permissionsGranted = !result.containsValue(false)
        reduce {
            state.copy(permissionsGranted = permissionsGranted)
        }
    }

    fun handleShortcutProfileSelected(profile: Profile) = intent {
        reduce {
            state.copy(shortcutProfile = profile)
        }
    }

    private fun requestPermissionsOrStartBleScan() = intent {
        if (setupRepository.isBleSupported()) {
            if (setupRepository.arePermissionsGranted()) {
                reduce {
                    state.copy(
                        permissionsGranted = setupRepository.arePermissionsGranted(),
                        bluetoothEnabled = setupRepository.isBluetoothEnabled()
                    )
                }
            } else {
                postSideEffect(
                    SetupSideEffect.GrantPermissions(setupRepository.requiredPermissions)
                )
            }
        } else {
            postSideEffect(SetupSideEffect.Ble.Unsupported)
        }
    }

    fun startBleScan() = intent {
        val success = setupRepository.startBleScan(
            onScanResult = { result ->
                viewModelScope.launch {
                    scannedDeviceChannel.send(result)
                }
            }
        )
        if (success) {
            reduce {
                state.copy(isLoading = true)
            }
        }
    }

    fun stopBleScan() = intent {
        setupRepository.stopBleScan()
        reduce {
            state.copy(isLoading = false)
        }
    }

    fun synchronizeState() = intent {
        reduce {
            state.copy(permissionsGranted = setupRepository.arePermissionsGranted())
        }
    }
}
