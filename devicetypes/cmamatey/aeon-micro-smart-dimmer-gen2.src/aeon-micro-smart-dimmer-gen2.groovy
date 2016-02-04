/**
 *	SmartThings device handler implementation for Aeon Labs Aeotec Micro Smart Dimmer Gen2 z-wave device.
 *	Written by Chase Mamatey (cmamat213+dev@gmail.com)
 *	This device handler is a work in progress with no warranty or guarantees of any kind.
 *	This device handler adapts code from various published SmartThings z-wave device handlers.
 *	Information about this device's z-wave configuration came from several documents found online.
 */
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Aeon Labs Micro Smart Dimmer Gen2", namespace: "cmamatey", author: "Chase Mamatey") {
		capability "Energy Meter"
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Configuration"
		capability "Polling"
		capability "Power Meter"
		capability "Refresh"
		capability "Sensor"

		attribute "voltage", "string"
		attribute "current", "string"

		command "reset"
		command "configReset"
	//	command "meterDebug"

	//	fingerprint deviceId: "0x1104", inClusters: "0x26,0x32,0x27,0x2C,0x2B,0x70,0x85,0x72,0x86", outClusters: "0x82"
		fingerprint deviceId: "0x1104", inClusters: "0x26,0x32,0x27,0x2C,0x2B,0x70,0x85,0x72,0x86,0xEF,0x82"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}

	tiles {
		standardTile("switch", "device.switch", canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.Lighting.light7", backgroundColor:"#79b821"//, nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.Lighting.light7", backgroundColor:"#ffffff"//, nextState:"turningOn"
		//	state "turningOn", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#79b821"
		//	state "turningOff", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
		valueTile("energy", "device.energy", inactiveLabel: false) {
			state "default", label:'${currentValue} Wh'//, unit: "kWh"
		//	state "Wh", label:'${currentValue} Wh'//, unit: "Wh"
		}
		valueTile("power", "device.power", inactiveLabel: false) {
			state "default", label:'${currentValue} W'
		}
		valueTile("current", "device.current", inactiveLabel: false) {
			state "default", label:'${currentValue} mA'
		}
		valueTile("voltage", "device.voltage", inactiveLabel: false) {
			state "default", label:'${currentValue} V'
		}
	/*	standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("configure", "device.energy", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}	*/
		standardTile("refresh", "device.level", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
		details(["levelSliderControl","switch","power","energy","refresh","current","voltage"])
	}

/*	preferences {
	//	input "energyUnit", "string", title: "Energy Unit (kWh or Wh)", description: "kWh or Wh", defaultValue: "kWh", required: true, displayDuringSetup: true
		input "energyUnit", "enum", title: "Energy Unit", description: "kWh or Wh", options: ["kWh", "Wh"], defaultValue: "kWh", required: true, displayDuringSetup: true//, style: "segmented"
	}	*/
}

def parse(String description) {
	def item1 = [
		canBeCurrentState: false,
		linkText: getLinkText(device),
		isStateChange: false,
		displayed: false,
		descriptionText: description,
		value:  description
	]
	def result
	def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1, 0x32: 2])
	if (cmd) {
		result = createEvent(cmd, item1)
	}
	else {
		item1.displayed = displayed(description, item1.isStateChange)
		result = [item1]
	}
	log.debug "Parse returned ${result?.descriptionText}"
	result
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	result[0].descriptionText = "${item1.linkText} is ${item1.value}"
	result[0].handlerName = cmd.value ? "statusOn" : "statusOff"
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "digital"
	}
	result
}

def doCreateEvent(physicalgraph.zwave.Command cmd, Map item1) {
	def result = [item1]

	item1.name = "switch"
	item1.value = cmd.value ? "on" : "off"
	item1.handlerName = item1.value
	item1.descriptionText = "${item1.linkText} was turned ${item1.value}"
	item1.canBeCurrentState = true
	item1.isStateChange = isStateChange(device, item1.name, item1.value)
	item1.displayed = item1.isStateChange

	if (cmd.value > 15) {
		def item2 = new LinkedHashMap(item1)
		item2.name = "level"
		item2.value = cmd.value as String
		item2.unit = "%"
		item2.descriptionText = "${item1.linkText} dimmed ${item2.value} %"
		item2.canBeCurrentState = true
		item2.isStateChange = isStateChange(device, item2.name, item2.value)
		item2.displayed = false
		result << item2
	}
	result
}

