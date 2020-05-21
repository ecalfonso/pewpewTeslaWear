# pewpewTeslaWear ![Icon](/wear/src/main/res/mipmap-mdpi/pptw_launcher_icon.webp)
A personal project for controlling your Tesla vehicle(s) through your Android phone and/or Wear OS smartwatch using the unofficial Tesla API.

This is by no means perfect or optimized, most code was taken from various StackOverflow threads to get to a working state. 

## Disclaimer
<b>The Mobile and Wear OS apps/code are provided "as is" without warranty of any kind, either express or implied. Use at your own risk.</b>

## Features
### Login and Car selection
Login with your Tesla credentials or with an Access token. Optionally include your Refresh token to stay logged in past the Access token expiration.
#### Phone login:
![Phone login](/github_images/mobile_login.gif)

#### Wear OS login:
![Wear OS login](/github_images/wear_login.gif)

<b>Tesla username/password is not saved.</b> Only the Access and Refresh tokens are stored.

### View vehicle status in app
View Vehicle name, Battery charge/range remaining, Indoor/Outdoor temperature, Charging status and time of last data update.
The app also dynamically displays car alerts in the app when the data refreshes.

![App car alerts](/github_images/full_car_alerts.png)

Icons show when these conditions are met:

	Vehicle is unlocked
	Door(s) is/are open
	Frunk is open
	Trunk is open
	Windows(s) is/are open
	Software Update is available
	Sentry Mode is on
	Climate controls are active

<b>Notifications are not supported at this time</b>

### Vehicle controls
#### Control options:
	Lock/Unlock 
	Turn on/off climate
	Open frunk/trunk*
	Vent/Close windows
	Turn on/off sentry mode
	Start/Stop charging
	Open/Close charge port
	Set charge limit (50-100%)
	Activate Homelink*

<b>*note:</b> App will display a confirmation message before executing these commands.

#### Mobile demo
![Mobile cmd and alerts demo](/github_images/mobile_cmd_and_car_alerts.gif) ![Mobile cmd confirmation](/github_images/mobile_cmd_confirmation.gif)

#### Wear OS demo
Wear OS controls give you a 2.5 second confirmation delay before executing the command.
![Wear OS cmd confirmation timeout](/github_images/wear_cmd_timeout.gif)

![Wear OS cmd confirmation dialog](/github_images/wear_cmd_confirmation.gif)

![Wear OS cmd set charge](/github_images/wear_cmd_charge_limit.gif)

## TODO Features
### Mobile:
	Add Widgets
	Add Shortcuts
	Implement permanent notification

### Wear:
	Implement Complications
	Implement Tile
	Implement sync access/refresh token from phone
	Add direct commands from app list, like Google fit
	Implement Settings:
		Custom confirmation time delay 
		
### Common
    Move from static views to List/Adapters
	Set climate for driver/passenger
	Show current climate setting 
	Include STANDARD/MAX in set charge limit dialog
	Reset car data when selecting a new car

	Gate commands by vehicle config:
		Trunk Open/Close, if vehicle_config.plg==true
		Start/Stop charge, if charge_state.charging_state!=Disconnected
		Open/Close charge port, if vehicle_config.motorized_charge_port==true
		Open/Close sunroof, if vehicle_config.sun_roof_installed!=null

## Known bugs
    TBD

## Changelist
### 1.0.2:
	Improve CarSelect activities for mobile and wear
	Move CarAlerts from static images/views to a list/adapter
	Wear: Reorganize home screen
	Mobile: Change Tokenscreen to Car Info screen
	Create common code directory for mobile and wear	

### 1.0.1:
	common: new app icon
	common: Move start of data update to onResume()
	common: Clear previous vehicle data when new vehicle is selected
	mobile: Fix AutoFill
	wear: Fix RotaryInput
	wear: Reset back to top of home screen on re-open
	wear: Properly close LoginSelectorActivity when login is good

### 1.0.0:
	Initial implementation
