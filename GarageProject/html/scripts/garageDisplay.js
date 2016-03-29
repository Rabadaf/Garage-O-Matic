 webiopi().ready(function() {
    // Create a "Light" labeled button for GPIO 17
    var relayButton = webiopi().createGPIOButton(17, "Relay");
    var doorButton = webiopi().createGPIOButton(8, "Door");
    var pulseButton = webiopi().createPulseButton("pulse", "Pulse", 17);
    var seqButton = webiopi().createSequenceButton("sequence", "Sequence", 17, 100, "01");

    // Append button to HTML element with ID="controls" using jQuery
    $("#controls").append(relayButton);
    $("#controls").append(doorButton);
    //$("#controls").append(pulseButton);
    $("#controls").append(seqButton);    

    // Refresh GPIO buttons
    // pass true to refresh repeatedly of false to refresh once
    webiopi().refreshGPIO(true);
});

setInterval("callMacro_getDoorStatus()", 500);{}

function callMacro_getDoorStatus()
{
    webiopi().callMacro("getDoorStatus", [], doorStatusCallback);
}

function doorStatusCallback(macroName, args, data)
{
    if (data == false)
    {
        //$("#door").text("Open");
        webiopi().setLabel("gpio8", "Open");
    }
    else
    {
        //$("#door").text("Closed");
        webiopi().setLabel("gpio8", "Closed");
    }
}