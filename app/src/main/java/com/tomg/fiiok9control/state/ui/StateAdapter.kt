/*
 * Copyright (c) 2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.tomg.fiiok9control.state.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.databinding.ItemIndicatorBinding
import com.tomg.fiiok9control.databinding.ItemInputBinding
import com.tomg.fiiok9control.databinding.ItemMiscBinding
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource

class StateAdapter(
    private val listener: Listener
) : ListAdapter<Any, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Any>() {

        override fun areItemsTheSame(
            oldItem: Any,
            newItem: Any
        ) = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: Any,
            newItem: Any
        ) = false
    }
) {

    interface Listener {

        fun onInputSourceRequested(inputSource: InputSource)
        fun onIndicatorStateRequested(indicatorState: IndicatorState)
        fun onIndicatorBrightnessRequested(indicatorBrightness: Int)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                ItemMiscViewHolder(
                    ItemMiscBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            1 -> {
                ItemInputViewHolder(
                    ItemInputBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }
            else -> {
                ItemIndicatorViewHolder(
                    ItemIndicatorBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val payload = currentList.getOrNull(position)
        when (position) {
            0 -> {
                val info = payload as? Pair<*, *>
                if (info != null) {
                    val fwVersion = payload.first as? String
                    val audioFmt = payload.second as? String
                    if (!fwVersion.isNullOrEmpty() && !audioFmt.isNullOrEmpty()) {
                        (holder as? ItemMiscViewHolder)?.bindItemMisc(fwVersion, audioFmt)
                    }
                }
            }
            1 -> {
                val inputSource = payload as? InputSource
                if (inputSource != null) {
                    (holder as? ItemInputViewHolder)?.bindItemInput(inputSource)
                }
            }
            else -> {
                val indicator = payload as? Pair<*, *>
                if (indicator != null) {
                    val indicatorState = payload.first as? IndicatorState
                    val indicatorBrightness = payload.second as? Int
                    if (indicatorState != null && indicatorBrightness != null) {
                        (holder as? ItemIndicatorViewHolder)?.bindItemIndicator(
                            indicatorState,
                            indicatorBrightness
                        )
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemCount() = 3
}
