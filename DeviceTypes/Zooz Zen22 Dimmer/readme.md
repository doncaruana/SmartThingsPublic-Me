Zooz Zen22 Dimmer

How to use the control:

Single press on the top turns it on, single press on the bottom turns it off. The "bottom" is the side with the LED, 
  but you can invert the switch with parameters. 
Long press will change the dim level when the light is on.
The previous dim level will be resumed when the light is turned back on.
Long press on when the light is off will turn to full brightness, regardless of previous level, after about 2 seconds.
There is no control for the dimming speed.

Notes about the device itself
You may get a device that doesn't list any parameters in the included documentation. As of at least 
version 20.16 of the firmware, there are 2 parameters:

Parameter 1 (1 byte) - Invert Switch - When on, this inverts the switch so that the bottom (side by the LED) actually
                       turns the switch on. The default is for this to be off.
Parameter 2 (1 byte) - LED indicator - When this is turned on, the LED will follow the light and only be on when 
                       the light is on. Normal operation (parameter off) is that the LED is only on when the light is off

If you are looking at live logging when using this switch, you may notice an exception:
          Exception 'java.lang.IndexOutOfBoundsException: toIndex = 4' encountered parsing 'cmd: 7006, payload: 02 02 00'

This is because there is a bug in the firmware that improperly reports the length of the 2nd parameter as 2 
    during the configuration report. The spec says the report should look like this: 7006AABBXX. The map is like this:
    70 (configuration command) 06 (report) AA (parameter number) BB (size) and XX is the number of bytes indicated 
    with BB of the actual parameter value.

The simple workaround is to just look for a configuration report on parameter 2 and add a byte to the returned 
description. The 2nd byte is the least significant byte anyway and also is just not processed in the ConfigurationReport routine.

The handler and device will work perfectly though.

