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

package io.github.tommygeenexus.fiiok9control.profile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.profile.ProfileDiffCallback
import io.github.tommygeenexus.fiiok9control.databinding.ItemProfileBinding

class ProfileAdapter(private val listener: Listener) :
    ListAdapter<Profile, RecyclerView.ViewHolder>(ProfileDiffCallback) {

    interface Listener {

        fun onProfileShortcutAdd(position: Int)
        fun onProfileShortcutRemove(position: Int)
        fun onProfileApply(position: Int)
        fun onProfileDelete(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemProfileViewHolder(
        binding = ItemProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ),
        listener = listener
    )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ItemProfileViewHolder)?.bindItemProfile(
            profileName = currentList.getOrNull(position)?.name.orEmpty()
        )
    }
}
