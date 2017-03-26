/**
 *  Aeotec Recessed Door Sensor Gen5
 *  Device Handler by Don Caruana
 *
 *  Date: 2017-1-29
 *  Supported Command Classes
 *  
 *         Association
 *         Association Group Information
 *         Battery
 *         Binary Sensor
 *         Configuration
 *         Device Reset Local
 *         Firmware Update Meta Data
 *         Manufacturer Specific
 *         Notification
 *         Powerlevel
 *         Security
 *         Version
 *         Wake Up
 *         ZWavePlus Info
 *  
 *  
 * Controlled Command Classes
 *  
 *         Basic
 *         Hail
 *
 *   Parm Size Description                                   Value
 *      1    1 Which value to send for Sensor Binary Report  0 (Default)-Sensor triggered on=0xFF, 1-Sensor triggered on=0x00
 *      3    1 Which value is sent for Sensor Basic Report   0 (Default)-Sensor triggered on=0xFF, 1-Sensor triggered on=0x00
 *     39    1 Low Battery threshold                         10 (Default), 10-50, Threshold below which a low battery is reported, must enable 101 and 111 to work
 *    101    1 Enable the function of low battery checking   0 (Default)-Disable low battery checking, 1-Enable low battery checking
 *    111    4 Set the interval time of the battery report   86640 (Default)-disabled, 1 to 2147483647 in seconds
 *    121    4 Which command to send                         256 (Default)-Basic, 16-Binary, 272-Both
 *    252    1 Enable Configuration parameters to be locked  0 (Default)-Disable all configuration parameters to be locked, 1-Enable all configuration parameters to be locked
 *    254    2 Device Tag                                    0 to 65535 The range of the device tag is 65535
 *
 *    Enabling battery checking or setting the battery interval (which is rounded to 4 minute intervals anyway) seems to have no effect whatsoever. The
 *      wakeup interval, although it's supposed to be a different setting, follows this same, 4 minute rule. It's pointless to set either.
 * 
 *    This device handler will just rely on the smartthings default wakeup interval of 4 hours and check the battery once a day (no sooner than every 23 hours)
 */

metadata {
	definition (name: "Aeotec Recessed Door Sensor Gen5", namespace: "doncaruana", author: "Don Caruana") {

		capability "Contact Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Sensor"
			attribute   "needUpdate", "string"

//zw:Ss type:0701 mfr:0086 prod:0102 model:0059 ver:1.13 zwv:3.92 lib:03 cc:5E,86,72,98 ccOut:5A,82 sec:30,80,84,70,85,59,71,7A,73 role:06 ff:8C00 ui:8C00
       
    fingerprint mfr:"0086", prod:"0102", model:"0059", deviceJoinName:"Aeotec Recessed Door Sensor Gen5"
    fingerprint deviceId:"0x0701", inClusters: "0x5E,0x86,0x72,0x98", outClusters:"0x5A,0x82"
    fingerprint cc: "0x5E,0x86,0x72,0x98", ccout:"0x5A,0x82", mfr:"0086", prod:"0102", model:"0059", deviceJoinName:"Aeotec Recessed Door Sensor Gen5"
	}

    preferences {
    input(
        title : "Settings with * are physical and will not change until the next wakeup."
        ,description : null
        ,type : "paragraph"
        )
		input "lowBatteryCheck", "bool", 
			title: "*Enable low battery checking?",
			defaultValue: false,
			displayDuringSetup: false
		input "batteryInterval",
        	"enum",
            title: "*Low Battery Interval - Only used if Low Battery Checking is enabled",
            description: "24 hours",
            defaultValue: "86400",
            required: false,
            displayDuringSetup: false,
            options: buildInterval()
		input "wakeupInterval",
        	"enum",
            title: "*Device Wakeup Interval",
            description: "24 hours",
            defaultValue: "14400",
            required: false,
            displayDuringSetup: false,
            options: buildInterval()
		input "invertOutput", "bool", 
			title: "Invert open/closed reporting",
			defaultValue: false,
			displayDuringSetup: true
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
				attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			}
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}% battery', unit:""
		}
    standardTile("configure", "device.needUpdate", inactiveLabel: false, width: 2, height: 2) {
        state "NO" , label:'Synced', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#99CC33"
        state "YES", label:'Pending', action:"configuration.configure", icon:"st.secondary.refresh-icon", backgroundColor:"#CCCC33"
    }

		main (["contact"])
		details(["contact","battery","configure"])
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
	def cmds = []

	if (state.sec != 1) {
		// secure inclusion may not be complete yet
		cmds << "delay 1000"
	}
  
	state.lastupdate = now()

	log.debug "Listing all device parameters and defaults since this is new inclusion"
	cmds += secureSequence([
		zwave.manufacturerSpecificV2.manufacturerSpecificGet(),
		zwave.versionV1.versionGet(),
		zwave.batteryV1.batteryGet(),
		zwave.wakeUpV1.wakeUpIntervalSet(seconds: 14400, nodeid:zwaveHubNodeId),
		zwave.wakeUpV1.wakeUpIntervalGet(),

//  The actual default for the battery interval (111) is 86640, instead of 86400, which is not selectable as a value. 
//  Set this to the standard value up front to avoid a need for an update on a standard value
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 86400),
		zwave.configurationV1.configurationGet(parameterNumber: 1),
		zwave.configurationV1.configurationGet(parameterNumber: 3),
		zwave.configurationV1.configurationGet(parameterNumber: 101),
		zwave.configurationV1.configurationGet(parameterNumber: 111),
		zwave.configurationV1.configurationGet(parameterNumber: 121),
		zwave.configurationV1.configurationGet(parameterNumber: 252),
		zwave.configurationV1.configurationGet(parameterNumber: 254),
	], 500)

	cmds << "delay 8000"
	cmds << secure(zwave.wakeUpV1.wakeUpNoMoreInformation())
	return cmds
}

