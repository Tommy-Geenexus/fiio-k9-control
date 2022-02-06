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

package com.tomg.fiiok9control.audio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.audio.BluetoothCodec
import com.tomg.fiiok9control.audio.LowPassFilter
import com.tomg.fiiok9control.databinding.ItemChannelBalanceBinding
import com.tomg.fiiok9control.databinding.ItemCodecsEnabledBinding
import com.tomg.fiiok9control.databinding.ItemLowPassFilterBinding

class AudioAdapter(
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

        fun onBluetoothCodecChanged(
            codec: BluetoothCodec,
            enabled: Boolean
        )
        fun onChannelBalanceRequested(value: Int)
        fun onLowPassFilterRequested(lowPassFilter: LowPassFilter)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                ItemCodecsEnabledViewHolder(
                    ItemCodecsEnabledBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }
            1 -> {
                ItemLowPassFilterViewHolder(
                    ItemLowPassFilterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }
            else -> {
                ItemChannelBalanceViewHolder(
                    ItemChannelBalanceBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val payload = currentList.getOrNull(position)
        when (position) {
            0 -> {
                val codecsEnabled = payload as? List<BluetoothCodec>
                if (codecsEnabled != null) {
                    (holder as? ItemCodecsEnabledViewHolder)?.bindItemCodecsEnabled(codecsEnabled)
                }
            }
            1 -> {
                val lowPassFilter = payload as? LowPassFilter
                if (lowPassFilter != null) {
                    (holder as? ItemLowPassFilterViewHolder)?.bindItemLowPassFilter(lowPassFilter)
                }
            }
            else -> {
                val channelBalance = payload as? Int
                if (channelBalance != null) {
                    (holder as? ItemChannelBalanceViewHolder)?.bindItemChannelBalance(
                        channelBalance
                    )
                }
            }
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemCount() = 3
}