def createEvent(physicalgraph.zwave.commands.meterv2.MeterReport cmd, Map item1)
{
	if (cmd.scale == 0 && cmd.reserved02 == false) {
	//	def unit = settings.energyUnit
	//	log.debug unit
	//	switch(unit) {
	//		case "Wh":
				createEvent(
					[name: "energy", value: Math.round(cmd.scaledMeterValue * 1000), unit: "Wh"]
				)
	//			break;
	//		case "kWh":
	//			createEvent(
	//				[name: "energy", value: cmd.scaledMeterValue, unit: "kWh", state: "kWh"]
	//			)
	//			break;
	//		default:
	//			log.debug "Invalid energy unit: ${unit}"
	//			break;
	//	}
		
	}
	else if (cmd.scale == 0 && cmd.reserved02 == true) {
    //    log.debug "V: " + cmd
		createEvent(
			[name: "voltage", value: Math.round(cmd.scaledMeterValue * 10)/10, unit: "V"]
		)
	}
	else if (cmd.scale == 1) {
    //    log.debug "A: " + cmd
		createEvent(
		//	[name: "current", value: Math.round(cmd.scaledMeterValue * 100)/100, unit: "A"]
			[name: "current", value: Math.round(cmd.scaledMeterValue * 1000), unit: "mA"]
		)
	}
	else if (cmd.scale == 2) {
    //    log.debug "W: " + cmd
		createEvent(
		//	[name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W"]
			[name: "power", value: cmd.scaledMeterValue, unit: "W"]
		)
	}
}

def createEvent(physicalgraph.zwave.Command cmd,  Map map) {
	// Handles any Z-Wave commands we aren't interested in
	log.debug "UNHANDLED COMMAND $cmd"
}

