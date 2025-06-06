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

package io.github.tommygeenexus.fiiok9control.core.fiiok9.feature

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class EqPreSet(
    val id: Int
) : Parcelable {

    companion object {

        fun findById(id: Int) = when (id) {
            Jazz.id -> Jazz
            Pop.id -> Pop
            Rock.id -> Rock
            Dance.id -> Dance
            Custom.id -> Custom
            Rb.id -> Rb
            Classical.id -> Classical
            else -> null
        }
    }

    @Parcelize
    data object Jazz : EqPreSet(id = 0)

    @Parcelize
    data object Pop : EqPreSet(id = 1)

    @Parcelize
    data object Rock : EqPreSet(id = 2)

    @Parcelize
    data object Dance : EqPreSet(id = 3)

    @Parcelize
    data object Custom : EqPreSet(id = 4)

    @Parcelize
    data object Rb : EqPreSet(id = 5)

    @Parcelize
    data object Classical : EqPreSet(id = 6)
}
