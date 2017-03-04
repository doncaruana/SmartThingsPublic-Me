Some notes about the Strips device and this handler:

The preference defaults are set to the device defaults. If you are looking for accurate values in the IDE, you
have to hit "Done" in preferences once, even if you don't change any settings.

The wakeup interval from the manufacturer is set to 24 hours. Smartthings sets everything to have a wakeupinterval
of 4 hours. This will have some (although reportedly small) affect on battery life. While you can set the interval via
the handler, the default setting via this handler is back to the manufacturer default of 24 hours.

Preferences with an asterisk have to wait for the next wakeup, automatic or manual. The pending icon will display 
if there are changes pending and say "Synced" otherwise. If you revert your preferences to values that match the
device before the next wakeup, it will go back to "Synced". Only changed preferences are pushed to the device.

The actual wakeup by the device is approximately 90% of the wakeupinterval setting. So the observed wakeup interval
will be a little less than the setting. Also, the wakeupinterval has a minimum of 1800 and a maximum of 86400 and 
must be set at 60 second increments. 

A tamper notification is sent in 2 cases:
- Putting a magnet on the round edge when there is a magnet on the square one triggers tamper 
- During a Manual Wake Up, a tamper is sent before the Node Info. 

The tamper event time can be cleared by just tapping on the time itself. Tamper events are reported so they can be 
seen historically in the GUI.

