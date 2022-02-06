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

package com.tomg.fiiok9control.gaia

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class GaiaGattSideEffect : Parcelable {

    sealed class Gaia : GaiaGattSideEffect() {

        @Parcelize
        object Ready : Gaia()

        @Parcelize
        data class Packet(
            val data: ByteArray
        ) : Gaia() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Packet
                if (!data.contentEquals(other.data)) return false
                return true
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }
    }

    sealed class Gatt : GaiaGattSideEffect() {

        @Parcelize
        object Disconnected : Gatt()

        @Parcelize
        object Error : Gatt()

        @Parcelize
        object Ready : Gatt()

        @Parcelize
        object ServiceDiscovery : Gatt()

        sealed class WriteCharacteristic : Gatt() {

            @Parcelize
            data class Success(
                val commandId: Int
            ) : WriteCharacteristic()

            @Parcelize
            data class Failure(
                val commandId: Int
            ) : WriteCharacteristic()
        }
    }
}
