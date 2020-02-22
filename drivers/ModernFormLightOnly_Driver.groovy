/*
* Driver for Simple Form Fans - LIGHT ONLY can be used as a dimmer
* - See https://github.com/gjunky/hubitat-ModernForms/blob/master/drivers/ModernFormsFan.groovy for combined driver of Fan and Light
*
* Allows basic control of the Light part of Simple Form Fans
* 
*    Versions: 1.02   2019-12-10 - Fixed a bug in the sleep timer reporting 
 */
metadata {
    definition(name: "Modern Form Fan-Light-Only", namespace: "gjunky", author: "RobJodh@gmail.com") {
        capability "Switch"
        capability "SwitchLevel"
        command "GetStatus"
    }
}

preferences {
    section("URIs") {
        input "fanIP", "text", title: "Fan IP Address", required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
} 

def sendCommand(command, commandValue) {
    if (logEnable)  log.debug("------- in sendCommand --------")
    log.info("Fan IP : $fanIP, command: $command, commandValue: $commandValue")

def params = [
		uri: "http://" + fanIP,
		path: "/mf",
		contentType: "application/json",
		headers: ['something' : 'abc'],
		body: "{'$command' : $commandValue}"
	]
    
    if (logEnable) log.debug("params: $params")
    
    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                if (logEnable) log.debug "${it.name} : ${it.value}"
            }
            if (logEnable) log.debug("resp.data: $resp.data")
            if (logEnable) log.debug "response contentType: ${resp.contentType}"
            showStatus(resp.data)
		    return resp.data
        }
    } catch (e) {
        log.debug "something went wrong: $e"
        checkHttpResponse("error in $command", e.getResponse())
		return null
    }
}
/*
 * Command Section
 */
def on() {
    if (logEnable) log.debug "Sending Light On Command "
    sendCommand("lightOn", true)
}

def off() {
    if (logEnable) log.debug "Sending Light Off Command "
    sendCommand("lightOn", false)
}


def GetStatus() {
    if (logEnable) log.debug("Querying Fan")
    showStatus(sendCommand("queryDynamicShadowData", 1))
}


/*
 *    Set the light's brightness
 */
def setLevel(level) {
    if (logEnable) log.debug "Sending Light Level " + level
    sendCommand("lightBrightness", level)
}

/*
 *    Show the current status in the device page
 */
def showStatus(retData) {
    if (retData) {
        if (logEnable) log.debug("Show Status: $retData")
        def sleepUntil = 0
        
        if (retData.fanOn) device.sendEvent(name: "fan", value: "On")
        else device.sendEvent(name: "fan", value: "Off")
        device.sendEvent(name: "fanSpeed", value: retData.fanSpeed)
        device.sendEvent(name: "fanDirection", value: retData.fanDirection)
        if (retData.lightOn) device.sendEvent(name: "light", value: "On")
        else device.sendEvent(name: "light", value: "Off")
        device.sendEvent(name: "lightBrightness", value: retData.lightBrightness)
        if (retData.fanSleepTimer != 0) {
            sleepUntil = new Date((retData.fanSleepTimer as long)*1000).format( 'M-d-yyyy HH:mm-ss' )
            device.sendEvent(name: "fanSleepTimer", value: sleepUntil)
        }
        if (retData.lightSleepTimer) {
            sleepUntil = new Date((retData.lightSleepTimer as long)*1000).format( 'M-d-yyyy HH:mm-ss' )
            device.sendEvent(name: "lightSleepTimer", value: retData.lightSleepTimer)
        }
    }
}