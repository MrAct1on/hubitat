/**
 * Advanced Hue Bridge
 * Download: https://github.com/apwelsh/hubitat
 * Description:
 * This is a child device handler for the Advance Hue Bridge Integration App.  This device manage the hub directly for
 * actions such as refresh.
 *-------------------------------------------------------------------------------------------------------------------
 * Copyright 2020 Armand Peter Welsh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the 'Software'), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *-------------------------------------------------------------------------------------------------------------------
 **/

import groovy.transform.Field

@Field static final Boolean DEFAULT_AUTO_REFRESH     = false
@Field static final Number  DEFAULT_REFRESH_INTERVAL = 60
@Field static final Boolean DEFAULT_ANY_ON           = true
@Field static final Boolean DEFAULT_LOG_ENABLE       = true
@Field static final Boolean DEFAULT_DEBUG            = false

@Field static final Map SCHEDULE_NON_PERSIST = [overwrite: true, misfire:'ignore']

metadata {
    definition (
        name:      'AdvancedHueBridge',
        namespace: 'apwelsh',
        author:    'Armand Welsh',
        importUrl: 'https://raw.githubusercontent.com/apwelsh/hubitat/master/hue/device/advanced-hue-bridge.groovy') {
        capability 'Switch'
        capability 'Refresh'
    }
}

preferences {

    input name: 'autoRefresh',
          type: 'bool',
          defaultValue: DEFAULT_AUTO_REFRESH,
          title: 'Auto Refresh',
          description: 'Should this device support automatic refresh'

    if (autoRefresh) {
        input name: 'refreshInterval',
              type: 'number',
              defaultValue: DEFAULT_REFRESH_INTERVAL,
              title: 'Refresh Inteval',
              description: 'Number of seconds to refresh the group state'
    }

    input name: 'anyOn',
          type: 'bool',
          defaultValue: DEFAULT_ANY_ON,
          title: 'ANY on or ALL on',
          description: 'When ebabled, the group is considered on when any light is on'

    input name: 'logEnable',
          type: 'bool',
          defaultValue: DEFAULT_LOG_ENABLE,
          title: 'Enable informational logging'

    input name: 'debug',
          type: 'bool',
          defaultValue: DEFAULT_DEBUG,
          title: 'Enable debug logging'
}

/**
 * Hubitat DTH Lifecycle Functions
 **/
def installed() {
    updated()
}

def updated() {
    if (settings.autoRefresh     == null) { device.updateSetting('autoRefresh',     DEFAULT_AUTO_REFRESH) }
    if (settings.refreshInterval == null) { device.updateSetting('refreshInterval', DEFAULT_REFRESH_INTERVAL) }
    if (settings.anyOn           == null) { device.updateSetting('anyOn',           DEFAULT_ANY_ON) }
    if (settings.logEnable       == null) { device.updateSetting('logEnable',       DEFAULT_LOG_ENABLE) }

    if (logEnable) { log.debug 'Preferences updated' }
    refresh()
}

/*
 * Device Capability Interface Functions
 */

/** Switch Commands **/

void on() {
    if (logEnable) { log.info "Bridge (${this}) turning on" }
    parent.setDeviceState(this, ['on':true])
}

void off() {
    if (logEnable) { log.info "Bridge (${this}) turning off" }
    parent.setDeviceState(this, ['on': false])
}

void refresh() {
    if (debug) { log.debug "Bridge (${this}) refreshing" }
    parent.getDeviceState(this)
    parent.refreshHubStatus()
    resetRefreshSchedule()
}

void autoRefresh() {
    if (autoRefresh) {
        runIn(refreshInterval ?: DEFAULT_REFRESH_INTERVAL, refresh, SCHEDULE_NON_PERSIST)
    }
    refresh()
}

void resetRefreshSchedule() {
    unschedule()
    if (autoRefresh) {
        runIn(refreshInterval ?: DEFAULT_REFRESH_INTERVAL, refresh, SCHEDULE_NON_PERSIST)
    }
}

void setHueProperty(Map args) {
    if (args.name == (anyOn ? 'any_on' : 'all_on')) {
        parent.sendChildEvent(this, [name: 'switch', value: value ? 'on' : 'off'])
    }
}
