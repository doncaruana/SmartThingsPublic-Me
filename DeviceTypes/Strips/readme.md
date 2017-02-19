Some notes about the Strips device and this handler:

The preference defaults are set to the device defaults. If you are looking for accurate values in the IDE, you
have to hit "Done" in preferences once, even if you don't change any settings.

Preferences with an asterisk have to wait for the next wakeup, automatic or manual. The pending icon will display 
if there are changes pending and say "Synced" otherwise. If you revert your preferences to values that match the
device before the next wakeup, it will go back to "Synced". Only changed preferences are pushed to the device.

The actual wakeup by the device is approximately 90% of the wakeupinterval setting. I've adjusted the numbers so that
the desired interval is set when you use preferences.

In the Strips internal software, there is a "failed"counter - when a number of events fail between the device and 
the z-wave chip, the microcontroller assumes it is because of the battery and reports "Battery LOW".
The battery reports are:
NONE","DEAD","FULL","HIGH","LOW", "MEDIUM"
which correspond to: none, 0%, 100%, 75%, 25%, 50%
When successful events happen, the "failed" counter is reset and the original battery level is displayed.
The actual number to trigger or clear this report is unknown.

A tamper notification is sent in 2 cases:
- Putting a magnet on the round edge when there is a magnet on the square one triggers tamper 
- During a Manual Wake Up, a tamper is sent before the Node Info. 

The tamper event time can be cleared by just tapping on the time itself. Tamper events are reported so they can be 
seen historyically in the GUI.

If you need to do a manual wakeup to sync preferences, you need to toggle the setting "Alow Manual Wakeup" in
preferences. Otherwise, it will just send a tamper.
