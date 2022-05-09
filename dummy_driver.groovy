/*
*  Hubitat Driver for testing
*  Licenses CC-0 public domain
*/

metadata {
    definition(name: "Dummy",
               namespace: "dummy",
               author: "romangrebin",
               importUrl: "https://raw.githubusercontent.com/romangrebin/Hubitat-Drivers-Personal/master/dummy_driver.groovy") 
    {

        capability "Refresh"
        capability "Polling"

        attribute "valuenumone", 'number'
        attribute "valuenumtwo", 'number'

        attribute "valuestrone", "string"
        attribute "valuestrtwo", "string"
    }

    preferences {
        input name: "yes", type: "bool", title: "do yes", defaultValue: false        
    }
}

void installed() {
    log.debug "installed()..."

    refresh()

    runIn(2, poll)
}

def refresh() {
    log.debug "<<<<<<<<<<< REFRESHING >>>>>>>>>>>>"
    for (att in device.getSupportedAttributes()) {
        if (att.dataType.toString() == 'NUMBER') {
            sendEvent(name: att.name, value: -1)
        } else {
            sendEvent(name: att.name, value: 'abcd')
        }
    }
    // fireUpdate("voc",-1,"ppb","voc is ${-1} ppb")
    // fireUpdate("pm25",-1,"ug/m3","pm25 is ${-1} ug/m3")
    // fireUpdate("awair_score",-1,"","awair_score is ${-1}")
    // fireUpdate("temperature",-1,"°${location.temperatureScale}","Temperature is ${-1}°${location.temperatureScale}")
    // fireUpdate("carbonDioxide",-1,"ppm","carbonDioxide is ${-1} ppm")
    // fireUpdate("humidity",-1,"%","humidity is ${-1}")

    runIn(5, poll)
}

def poll() {
    log.debug 'polling'
    Random rng = new Random()

    for (att in device.getSupportedAttributes()) {
        if (att.dataType == 'NUMBER') {
            sendEvent(name: att.name, value: rng.nextInt() % 60)
        } else {
            sendEvent(name: att.name, value: 'na')
        }
    }

    // sendEvent(name: "valuestrone", value: "new", unit: 'idk')

    runIn(pollingInterval, poll)
}

def getAttribute(name)
{
    return device.currentValue(name).toString()
}