/*
 * Copyright (c) 2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.core.view.forEach
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.gaia.data.fiio.FiioK9Defaults

class StateMenuProvider(
    private val isHpPreSimultaneouslyEnabled: () -> Boolean,
    private val isLoading: () -> Boolean,
    private val isMqaEnabled: () -> Boolean,
    private val isMuteEnabled: () -> Boolean,
    private val isServiceConnected: () -> Boolean,
    private val volumeStepSize: () -> Int,
    private val onDisconnect: () -> Unit,
    private val onExportProfile: () -> Unit,
    private val onToggleHpPreSimultaneously: () -> Unit,
    private val onToggleMqaEnabled: () -> Unit,
    private val onToggleMuteEnabled: () -> Unit,
    private val onStandby: () -> Unit,
    private val onReset: () -> Unit,
    private val onVolumeUp: () -> Unit,
    private val onVolumeDown: () -> Unit,
    private val onVolumeStepSizeChanged: (Int) -> Unit
) : MenuProvider {

    override fun onCreateMenu(
        menu: Menu,
        menuInflater: MenuInflater
    ) {
        menuInflater.inflate(R.menu.menu_state, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.forEach { item ->
            item.isEnabled = isServiceConnected() && !isLoading()
        }
        if (isMuteEnabled()) {
            menu.findItem(R.id.mute_on).isChecked = true
        } else {
            menu.findItem(R.id.mute_off).isChecked = true
        }
        when (volumeStepSize()) {
            FiioK9Defaults.VOLUME_STEP_SIZE_MIN -> {
                menu.findItem(R.id.volume_step_size_1).isChecked = true
            }
            FiioK9Defaults.VOLUME_STEP_SIZE_MIN + 1 -> {
                menu.findItem(R.id.volume_step_size_2).isChecked = true
            }
            FiioK9Defaults.VOLUME_STEP_SIZE_MIN + 2 -> {
                menu.findItem(R.id.volume_step_size_3).isChecked = true
            }
            else -> {
                menu.findItem(R.id.volume_step_size_4).isChecked = true
            }
        }
        if (isMqaEnabled()) {
            menu.findItem(R.id.mqa_on).isChecked = true
        } else {
            menu.findItem(R.id.mqa_off).isChecked = true
        }
        if (isHpPreSimultaneouslyEnabled()) {
            menu.findItem(R.id.hp_pre_simultaneously_on).isChecked = true
        } else {
            menu.findItem(R.id.hp_pre_simultaneously_off).isChecked = true
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.disconnect -> {
                onDisconnect()
                true
            }
            R.id.export -> {
                onExportProfile()
                true
            }
            R.id.hp_pre_simultaneously_on,
            R.id.hp_pre_simultaneously_off -> {
                onToggleHpPreSimultaneously()
                true
            }
            R.id.mqa_on,
            R.id.mqa_off -> {
                onToggleMqaEnabled()
                true
            }
            R.id.mute_on,
            R.id.mute_off -> {
                onToggleMuteEnabled()
                true
            }
            R.id.standby -> {
                onStandby()
                true
            }

            R.id.reset -> {
                onReset()
                true
            }
            R.id.volume_up -> {
                onVolumeUp()
                true
            }
            R.id.volume_down -> {
                onVolumeDown()
                true
            }
            R.id.volume_step_size_1 -> {
                onVolumeStepSizeChanged(FiioK9Defaults.VOLUME_STEP_SIZE_MIN)
                true
            }
            R.id.volume_step_size_2 -> {
                onVolumeStepSizeChanged(FiioK9Defaults.VOLUME_STEP_SIZE_MIN + 1)
                true
            }
            R.id.volume_step_size_3 -> {
                onVolumeStepSizeChanged(FiioK9Defaults.VOLUME_STEP_SIZE_MIN + 2)
                true
            }
            R.id.volume_step_size_4 -> {
                onVolumeStepSizeChanged(FiioK9Defaults.VOLUME_STEP_SIZE_MAX)
                true
            }
            else -> false
        }
    }
}
