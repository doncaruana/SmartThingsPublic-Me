/**
 *  Strips by Sensative
 *  Device Handler by Don Caruana
 *
 *  Date: 2017-2-19
 *  Supported Command Classes per device specs
 *  
 *         Association v2
 *         Association Group Information
 *         Battery
 *         Binary Sensor
 *         Configuration
 *         Device Reset Local
 *         Manufacturer Specific
 *         Notification v4
 *         Powerlevel
 *         Version v2
 *         Wake Up v2
 *         ZWavePlus Info v2
 *
 *   Parm Size Description                                   Value
 *      1    1 Type of report to send                        1 (Default)-Notification Report, 0-Binary Sensor report, 2-Basic report
 *      2    1 LED Indication                                1 (Default)-On for event (ex. door opened), 0-Off
 * 
 *    This device handler will just override the smartthings default wakeup interval of 4 hours and set to 24 hours (manufacturer default)
 *      and check the battery once a day (no sooner than every 23 hours)
 */

metadata {
	definition (name: "Strips by Sensative", namespace: "doncaruana", author: "Don Caruana") {

	capability "Contact Sensor"
	capability "Configuration"
	capability "Battery"
	capability "Sensor"
	capability "Refresh"
		attribute  "needUpdate", "string"
     
    fingerprint mfr:"019A", prod:"0003", model:"0003", deviceJoinName:"Strips by Sensative"
    fingerprint deviceId:"0x0701", inClusters: "0x5E,0x86,0x72,0x30,0x70,0x71,0x5A,0x85,0x59,0x80,0x84,0x73"
    fingerprint cc: "0x5E,0x86,0x72,0x30,0x70,0x71,0x5A,0x85,0x59,0x80,0x84,0x73", mfr:"019A", prod:"0003", model:"0003", deviceJoinName:"Strips by Sensative"
	}

    preferences {
    input(
        title : "Settings marked with * will not change until the next wakeup."
        ,description : null
        ,type : "paragraph"
        )
		input "led", "bool", 
			title: "*LED On",
			defaultValue: true,
			displayDuringSetup: false
		input "sendType", "enum", 
			title: "*Reporting Type",
			options:["binary": "Binary", "notification": "Notification", "basic": "Basic"],
			defaultValue: "notification",
			displayDuringSetup: false
		input "invertOutput", "bool", 
			title: "Invert open/closed reporting",
			defaultValue: false,
			displayDuringSetup: false
		input "wakeupInterval",
        	"enum",
            title: "*Device Wakeup Interval",
            description: "A value in seconds.",
            defaultValue: 96000,
            required: false,
            displayDuringSetup: false,
            options: buildInterval()
		input "allowWakeup", "bool", 
			title: "Allow manual wakeup",
			defaultValue: false,
			displayDuringSetup: false
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
				attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			}
		}
		valueTile("tamper", "device.tamper", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "tamper", label:'${currentValue}', backgroundColor:"#ffffff", action:"refresh.refresh"
		}
    standardTile("configure", "device.needUpdate", inactiveLabel: false, width: 2, height: 2) {
        state "NO" , label:'Synced', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#99CC33"
        state "YES", label:'Pending', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#CCCC33"
    }
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		main (["contact"])
		details(["contact","battery","configure","tamper"])
	}

	simulator {
		// messages the device returns in response to commands it receives
		status "open (basic)" : "command: 9881, payload: 00 20 01 FF"
		status "closed (basic)" : "command: 9881 payload: 00 20 01 00"
		status "open (notification)" : "command: 9881, payload: 00 71 05 06 FF 00 FF 06 16 00 00"
		status "closed (notification)" : "command: 9881, payload: 00 71 05 06 00 00 FF 06 17 00 00"
		status "wake up" : "command: 9881, payload: 00 84 07"
		status "battery (100%)" : "command: 9881, payload: 00 80 03 64"
		status "battery low" : "command: 9881, payload: 00 80 03 FF"
	}
}

