/*
*  Hubitat Driver for OpenWeatherMap
*  Licenses CC-0 public domain
*/


metadata {
    definition(name: "OpenWeatherMapCustom",
               namespace: "openweathermap",
               author: "romangrebin",
               importUrl: "https://raw.githubusercontent.com/romangrebin/Hubitat-Drivers-Personal/master/openweathermap_driver.groovy") 
    {
        // Capabilities (see https://docs.hubitat.com/index.php?title=Driver_Capability_List)
        capability "Sensor"
        capability "Refresh"
        capability "Polling"

		capability 'Temperature Measurement'
		capability 'Relative Humidity Measurement'
		capability 'Pressure Measurement'
		capability 'Ultraviolet Index'
		capability 'Illuminance Measurement'

        // attributes

        // attributes required for capabilities
        attribute 'temperature', 'number' // capability TemperatureMeasurement
        attribute 'humidity', 'number' // capability RelativeHumidityMeasurement
        attribute 'pressure', 'number' // capability PressureMeasurement
        attribute 'ultravioletIndex', 'number' // capability UltravioletIndex
        attribute 'illuminance', 'number' // capability IlluminanceMeasurement

        // attributes used by tile templates
		attribute 'city', 'string'
		attribute 'weatherIcons', 'string'
		attribute 'windDirection', 'number'
		attribute 'windSpeed', 'number'

        // other common attributes
        attribute 'feelsLike', 'number'

        // custom attributes - used and exposed by this driver
        attribute 'tempHighDay0', 'number'
        attribute 'tempHighDay1', 'number'
        attribute 'tempHighDay2', 'number'

        attribute 'tempLowDay0', 'number'
        attribute 'tempLowDay1', 'number'
        attribute 'tempLowDay2', 'number'

        attribute 'precipPercentDay0', 'number'
        attribute 'precipPercentDay1', 'number'
        attribute 'precipPercentDay2', 'number'

        attribute 'precipDay0', 'number'
		attribute 'precipDay1', 'number'
		attribute 'precipDay2', 'number'

        attribute 'cloudDay0', 'number'
		attribute 'cloudDay1', 'number'
		attribute 'cloudDay2', 'number'
    }

    preferences {
        input 'apiKey', 'text', required: true, title: 'Type OpenWeatherMap.org API Key Here', defaultValue: null
        input 'pollInterval', 'number', title: 'Minutes Between External Polls', required: true, defaultValue: 10
        input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false
        input 'latitude', 'text', title: "Latitude", required: true, defaultValue: location.latitude.toString()
        input 'longitude', 'text', title: "Longitude", required: true, defaultValue: location.longitude.toString()
    }
}

void installed() {
    if (logDebug) log.debug "installed()..."
    refresh()
    runIn(2, poll)
}

// Commands necessary for capabilities

def refresh() {
    // Required for Refresh capability
    LOGDEBUG("<<<<<<<<<<< REFRESHING >>>>>>>>>>>>")

    // Set numbers to -1, strings to 'empty'
    for (att in device.getSupportedAttributes()) {
        if (att.dataType.toString() == 'NUMBER') {
            sendEvent(name: att.name, value: -1)
        } else if (att.dataType.toString() == 'STRING') {
            sendEvent(name: att.name, value: 'empty')
        }
    }

    runIn(2, poll)
}

def poll() {
    // Required for Polling capability
    if( apiKey == null ) {
		LOGDEBUG('OpenWeatherMap API Key not found.  Please configure in preferences.')
		return
	}
    Map ParamsOWM
	ParamsOWM = [ uri: 'https://api.openweathermap.org/data/2.5/onecall?lat=' + (String)latitude + '&lon=' + (String)longitude + '&exclude=minutely,hourly&mode=json&units=imperial&appid=' + (String)apiKey, timeout: 20 ]
	LOGDEBUG('Polling ' + ParamsOWM)

    asynchttpGet('handleOWMResponse', ParamsOWM)

    runIn(pollInterval * 60, poll)
}

void updateAttribute(name, value) {
    LOGDEBUG("Updating " + name + "= " + value)
    sendEvent(name: name, value: value)
}

String roundValue(value, num_decimals = 0) {
    BigDecimal result
    Integer multiplier = Math.pow(10, num_decimals)
    result = Math.round(value.toBigDecimal() * multiplier) / multiplier
    return result.toString()
}