private getCommandClassVersions() {
	[
		0x71: 3,  // Notification
		0x5E: 2,  // ZwaveplusInfo
		0x59: 1,  // AssociationGrpInfo
		0x85: 2,  // Association
		0x30: 1,  // Binary
		0x80: 1,  // Battery
		0x70: 1,  // Configuration
		0x5A: 1,  // DeviceResetLocally
		0x7A: 2,  // FirmwareUpdateMd
		0x72: 2,  // ManufacturerSpecific
		0x73: 1,  // Powerlevel
		0x98: 1,  // Security
		0x84: 2,  // WakeUp
		0x86: 1,  // Version
	]
}

// Parse incoming device messages to generate events
def parse(String description) {
	def result = []
	def cmd
	def usedSecureWUI = false
	log.debug "${description}"
	if (description.startsWith("Err 106")) {
		state.sec = 0
		result = createEvent( name: "secureInclusion", value: "failed", eventType: "ALERT",
				descriptionText: "This sensor failed to complete the network security key exchange. If you are unable to control it via SmartThings, you must remove it from your network and add it again.")
	} else if (description.startsWith("Err")) {
		result = createEvent(descriptionText: "$device.displayName $description", isStateChange: true)
	} else {
	    if (description.indexOf('command: 9881, payload: 00 84') > -1) {
    		usedSecureWUI = secureWUIhandler(description)
            log.debug "outcome: ${usedSecureWUI}"
    	}
		if (!usedSecureWUI) {
        	cmd = zwave.parse(description, commandClassVersions)
			if (cmd) {
				result = zwaveEvent(cmd)
			}
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
		if(batteryInterval!= null && state.batteryInterval != batteryInterval) {isUpdateNeeded = "YES"}
		if(wakeupInterval!= null && state.wakeupInterval != wakeupInterval) {isUpdateNeeded = "YES"}
		if(lowBatteryCheck != null && lowBatteryCheck != state.lowBatteryCheck) {isUpdateNeeded = "YES"}
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
	if (state.wakeupInterval != wakeupInterval){
		// can't read the wakeup interval, so just set it to the pref value here
		state.wakeupInterval = wakeupInterval
		cmds << zwave.wakeUpV1.wakeUpIntervalSet(seconds: wakeupInterval.toInteger(), nodeid:zwaveHubNodeId)
		cmds << zwave.wakeUpV1.wakeUpIntervalGet()
	}
	if (sendType != state.sendType){
		cmds << zwave.configurationV1.configurationSet(parameterNumber: 121, size: 4, scaledConfigurationValue: [sendType == "binary" ? 16 : sendType == "basic" ? 256 : 272])
		cmds << "delay 1000"
		cmds << zwave.configurationV1.configurationGet(parameterNumber: 121)
	}
	if (lowBatteryCheck != state.lowBatteryCheck){
		cmds << zwave.configurationV1.configurationSet(parameterNumber: 101, size: 1, configurationValue: [(lowBatteryCheck) ? 1 : 0])
		cmds << "delay 1000"
		cmds << zwave.configurationV1.configurationGet(parameterNumber: 101)
	}
	if (batteryInterval != state.batteryInterval){
		cmds << zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: batteryInterval.toInteger())
		cmds << "delay 1000"
		cmds << zwave.configurationV1.configurationGet(parameterNumber: 111)
	}
  cmds = secureSequence(cmds, 1000)
	cmds << "delay 3000"
  sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
  return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def name = ""
    def value = ""
    def reportValue = cmd.scaledConfigurationValue
    switch (cmd.parameterNumber) {
        case 1:
            name = "invertOutputBinary"
            state.invertOutputBinary = value
            value = reportValue == 1 ? true : false
            log.debug "invertOutputBinary = ${value}"
            break
        case 3:
            name = "invertOutputBasic"
            state.invertOutputBasic = value
            value = reportValue == 1 ? true : false
            log.debug "invertOutputBasic = ${value}"
            break
        case 101:
            name = "lowBatteryCheck"
            value = reportValue
            log.debug "lowBatteryCheck = ${value}"
            state.lowBatteryCheck = reportValue == 1 ? true : false
            break
        case 111:
            name = "batteryInterval"
            value = reportValue
            log.debug "batteryInterval = $value"
            state.batteryInterval = value.toString()
            break
        case 121:
            name = "sendType"
            value = reportValue
						switch (value) {
							case 16:
							  value = "binary"
							  break
							case 256:
							  value = "basic"
							  break
							case 272:
							  value = "both"
							  break
							default:
							  break
						}
            state.sendType = value
            log.debug "sendType = $value"
            break
        case 252:
            name = "lockConfig"
            value = reportValue
            log.debug "lockConfig = $value"
            state.lockConfig = value
            break
        case 254:
            name = "deviceTag"
            value = reportValue
            log.debug "deviceTag = $value"
            state.deviceTag = value
            break
        default:
            break
    }
  sendEvent(name:"debug", value: "in config report-low batt check state,pref: ${state.lowBatteryCheck}, ${lowBatteryCheck}", displayed: true)
  sendEvent(name:"debug", value: "in config report-low batt interval state,pref: ${state.batteryInterval}, ${batteryInterval}", displayed: true)
	createEvent([name: name, value: value])
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
  if (invertOutput) {value = value ^ 0xFF}
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

def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd) {
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
				result << response(secure(zwave.manufacturerSpecificV2.manufacturerSpecificGet()))
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

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	def event = createEvent(name: "WakeUp", value: "wakeup", descriptionText: "${device.displayName} woke up", isStateChange: true, displayed: false)
	log.debug "Wokeup"
	def cmds = []

	if (!getDataValue("Manufacturer ID")) {
		cmds << secure(zwave.manufacturerSpecificV2.manufacturerSpecificGet())
		cmds << "delay 2000"
	}
	if (!state.lastbat || now() - state.lastbat > 23*60*60*1000) {  //check no sooner than once every 23 hours (once a day)
		log.debug "checking battery"
		event.descriptionText += ", requesting battery"
		cmds << secure(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:1, scale:1))
		cmds << "delay 1000"
		cmds << secure(zwave.batteryV1.batteryGet())
		cmds << "delay 1000"
	} else {
		log.debug "not checking battery, was updated ${(now() - state.lastbat)/60000 as int} min ago"
	}
  if (device.currentValue("needUpdate") == "YES") { cmds += update_settings() }
//  cmds += update_settings()

	cmds << secure(zwave.wakeUpV1.wakeUpNoMoreInformation())
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

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []
	def manufacturerCode = String.format("%04X", cmd.manufacturerId)
	def productTypeCode = String.format("%04X", cmd.productTypeId)
	def productCode = String.format("%04X", cmd.productId)
	def wirelessConfig = "ZWP"
	log.debug "MSR ${manufacturerCode} ${productTypeCode} ${productCode}"

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

def buildInterval() {

  def intervalList = []
   intervalList << [ "240" : "4 minutes" ]
   intervalList << [ "480" : "8 minutes" ]
   intervalList << [ "720" : "12 minutes" ]
   intervalList << [ "960" : "16 minutes" ]
   intervalList << [ "1200" : "20 minutes" ]
   intervalList << [ "1440" : "24 minutes" ]
   intervalList << [ "1800" : "30 minutes" ]
   intervalList << [ "3600" : "1 hour" ]
   intervalList << [ "7200" : "2 hours" ]
   intervalList << [ "10800" : "3 hours" ]
   intervalList << [ "14400" : "4 hours" ]
   intervalList << [ "18000" : "5 hours" ]
   intervalList << [ "21600" : "6 hours" ]
   intervalList << [ "36000" : "10 hours" ]
   intervalList << [ "43200" : "12 hours" ]
   intervalList << [ "86400" : "24 hours" ]
   intervalList << [ "172800" : "2 days" ]
   intervalList << [ "259200" : "3 days" ]
   intervalList << [ "345600" : "4 days" ]
   intervalList << [ "432000" : "5 days" ]
   intervalList << [ "518400" : "6 days" ]
   intervalList << [ "604800" : "1 week" ]
   intervalList << [ "1209600" : "2 weeks" ]
   intervalList << [ "1814400" : "3 weeks" ]
   intervalList << [ "2592000" : "1 month (30 days)" ]
   intervalList << [ "5184000" : "2 months (60 days)" ]
   intervalList << [ "7862400" : "3 months (91 days)" ]
   intervalList << [ "10454400" : "4 months (121 days)" ]
   intervalList << [ "15768000" : "Half year" ]
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
 	def appversion = String.format("%02d.%02d", cmd.applicationVersion, cmd.applicationSubVersion)
 	def zprotoversion = String.format("%d.%02d", cmd.zWaveProtocolVersion, cmd.zWaveProtocolSubVersion)
	updateDataValue("zWave Library", cmd.zWaveLibraryType.toString())
	updateDataValue("Firmware", appversion)
	updateDataValue("zWave Version", zprotoversion)
	sendEvent(name: "Firmware", value: appversion, displayed: true)
}

def secureWUIhandler (description) {
	def found = description.indexOf('command: 9881, payload: 00 84')
	def command = description.substring(found+30,found+32)
	def report = description.substring(found+33).replaceAll("\\s+","")
	def used = true
	if (command == "0A") {
		def minWUI = new BigInteger(report.substring(0,6),16)
		def maxWUI = new BigInteger(report.substring(6,12),16)
		def defaultWUI = new BigInteger(report.substring(12,18),16)
		def stepWUI = new BigInteger(report.substring(18,24),16)
		log.debug "minWUI: ${minWUI}, maxWUI: ${maxWUI}, defaultWUI: ${defaultWUI}, stepWUI: ${stepWUI}"
	}
	else if (command == "06") {
		def curWUI =  new BigInteger(report.substring(0,6),16)
		state.wakeupInterval = curWUI
		sendEvent(name: "wakeupInterval", value: state.wakeupInterval, displayed: true)
		log.debug "currentWUI: ${curWUI}"
	}
    else {used = false}
    return used
}