def configure() {
	def commands = []  
	//Since there is a bug in SmartThings that prevents reading wakeupinterval and we know, by default, SmartThings
	//sets it to 4 hours, we'll set this value to 24 hours (manufacturer default) to begin.
	state.wakeupInterval = "96000"
	state.lastupdate = now()
	sendEvent(name: "tamper", value: "No tamper", displayed: false)

	log.debug "Listing all device parameters and defaults since this is a new inclusion"

	commands << zwave.wakeUpV1.wakeUpIntervalSet(seconds: state.wakeupInterval.toInteger(), nodeid:zwaveHubNodeId).format()
	commands << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	commands << zwave.versionV1.versionGet().format()
	commands << zwave.batteryV1.batteryGet().format()
	commands << zwave.configurationV1.configurationGet(parameterNumber: 1).format()
	commands << zwave.configurationV1.configurationGet(parameterNumber: 2).format()
//  commands << zwave.wakeUpV1.wakeUpIntervalGet().format()
	delayBetween(commands, 1000)
}


private getCommandClassVersions() {
	[
		0x71: 3,  // Notification
		0x5E: 2,  // ZwaveplusInfo
		0x59: 1,  // AssociationGrpInfo
		0x85: 2,  // Association
		0x80: 1,  // Battery
		0x70: 1,  // Configuration
		0x5A: 1,  // DeviceResetLocally
		0x72: 1,  // ManufacturerSpecific
		0x73: 1,  // Powerlevel
		0x84: 1,  // WakeUp
		0x86: 1,  // Version
		0x30: 1,  // Binary Sensor
	]
}

// Parse incoming device messages to generate events
def parse(String description) {
	def result = []
	def cmd
	if (description.startsWith("Err 106")) {
		state.sec = 0
		result = createEvent( name: "secureInclusion", value: "failed", eventType: "ALERT",
				descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
	} else if (description.startsWith("Err")) {
		result = createEvent(descriptionText: "$device.displayName $description", isStateChange: true)
	} else {
		cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	if (result instanceof List) {
		result = result.flatten()
	}
	log.debug "Parsed '$description' to $result"
	return result
}


/**
* Triggered when Done button is pushed on Preference Pane
*/
def updated()
{
	if(now() - state.lastupdate > 3000){
		def isUpdateNeeded = "NO"
		if(wakeupInterval != null && state.wakeupInterval != wakeupInterval) {isUpdateNeeded = "YES"}
		if (sendType != null && sendType != state.sendType) {isUpdateNeeded = "YES"}
		if (led != null && led != state.led) {isUpdateNeeded = "YES"}
		state.lastupdate = now()
		sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
	}
}

/**
* Only device parameter changes require a state change 
*/
def update_settings()
{

  def cmds = []
  def isUpdateNeeded = "NO"
  // can't read the wakeup interval, so just set it to the pref value here
	if (state.wakeupInterval != wakeupInterval){
		state.wakeupInterval = wakeupInterval
		cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds: wakeupInterval.toInteger(), nodeid:zwaveHubNodeId).format()
		//cmds << zwave.wakeUpV1.wakeUpIntervalGet().format()
	}
	if (sendType != state.sendType){
		cmds << zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, configurationValue: [sendType == "binary" ? 0 : sendType == "basic" ? 2 : 1]).format()
		cmds << "delay 1000"
		cmds << zwave.configurationV1.configurationGet(parameterNumber: 1).format()
	}
	if (led != state.led){
		cmds << zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, configurationValue: [led == true ? 1 : 0]).format()
		cmds << "delay 1000"
		cmds << zwave.configurationV1.configurationGet(parameterNumber: 2).format()
	}
  cmds << "delay 1000"
  sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
  return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def name = ""
    def value = ""
    def tmpParm = cmd.parameterNumber

    def reportValue = cmd.configurationValue[0]
    switch (cmd.parameterNumber) {
        case 1:
            name = "sendType"
						switch (reportValue) {
							case 0:
							  value = "binary"
							  break
							case 1:
							  value = "notification"
							  break
							case 2:
							  value = "basic"
							  break
							default:
							  break
						}
						state.sendType = value
            log.debug "sendType = $value"
            break
        case 2:
            name = "led"
            value = reportValue
            log.debug "led = $value"
            state.led = reportValue == 1 ? true : false
            break
        default:
            break
    }
	sendEvent(name: name, value: value, displayed: true)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	log.debug "encapsulated: $encapsulatedCommand"
	if (encapsulatedCommand) {
		state.sec = 1
		return zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		return [createEvent(descriptionText: cmd.toString())]
	}
}