def on() {
	log.info "on"
	delayBetween([zwave.basicV1.basicSet(value: 0xFF).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 2000)
}

def off() {
	delayBetween ([zwave.basicV1.basicSet(value: 0x00).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 2000)
}

def setLevel(value) {
	delayBetween ([zwave.basicV1.basicSet(value: value).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 2000)
}

def setLevel(value, duration) {
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	zwave.switchMultilevelV2.switchMultilevelSet(value: value, dimmingDuration: dimmingDuration).format()
}

def poll() {
	delayBetween([
    	zwave.switchMultilevelV1.switchMultilevelGet().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 1).format(),
      	zwave.meterV2.meterGet(scale: 2).format()
    ])
}

def refresh() {
	log.debug "Doing Refresh"
	def cmd = delayBetween([
		zwave.switchMultilevelV1.switchMultilevelGet().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 1).format(),
      	zwave.meterV2.meterGet(scale: 2).format()
	])
    log.debug cmd
    cmd
}

def reset() {
	return [
		zwave.meterV2.meterReset().format(),
		zwave.meterV2.meterGet(scale: 0).format(),
		zwave.meterV2.meterGet(scale: 1).format(),
      	zwave.meterV2.meterGet(scale: 2).format()
	]
}

def configure() {
	def cmd = delayBetween([
	//	zwave.configurationV1.configurationSet(parameterNumber: 2, size: 2, scaledConfigurationValue: 3850).format(),
		//	Make Micro Smart Dimmer 2nd Edition blink.
		//	Configuration Value 1 (1st Byte)： 1‐255 (0x01-0xFF), The unit is seconds
		//	Configuration Value 1 is to specify the time that the Micro Smart Dimmer 2nd Edition will blink
		//	Configuration Value 2 (2nd Byte)： 1‐255 (0x01-0xFF) 
		//	Configuration Value 2 is to Specify the Cycle of on/off; the unit is 0.1 second.
		//	For example: if we set Configuration Value 1 to '15' (0x0F) and Configuration Value 2 to '10' (0x0A),then Micro Smart Dimmer 2nd Edition will open 0.5 second, close 0.5 second, and repeat for 14 times.
		//	To use this hex configuration value (0x0F0A) in this SmartThings implementation, we use the equivalent integer (3850 or 15*16^2 + 10*16^0) as the scaledConfigurationValue.
	//	zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 1).format(),
		//	Current Overload Protection. Load will be closed when the Current more than 2.7A and the time more than 2 minutes (0: disable, 1: enable, other: ignore). Default: 0
	//	zwave.configurationV1.configurationSet(parameterNumber: 13, size: 1, scaledConfigurationValue: 0).format(),
		//	Enable/Disable CRC16 encapsulation (1: Enable, 0: Disable, Other: ignore). Default: 0

	//	zwave.configurationV1.configurationSet(parameterNumber: 80, size: 1, scaledConfigurationValue: 0).format(),
		//	Enable to send notifications to associated devices (Group 1) when the state of Micro Smart Dimmer’s load changes (0:nothing, 1:hail CC, 2:basic CC report, other: ignore). Default: 0
	//	zwave.configurationV1.configurationSet(parameterNumber: 90, size: 1, scaledConfigurationValue: 1).format(),
		//	Enables/disables parameter 91 and 92 (1: enable, 0: disable, other: ignore). Default: 1
	//	zwave.configurationV1.configurationSet(parameterNumber: 91, size: 2, scaledConfigurationValue: 25).format(),
		//	The value here represents minimum change in wattage (in Watts) for a REPORT to be sent. Default: 25
	//	zwave.configurationV1.configurationSet(parameterNumber: 92, size: 1, scaledConfigurationValue: 5).format(),
		//	The value here represents minimum change in wattage percent (in %) for a REPORT to be sent. Default: 5

	//	zwave.configurationV1.configurationSet(parameterNumber: 100, size: 1, scaledConfigurationValue: 0).format(),
		//	Sets parameters 101‐103 to default.
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 6).format(),
		//	Specifies which reports need to send in Report group 1. Default: 4
		zwave.configurationV1.configurationSet(parameterNumber: 102, size: 4, scaledConfigurationValue: 9).format(),
		//	Specifies which reports need to send in Report group 2. Default: 8
		zwave.configurationV1.configurationSet(parameterNumber: 103, size: 4, scaledConfigurationValue: 0).format(),
		//	Specifies which reports need to send in Report group 3. Default: 0
		//	For parameters 101-103: The 4 least significant bits of the 4 byte value enable energy, power, current and voltage reporting. Those 4 bits (from Most to Least significant) control reporting of kWh, Watts, Amps and Volts, respectively.
		// 		Energy (kWh): 1000 (integer value 8);		Power (W): 0100 (4); 	Energy and Power (kWh, W): 1100 (12)
		//		Current (A): 0010 (2);		Voltage (V): 0001 (1);		All: 1111 (15);				

	//	zwave.configurationV1.configurationSet(parameterNumber: 110, size: 1, scaledConfigurationValue: 0).format(),
		//	Sets parameters 111‐113 to default.
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 20).format(),
		//	Specifies the time interval of sending Report group 1 (unit: second). Default: 3
		zwave.configurationV1.configurationSet(parameterNumber: 112, size: 4, scaledConfigurationValue: 300).format(),
		//	Specifies the time interval of sending Report group 2 (unit: second). Default: 600
		zwave.configurationV1.configurationSet(parameterNumber: 113, size: 4, scaledConfigurationValue: 600).format(),
		//	Specifies the time interval of sending Report group 3 (unit: second). Default: 600

		zwave.configurationV1.configurationSet(parameterNumber: 120, size: 1, scaledConfigurationValue: 0).format()//,
		//	Sets external button mode (0: Momentary button mode, 1: 2-state switch mode, 2: 3-way switch mode, 255: Unidentified mode, Other: ignore). Default: 255
	//	zwave.configurationV1.configurationSet(parameterNumber: 200, size: 1, scaledConfigurationValue: 0).format(),
		//	Partner ID (0= Aeon Labs Standard Product).
	//	zwave.configurationV1.configurationSet(parameterNumber: 252, size: 1, scaledConfigurationValue: 0).format(),
		//	Enable/disable Lock Configuration (0: disable, 1: enable, other: ignore). Default: 0
	//	zwave.configurationV1.configurationSet(parameterNumber: 254, size: 2, scaledConfigurationValue: 0).format(),
		//	Device tag. Default: 0
	//	zwave.configurationV1.configurationSet(parameterNumber: 255, size: 1, scaledConfigurationValue: 0).format(),
		//	Reset configuration to factory default settings (except parameter 254).
    ])
    log.debug cmd
    cmd
}

def configReset() {
	def cmd = delayBetween([
		zwave.configurationV1.configurationSet(parameterNumber: 255, size: 1, scaledConfigurationValue: 0).format()
		//	Reset configuration to factory default settings (except parameter 254).
    ])
    log.debug cmd
    cmd
}
/*
def setEnergyUnit(unit) {
	def energyUnit = device.latestState('energy')
	log.debug energyUnit
	
	switch (unit) {
		case "kWh":

			break;
		case "Wh":
			break;
		default:
			break;
	}
}*/