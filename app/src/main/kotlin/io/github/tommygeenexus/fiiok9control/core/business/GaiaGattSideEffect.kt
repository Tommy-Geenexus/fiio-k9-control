/*
 * Copyright (c) 2023-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.core.business

import android.os.Parcelable
import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE
import kotlinx.parcelize.Parcelize

sealed class GaiaGattSideEffect : Parcelable {

    sealed class Gaia : GaiaGattSideEffect() {

        @Parcelize
        data object Ready : Gaia()
    }

    sealed class Gatt : GaiaGattSideEffect() {

        @Parcelize
        data object Disconnected : Gatt()

        @Parcelize
        data object Error : Gatt()

        @Parcelize
        data class Ready(
            val deviceAddress: String
        ) : Gatt()

        @Parcelize
        data class ServiceDiscovery(
            val deviceAddress: String
        ) : Gatt()

        sealed class Characteristic : Gatt() {

            sealed class Write : Characteristic() {

                @Parcelize
                data class Success(
                    val packet: GaiaPacketBLE
                ) : Write()

                @Parcelize
                data class Failure(
                    val packet: GaiaPacketBLE
                ) : Write()
            }

            @Parcelize
            data class Changed(
                val packet: GaiaPacketBLE
            ) : Characteristic()
        }
    }
}