def sensorValueEvent(value) {
	//If the invertOutput parameter is set, logically invert the output value
    def flip = 0
    if (state.sendType == "notification"){
		flip = value ^ 0x1}
	else{
		flip = value ^ 0xFF}
  if (invertOutput) {value = flip}
	if (value) {
		createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
	} else {
		createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	return sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	return sensorValueEvent(cmd.value)
}

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	return sensorValueEvent(cmd.sensorValue)
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd) {
	return sensorValueEvent(cmd.sensorState)
}


def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd) {
	def result = []
	if (cmd.notificationType == 0x06 && cmd.event == 0x16) {
		result << sensorValueEvent(1)
	} else if (cmd.notificationType == 0x06 && cmd.event == 0x17) {
		result << sensorValueEvent(0)
	} else if (cmd.notificationType == 0x07) {
		if (cmd.event == 0x00) {
			if (cmd.eventParametersLength == 0 || cmd.eventParameter[0] != 3) {
				result << createEvent(descriptionText: "$device.displayName covering replaced", isStateChange: true, displayed: false)
			} else {
				result << sensorValueEvent(0)
			}
		} else if (cmd.event == 0x01 || cmd.event == 0x02) {
			result << sensorValueEvent(1)
		} else if (cmd.event == 0x03) {
			result << createEvent(descriptionText: "$device.displayName covering was removed", isStateChange: true)
			if (!device.currentState("ManufacturerCode")) {
  				result << response(secure(zwave.manufacturerSpecificV1.manufacturerSpecificGet()))
			}
		} else if (cmd.event == 0x04) {
            if (allowWakeup) {
				sendEvent(name:"WakeUp", value: "Manual Wakeup", descriptionText: "${device.displayName} woke up", isStateChange: true, displayed: true)
				result << doWakeup()
            }
            else {
			def timeString1 = new Date().format("MMM d yyyy", location.timeZone)
			def timeString2 = new Date().format("hh:mm:ss", location.timeZone)
			result << createEvent(name: "tamper", value: "Tamper at \n${timeString1}\n${timeString2}", descriptionText: "$device.displayName was tampered with at ${timeString1} ${timeString2}")
            }
		} else if (cmd.event == 0x05 || cmd.event == 0x06) {
			result << createEvent(descriptionText: "$device.displayName detected glass breakage", isStateChange: true)
		} else {
			result << createEvent(descriptionText: "$device.displayName event $cmd.event ${cmd.eventParameter.inspect()}", isStateChange: true, displayed: false)
		}
	} else if (cmd.notificationType) {
		result << createEvent(descriptionText: "$device.displayName notification $cmd.notificationType event $cmd.event ${cmd.eventParameter.inspect()}", isStateChange: true, displayed: false)
	} else {
		def value = cmd.v1AlarmLevel == 255 ? "active" : cmd.v1AlarmLevel ?: "inactive"
		result << createEvent(name: "alarm $cmd.v1AlarmType", value: value, isStateChange: true, displayed: false)
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
	def event = createEvent(name: "WakeUp", value: "Auto Wakeup", descriptionText: "${device.displayName} woke up", isStateChange: true, displayed: false)
	def cmds = []

	if (!device.currentState("ManufacturerCode")) {
		cmds << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
		cmds << "delay 2000"
	}
	if (!state.lastbat || now() - state.lastbat > 23*60*60*1000) {  //check no sooner than once every 23 hours (once a day)
		log.debug "checking battery"
		event.descriptionText += ", requesting battery"
		cmds << zwave.batteryV1.batteryGet().format()
		cmds << "delay 2000"
	} else {
		log.debug "not checking battery, was updated ${(now() - state.lastbat)/60000 as int} min ago"
	}
	if (device.currentValue("needUpdate") == "YES") { cmds += update_settings() }
	cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	return [event, response(cmds)]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
  def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "${device.displayName} has a low battery"
		map.isStateChange = true
	} else {
		map.value = cmd.batteryLevel
	}
	def event = createEvent(map)

	map.isStateChange = true

	state.lastbat = now()
	return [event]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv1.ManufacturerSpecificReport cmd) {
	def result = []
	def manufacturerCode = String.format("%04X", cmd.manufacturerId)
	def productTypeCode = String.format("%04X", cmd.productTypeId)
	def productCode = String.format("%04X", cmd.productId)
	def wirelessConfig = "ZWP"

	updateDataValue("Manufacturer", cmd.manufacturerName)
	updateDataValue("Manufacturer ID", manufacturerCode)
	updateDataValue("Product Type", productTypeCode)
	updateDataValue("Product Code", productCode)
	return result
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	return [createEvent(descriptionText: "$device.displayName: $cmd", displayed: false)]
}

