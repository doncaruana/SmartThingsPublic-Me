Wakeup Interval Information
  - Minimum Interval : 240 seconds
  - Maximum Interval : 16777215 seconds (194 days)
  - Default Interval : 0
  - Minimum Step : 240 seconds
  
Parm Size Description                                   Value
   1    1 Which value to send for Sensor Binary Report  0 (Default)-Sensor triggered on=0xFF, 1-Sensor triggered on=0x00
   3    1 Which value is sent for Sensor Basic Report   0 (Default)-Sensor triggered on=0xFF, 1-Sensor triggered on=0x00
  39    1 Low Battery threshold                         10 (Default), 10-50, Threshold below which a low battery is reported, 
                                                            must enable 101 and 111 to work
 101    1 Enable the function of low battery checking   0 (Default)-Disable low battery checking, 1-Enable low battery checking
 111    4 Set the interval time of the battery report   86640 (Default)-disabled, 1 to 2147483647 in seconds
 121    4 Which command to send                         256 (Default)-Basic, 16-Binary, 272-Both
 252    1 Enable Configuration parameters to be locked  0 (Default)-Disable all configuration parameters to be locked, 
                                                        1-Enable all configuration parameters to be locked
 254    2 Device Tag                                    0 to 65535 The range of the device tag is 65535

 Enabling battery checking or setting the battery interval (which is rounded to 4 minute intervals anyway) seems to have no 
 effect whatsoever. The wakeup interval, although it's supposed to be a different setting, follows this same, 4 minute rule. 
 It's pointless to set either.
 
 This device handler will just rely on the smartthings default wakeup interval of 4 hours and check the battery once a day 
 (no sooner than every 23 hours)