void handleOWMResponse(response, data) {
	if(response.getStatus() != 200 && response.getStatus() != 207) {
		LOGDEBUG(response.getStatus() + '; ' + response.getErrorMessage())
        return
	}
    
    Map owm_data = parseJson(response.data)

    if(owm_data.toString()==(String)null) {
        LOGDEBUG('Empty response.')
        return
    }

    LOGDEBUG('OpenWeatherMap Data: ' + owm_data.toString())

    // <<< Current weather info >>>

    BigDecimal temp_dewpoint = owm_data?.current?.dew_point

    updateAttribute('dewpoint', roundValue(temp_dewpoint, 0))

    BigDecimal humidity = owm_data.current.humidity
    updateAttribute('humidity', humidity)

    BigDecimal pressure = owm_data.current.pressure
    updateAttribute('pressure', roundValue(pressure, 1))

    BigDecimal temperature = owm_data.current.temp
    updateAttribute('temperature', roundValue(temperature, 0))

    BigDecimal wind_speed = owm_data.current.wind_speed
    updateAttribute('windSpeed', roundValue(wind_speed, 1))

    BigDecimal wind_direction_degrees = owm_data.current.wind_deg
    updateAttribute('windDirection', wind_direction_degrees.toInteger())

    BigDecimal uv_index = owm_data.current.uvi
    updateAttribute('ultravioletIndex', uv_index)

    BigDecimal feels_like = owm_data.current.feels_like
    updateAttribute('feelsLike', feels_like)

    
    // BigDecimal visibility = owm.current.visibility.toBigDecimal()
    // updateAttribute('vis', roundValue(visibility, 2))

    // List owmCweat = owm?.current?.weather
    // myUpdData('condition_id', owmCweat==null || owmCweat[0]?.id==null ? '999' : owmCweat[0].id.toString())
    // myUpdData('condition_code', getCondCode(myGetData('condition_id').toInteger(), myGetData('is_day')))
    // myUpdData('condition_text', owmCweat==null || owmCweat[0]?.description==null ? 'Unknown' : owmCweat[0].description.capitalize())
    // myUpdData('OWN_icon', owmCweat == null || owmCweat[0]?.icon==null ? (myGetData('is_day')==sTRU ? '50d' : '50n') : owmCweat[0].icon)
    
    // >>> End Current Weather <<<

    // >>> Begin Daily weather <<<
//     List owmDaily = owm?.daily != null && ((List)owm.daily)[0]?.weather != null ? ((List)owm?.daily)[0].weather : null
//     myUpdData('forecast_id', owmDaily==null || owmDaily[0]?.id==null ? '999' : owmDaily[0].id.toString())
//     myUpdData('forecast_code', getCondCode(myGetData('forecast_id').toInteger(), sTRU))
//     myUpdData('forecast_text', owmDaily==null || owmDaily[0]?.description==null ? 'Unknown' : owmDaily[0].description.capitalize())

    List owmDaily = owm_data?.daily != null ? (List)owm_data.daily : null
    if (owmDaily == null) {
        LOGDEBUG('Daily object in response is empty')
        return
    }

    Map dayweather
    for (int day_num in 0..2) {
        dayweather = owmDaily[day_num]

        // precipitation
        BigDecimal rain = mm_to_inches(!owmDaily[day_num]?.rain ? 0.00 : owmDaily[day_num].rain.toBigDecimal())
        BigDecimal snow = mm_to_inches(!owmDaily[day_num]?.snow ? 0.00 : owmDaily[day_num].snow.toBigDecimal())
        BigDecimal precip = rain + snow

        String daynumstr = day_num.toString()
        updateAttribute('precipDay'+daynumstr, precip)
        updateAttribute('precipPercentDay'+daynumstr, dayweather.pop.toBigDecimal() * 100.toInteger())

        // cloud
        BigDecimal cloud = dayweather.clouds==null ? 1 : owmDaily[0].clouds <= 1 ? 1 : owmDaily[0].clouds
        updateAttribute('cloudDay'+daynumstr, cloud)

        BigDecimal hightemp = dayweather.temp.max
        BigDecimal lowtemp = dayweather.temp.min
        updateAttribute('tempHighDay'+daynumstr, hightemp)
        updateAttribute('tempLowDay'+daynumstr, lowtemp)
    }
//         //  <<<<<<<<<< Begin Built alertTile >>>>>>>>>>
//         String alertTile = (myGetData('alert')== sNCWA ? 'No Weather Alerts for ' : 'Weather Alert for ') + myGetData('city') + (myGetData('alertSender')==null || myGetData('alertSender')==sSPC ? '' : ' issued by ' + myGetData('alertSender')) + sBR
//         alertTile+= myGetData('alertTileLink') + sBR
//         alertTile+= '<a href="https://openweathermap.org/city/' + myGetData('OWML') + '" target="_blank">' + sIMGS5 + myGetData(sICON) + 'OWM.png style="height:2em"></a> @ ' + myGetData(sSUMLST)
//         myUpdData('alertTile', alertTile)
//         sendEvent(name: 'alert', value: myGetData('alert'))
//         sendEvent(name: 'alertDescr', value: myGetData('alertDescr'))
//         sendEvent(name: 'alertSender', value: myGetData('alertSender'))
//         sendEvent(name: 'alertTile', value: myGetData('alertTile'))
//         //  >>>>>>>>>> End Built alertTile <<<<<<<<<<
//     }
// // >>>>>>>>>> End Setup Forecast Variables <<<<<<<<<<

//     // <<<<<<<<<< Begin Icon Processing  >>>>>>>>>>
//     String imgName = (myGetData('iconType')== sTRU ? getImgName(myGetData('condition_id').toInteger(), myGetData('is_day')) : getImgName(myGetData('forecast_id').toInteger(), myGetData('is_day')))
//     sendEventPublish(name: 'condition_icon', value: sIMGS5 + myGetData(sICON) + imgName + imgT1 + sRB)
//     sendEventPublish(name: 'condition_iconWithText', value: sIMGS5 + myGetData(sICON) + imgName + imgT1 + sRB+ sBR + (myGetData('iconType')== sTRU ? myGetData('condition_text') : myGetData('forecast_text')))
//     sendEventPublish(name: 'condition_icon_url', value: myGetData(sICON) + imgName + imgT1)
//     myUpdData('condition_icon_url', myGetData(sICON) + imgName + imgT1)
//     sendEventPublish(name: 'condition_icon_only', value: imgName.split('/')[-1].replaceFirst('\\?raw=true',sBLK))
// // >>>>>>>>>> End Icon Processing <<<<<<<<<<
//     PostPoll()
}
def mm_to_inches(mm_val) {
    // Convert millimeters to inches
    return (mm_val * 0.0393701)
}

