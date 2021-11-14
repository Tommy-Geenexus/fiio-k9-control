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

package com.tomg.fiiok9control.eq.ui

import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.ItemEqBinding
import com.tomg.fiiok9control.eq.EqPreSet

class ItemEqViewHolder(
    private val binding: ItemEqBinding,
    private val listener: EqAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.eqEnabled.setOnCheckedChangeListener { view, isChecked ->
            if (view.isPressed) {
                listener.onEqEnabled(isChecked)
            }
        }
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.jazz -> {
                    if (binding.jazz.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Jazz)
                    }
                }
                R.id.pop -> {
                    if (binding.pop.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Pop)
                    }
                }
                R.id.rock -> {
                    if (binding.rock.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Rock)
                    }
                }
                R.id.dance -> {
                    if (binding.dance.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Dance)
                    }
                }
                R.id.pre_set_default -> {
                    if (binding.preSetDefault.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Default)
                    }
                }
                R.id.rb -> {
                    if (binding.rb.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Rb)
                    }
                }
                R.id.classical -> {
                    if (binding.classical.isPressed) {
                        listener.onEqPreSetRequested(EqPreSet.Classical)
                    }
                }
            }
        }
    }

    fun bindItemEq(
        eqEnabled: Boolean,
        eqPreSet: EqPreSet
    ) {
        binding.eqEnabled.isChecked = eqEnabled
        binding.group.children.forEach { child ->
            child.isEnabled = eqEnabled
        }
        when (eqPreSet) {
            EqPreSet.Classical -> binding.classical.isChecked = true
            EqPreSet.Dance -> binding.dance.isChecked = true
            EqPreSet.Default -> binding.preSetDefault.isChecked = true
            EqPreSet.Jazz -> binding.jazz.isChecked = true
            EqPreSet.Pop -> binding.pop.isChecked = true
            EqPreSet.Rb -> binding.rb.isChecked = true
            EqPreSet.Rock -> binding.rock.isChecked = true
        }
    }
}
