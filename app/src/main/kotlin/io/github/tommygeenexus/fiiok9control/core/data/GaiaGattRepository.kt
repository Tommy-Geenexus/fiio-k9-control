/*
 * Copyright (c) 2023-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.core.data

import com.qualcomm.qti.libraries.gaia.packets.GaiaPacket
import io.github.tommygeenexus.fiiok9control.core.di.DispatcherIo
import io.github.tommygeenexus.fiiok9control.core.extension.suspendRunCatching
import io.github.tommygeenexus.fiiok9control.core.ui.gaia.GaiaGattService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class GaiaGattRepository @Inject constructor(
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    private companion object {

        const val GAIA_CMD_DELAY_MS = 200L
    }

    suspend fun connect(service: GaiaGattService, deviceAddress: String): Boolean =
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                service.connect(deviceAddress)
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }

    suspend fun sendGaiaPacket(service: GaiaGattService, packet: GaiaPacket): Boolean =
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val result = service.sendGaiaPacket(packet)
                delay(GAIA_CMD_DELAY_MS)
                result
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }

    suspend fun sendGaiaPackets(service: GaiaGattService, packets: List<GaiaPacket>): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                packets.forEach { packet ->
                    val result = service.sendGaiaPacket(packet)
                    delay(GAIA_CMD_DELAY_MS)
                    if (!result) {
                        return@suspendRunCatching false
                    }
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }
}