void LOGDEBUG(text) {
    if (logDebug) {
        log.debug('OpenWeatherCustomDriver - Debug ' + text)
    }
}

// def ReceiveData(response, data) {
//     try{
//         if (response.getStatus() == 200 || response.getStatus() == 207) {
//             if (logDebug) log.debug "start parsing"      

//             Json = parseJson( response.data )

//             fireUpdate("voc",Json.voc,"ppb","voc is ${Json.voc} ppb")
//             fireUpdate("pm25",Json.pm25,"ug/m3","pm25 is ${Json.pm25} ug/m3")
//             fireUpdate("awair_score",Json.score,"","awair_score is ${Json.score}")

//             temperature=convertTemperatureIfNeeded(Json.temp,"c",1)
//             fireUpdate("temperature",temperature,"°${location.temperatureScale}","Temperature is ${temperature}°${location.temperatureScale}")
//             fireUpdate("carbonDioxide",Json.co2,"ppm","carbonDioxide is ${Json.co2} ppm")
//             fireUpdate("humidity",(int)Json.humid,"%","humidity is ${Json.humid}")

// 			temp_quality = getTempQual(Json.temp)
//             fireUpdate_small("temp_quality", temp_quality)
			
//             humidity_quality = getHumidityQual(Json.humid)
//             fireUpdate_small("humidity_quality", humidity_quality)
            
//             co2_quality = getCO2Qual(Json.co2)
//             fireUpdate_small("co2_quality", co2_quality)
            
//             voc_quality = getVOCQual(Json.voc)
//             fireUpdate_small("voc_quality", voc_quality)
            
//             pm25_quality = getPM25Qual(Json.pm25)
//             fireUpdate_small("pm25_quality", pm25_quality)


//         } else {
//             log.error "parsing error"
//         }
//     } catch(Exception e) {
//         log.error "error #5415 : ${e}"
//     }
// }

// void fireUpdate(name,value,unit,description)
// {
//     result = [
//         name: name,
//         value: value,
//         unit: unit,
//         descriptionText: description
//         //	translatable:true
//     ]
//     eventProcess(result)   
// }

// def getTempQual(temp)
// {
//     if (temp >= 33 || temp <= 9) {
//         return 5
//     } else if (temp > 31 || temp < 11) {
//         return 4
//     } else if (temp > 26 || temp < 17) {
//         return 3
//     } else if (temp > 25 || temp < 18) {
//         return 2
//     } else {
//         return 1
//     }
// }

// def getAttribute(name)
// {
//     return device.currentValue(name).toString()
// }

// void eventProcess(Map evt) {
//     if (getAttribute(evt.name).toString() != evt.value.toString() ) 
//     {
//         evt.isStateChange=true
//         sendEvent(evt)

//         if (logEnable) log.info device.getName()+" "+evt.descriptionText
//         if (logDebug) log.debug "result : "+evt
//     }
// }
