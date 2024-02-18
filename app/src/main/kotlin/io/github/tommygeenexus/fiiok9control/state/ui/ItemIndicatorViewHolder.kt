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

package io.github.tommygeenexus.fiiok9control.state.ui

import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.extension.relativeTo
import io.github.tommygeenexus.fiiok9control.core.fiiok9.FiioK9Defaults
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.IndicatorState
import io.github.tommygeenexus.fiiok9control.databinding.ItemIndicatorBinding
import kotlin.math.roundToInt

class ItemIndicatorViewHolder(
    private val binding: ItemIndicatorBinding,
    private val listener: StateAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.group.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.disabled -> {
                    if (binding.disabled.isPressed) {
                        listener.onIndicatorStateRequested(IndicatorState.Disabled)
                    }
                }
                R.id.enabled_default -> {
                    if (binding.enabledDefault.isPressed) {
                        listener.onIndicatorStateRequested(IndicatorState.EnabledDefault)
                    }
                }
                R.id.enabled_gradient_only -> {
                    if (binding.enabledGradientOnly.isPressed) {
                        listener.onIndicatorStateRequested(IndicatorState.EnabledGradientOnly)
                    }
                }
            }
        }
        binding.indicatorBrightnessSlider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {

                override fun onStartTrackingTouch(slider: Slider) {
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    listener.onIndicatorBrightnessRequested(slider.value.toInt())
                }
            }
        )
        binding.indicatorBrightnessSlider.setLabelFormatter { value ->
            itemView.context.getString(
                R.string.indicator_brightness_value,
                value.roundToInt().relativeTo(max = FiioK9Defaults.INDICATOR_BRIGHTNESS_MAX)
            )
        }
    }

    fun bindItemIndicator(
        indicatorState: IndicatorState,
        indicatorBrightness: Int,
        isItemEnabled: Boolean
    ) {
        when (indicatorState) {
            IndicatorState.Disabled -> binding.disabled.isChecked = true
            IndicatorState.EnabledDefault -> binding.enabledDefault.isChecked = true
            IndicatorState.EnabledGradientOnly -> binding.enabledGradientOnly.isChecked = true
        }
        binding.indicatorBrightnessSlider.value = indicatorBrightness.toFloat()
        binding.group.forEach { item ->
            item.isEnabled = isItemEnabled
        }
        binding.indicatorBrightnessSlider.isEnabled = isItemEnabled
    }
}
