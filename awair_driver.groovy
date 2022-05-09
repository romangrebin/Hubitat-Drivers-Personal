/*
*  Hubitat Driver for AwAir Elements
*  Licenses CC-0 public domain
*/

metadata {
    definition(name: "AwAirPublic",
               namespace: "awair",
               author: "romangrebin",
               importUrl: "https://raw.githubusercontent.com/romangrebin/Hubitat-Drivers-Personal/master/awair_driver.groovy") 
    {
        capability "Sensor"
        capability "Refresh"
        capability "Polling"
        capability "TemperatureMeasurement"
        capability "CarbonDioxideMeasurement"
        capability "RelativeHumidityMeasurement"

        attribute "temperature", "number"
        attribute "humidity", "string"
        attribute "carbonDioxide", "number"
        attribute "voc", "number"
        attribute "pm25", "number"

        attribute "awair_score", "number"
        
        attribute "temp_quality", "number"
        attribute "humidity_quality", "number"
        attribute "co2_quality", "number"
        attribute "voc_quality", "number"
        attribute "pm25_quality", "number"
    }

    preferences {
        input("ip", "text", title: "IP Address", description: "ip of AwAir", required: true, defaultValue: "http://192.168.1.3" )
        input("urlPath", "text", title: "Path Address", description: "URL path of AwAir", required: true, defaultValue: "/air-data/latest" )

        input name: "pollingInterval", type: "number", title: "Time (seconds) between status checks", defaultValue: 300

        input name: "logEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: false        
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false  
    }
}

void installed() {
    if (logDebug) log.debug "installed()..."
    refresh()   
    runIn(2, poll)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def refresh() {
    if (logDebug) log.debug "refreshing"
    fireUpdate("voc",-1,"ppb","voc is ${-1} ppb")
    fireUpdate("pm25",-1,"ug/m3","pm25 is ${-1} ug/m3")
    fireUpdate("awair_score",-1,"","awair_score is ${-1}")
    fireUpdate("temperature",-1,"°${location.temperatureScale}","Temperature is ${-1}°${location.temperatureScale}")
    fireUpdate("carbonDioxide",-1,"ppm","carbonDioxide is ${-1} ppm")
    fireUpdate("humidity",-1,"%","humidity is ${-1}")

    runIn(2, poll)
}

def poll() {
    try {
        def Params = [ 
            uri: ip,
            path: urlPath,
            contentType: "application/json" ]
        asynchttpGet( 'ReceiveData', Params)
        if (logDebug) log.debug "poll state"
    } catch(Exception e) {
        if (logDebug) 
        log.error "error occured calling httpget ${e}"
        else
            log.error "error occured calling httpget"
    }

    runIn(pollingInterval, poll)
}

def ReceiveData(response, data) {
    try{
        if (response.getStatus() == 200 || response.getStatus() == 207) {
            if (logDebug) log.debug "start parsing"      

            Json = parseJson( response.data )

            fireUpdate("voc",Json.voc,"ppb","voc is ${Json.voc} ppb")
            fireUpdate("pm25",Json.pm25,"ug/m3","pm25 is ${Json.pm25} ug/m3")
            fireUpdate("awair_score",Json.score,"","awair_score is ${Json.score}")

            temperature=convertTemperatureIfNeeded(Json.temp,"c",1)
            fireUpdate("temperature",temperature,"°${location.temperatureScale}","Temperature is ${temperature}°${location.temperatureScale}")
            fireUpdate("carbonDioxide",Json.co2,"ppm","carbonDioxide is ${Json.co2} ppm")
            fireUpdate("humidity",(int)Json.humid,"%","humidity is ${Json.humid}")

			temp_quality = getTempQual(Json.temp)
            fireUpdate_small("temp_quality", temp_quality)
			
            humidity_quality = getHumidityQual(Json.humid)
            fireUpdate_small("humidity_quality", humidity_quality)
            
            co2_quality = getCO2Qual(Json.co2)
            fireUpdate_small("co2_quality", co2_quality)
            
            voc_quality = getVOCQual(Json.voc)
            fireUpdate_small("voc_quality", voc_quality)
            
            pm25_quality = getPM25Qual(Json.pm25)
            fireUpdate_small("pm25_quality", pm25_quality)


        } else {
            log.error "parsing error"
        }
    } catch(Exception e) {
        log.error "error #5415 : ${e}"
    }
}

void fireUpdate(name,value,unit,description)
{
    result = [
        name: name,
        value: value,
        unit: unit,
        descriptionText: description
        //	translatable:true
    ]
    eventProcess(result)   
}

void fireUpdate_small(name,value)
{
    result = [
        name: name,
        value: value
    ]
    eventProcess(result)   
}

def getTempQual(temp)
{
    if (temp >= 33 || temp <= 9) {
        return 5
    } else if (temp > 31 || temp < 11) {
        return 4
    } else if (temp > 26 || temp < 17) {
        return 3
    } else if (temp > 25 || temp < 18) {
        return 2
    } else {
        return 1
    }
}

def getHumidityQual(humidity)
{
    if (humidity > 80 || humidity < 15) {
        return 5
    } else if (humidity > 65 || humidity < 20) {
        return 4
    } else if (humidity > 60 || humidity < 35) {
        return 3
    } else if (humidity > 50 || humidity < 40) {
        return 2
    } else {
        return 1
    }
}

def getCO2Qual(co2)
{
    if (co2 > 2500) {
        return 5
    } else if (co2 > 1500) {
        return 4
    } else if (co2 > 1000) {
        return 3
    } else if (co2 > 600) {
        return 2
    } else {
        return 1
    }
}

def getVOCQual(voc)
{
    if (voc > 8332) {
        return 5
    } else if (voc > 3333) {
        return 4
    } else if (voc > 1000) {
        return 3
    } else if (voc > 333) {
        return 2
    } else {
        return 1
    }
}

def getPM25Qual(pm25)
{
    if (pm25 > 75) {
        return 5
    } else if (pm25 > 55) {
        return 4
    } else if (pm25 > 35) {
        return 3
    } else if (pm25 > 15) {
        return 2
    } else {
        return 1
    }
}

def getAttribute(name)
{
    return device.currentValue(name).toString()
}

void eventProcess(Map evt) {
    if (getAttribute(evt.name).toString() != evt.value.toString() ) 
    {
        evt.isStateChange=true
        sendEvent(evt)

        if (logEnable) log.info device.getName()+" "+evt.descriptionText
        if (logDebug) log.debug "result : "+evt
    }
}