private secure(physicalgraph.zwave.Command cmd) {
	if (state.sec == 0) {  // default to secure
		cmd.format()
	} else {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	}
}

private secureSequence(commands, delay=200) {
	delayBetween(commands.collect{ secure(it) }, delay)
}

// Strips actual wakeup time is 90% of the setting, so the values below are adjusted so the desired time interval is obtained. The minimum is 30 minutes
def buildInterval() {

  def intervalList = []
   intervalList << [ "2000" : "30 minutes" ]
   intervalList << [ "4000" : "1 hour" ]
   intervalList << [ "8000" : "2 hours" ]
   intervalList << [ "12000" : "3 hours" ]
   intervalList << [ "16000" : "4 hours" ]
   intervalList << [ "20000" : "5 hours" ]
   intervalList << [ "24000" : "6 hours" ]
   intervalList << [ "40000" : "10 hours" ]
   intervalList << [ "48000" : "12 hours" ]
   intervalList << [ "96000" : "1 day" ]
   intervalList << [ "192000" : "2 days" ]
   intervalList << [ "288000" : "3 days" ]
   intervalList << [ "384000" : "4 days" ]
   intervalList << [ "480000" : "5 days" ]
   intervalList << [ "576000" : "6 days" ]
   intervalList << [ "672000" : "1 week" ]
   intervalList << [ "1344000" : "2 weeks" ]
   intervalList << [ "2016000" : "3 weeks" ]
   intervalList << [ "2880000" : "1 month" ]
}

def doWakeup() {	
 	def cmds = []
	if (device.currentValue("needUpdate") == "YES") { cmds += update_settings() }
	cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	return response(cmds)
}

def refresh() {
 sendEvent(name: "tamper", value: "No tamper", displayed: false)
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
 	def appversion = String.format("%02d.%02d", cmd.applicationVersion, cmd.applicationSubVersion)
 	def zprotoversion = String.format("%d.%02d", cmd.zWaveProtocolVersion, cmd.zWaveProtocolSubVersion)
	updateDataValue("zWave Library", cmd.zWaveLibraryType.toString())
	updateDataValue("Firmware", appversion)
	updateDataValue("zWave Version", zprotoversion)
	sendEvent(name: "Firmware", value: appversion, displayed: true)
}
