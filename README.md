# WifiWizard2

WifiWizard2 enables Wifi management for both Android and iOS applications within Cordova/Phonegap projects.

This project is a fork of the [WifiWizard](https://github.com/hoerresb/WifiWizard) plugin with fixes and updates, as well as patches taken from the [Cordova Network Manager](https://github.com/arsenal942/Cordova-Network-Manager) plugin.  Majority of credit for code base goes to those projects.

iOS has limited functionality as Apple's WifiManager equivalent is only available  as a private API. Any app that used these features would not be allowed on the app store.

# Global Functions
These are functions that can be used by both Android and iOS applications
```javascript
WifiWizard2.getConnectedSSID(success, fail)
```
 - Returns connected network SSID (only if connected) in success callback, otherwise fail callback will be called (if not connected or unable to retrieve)
 - This does **NOT** return the BSSID if unable to obtain SSID (like original WifiWizard did)
```javascript
WifiWizard2.getConnectedBSSID(success, fail)
```
 - Same as above, except BSSID (mac) is returned
```javascript
WifiWizard2.scan([options], success, fail)
```
- Same as calling `startScan` and then `getScanResults`, but this method returns the networks found in the callback

# iOS Functions
For functionality, you need to note the following:
 - Connect/Disconnect only works for iOS11+
 - Can't run in the simulator so you need to attach an actual device when building with xCode
 - Need to add the 'HotspotConfiguration' and 'NetworkExtensions' capabilities to your xCode project

```javascript
WifiWizard2.iOSConnectNetwork(ssid, ssidPassword, success, fail)
```
```javascript
WifiWizard2.iOSDisconnectNetwork(ssid, success, fail)
```

# Android Functions
 - Based off the original [WifiWizard](https://github.com/hoerresb/WifiWizard) however will undergo a rework. 
 - **WifiWizard2** *will automagically try to enable WiFi if it's disabled when calling any android related methods that require WiFi to be enabled*

```javascript
WifiWizard2.androidConnectNetwork(ssid, success, fail)
```
 - WifiWizard will automatically disable/disconnect from currently connected networks to connect to SSID passed
```javascript
WifiWizard2.androidDisconnectNetwork(ssid, success, fail)
```

```javascript
WifiWizard2.formatWifiConfig(ssid, password, algorithm)
```
 - `algorithm` is not required if connecting to an open network
 - Currently `WPA` and `WEP` are only supported algorithms
```javascript
WifiWizard2.formatWPAConfig(ssid, password)
```
```javascript
WifiWizard2.addNetwork(wifi, success, fail)
```
 - `wifi` must be an object formatted by `formatWifiConfig`
```javascript
WifiWizard2.removeNetwork(wifi, success, fail)
```
```javascript
WifiWizard2.listNetworks(success, fail)
```
```javascript
WifiWizard2.startScan(success, fail)
```
```javascript
WifiWizard2.getScanResults([options], success, fail)
```
- `[options]` is optional, if you do not want to specify, just pass `success` callback as first parameter, and `fail` callback as second parameter
- Retrieves a list of the available networks as an array of objects and passes them to the function listHandler. The format of the array is:
```javascript
networks = [
    {   "level": signal_level, // raw RSSI value
        "SSID": ssid, // SSID as string, with escaped double quotes: "\"ssid name\""
        "BSSID": bssid // MAC address of WiFi router as string
        "frequency": frequency of the access point channel in MHz
        "capabilities": capabilities // Describes the authentication, key management, and encryption schemes supported by the access point.
    }
]
```
An options object may be passed. Currently, the only supported option is `numLevels`, and it has the following behavior: 

- if `(n == true || n < 2)`, `*.getScanResults({numLevels: n})` will return data as before, split in 5 levels;
- if `(n > 1)`, `*.getScanResults({numLevels: n})` will calculate the signal level, split in n levels;
- if `(n == false)`, `*.getScanResults({numLevels: n})` will use the raw signal level;

```javascript
WifiWizard2.isWifiEnabled(success, fail)
```
```javascript
WifiWizard2.setWifiEnabled(enabled, success, fail)
```
 - Pass `true` for `enabled` parameter to set Wifi enabled
```javascript
WifiWizard2.getConnectedNetworkID(success, fail)
```
 - Returns currently connected network ID in success callback (only if connected), otherwise fail callback will be called

### Installation

##### Master
Run ```cordova plugin add https://github.com/tripflex/wifiwizard2``` 

This plugin is in active development. If you are wanting to have the latest and greatest stable version, then run the 'Releases' command below.

##### Releases
Run ```cordova plugin add wifiwizard2```

##### Meteor
To install and use this plugin in a Meteor project, you have to specify the exact version from NPM repository:
[https://www.npmjs.com/package/wifiwizard2](https://www.npmjs.com/package/wifiwizard2)

As of 1/7/2017, the latest version is 2.0.0:

```meteor add cordova:wifiwizard2@2.0.0```

License
----

Apache 2.0

## Changelog:

#### 2.0.0
- Added automatic disable of currently connected network on connect call (to prevent reconnect to previous wifi ssid)
- Added initial `disconnect()` before and `reconnect()` after disable/enable network on connect call
- Added `getConnectedNetworkID` to return currently connected network ID
- Added `verifyWiFiEnabled` to automatically enable WiFi for methods that require WiFi to be enabled
- Strip enclosing double quotes returned on getSSID (android) [@props lianghuiyuan](https://github.com/hoerresb/WifiWizard/pull/59)
- Fixed Android memory leak [@props Maikell84](https://github.com/hoerresb/WifiWizard/pull/122)
- Add new ScanResult fields to results (centerFreq0,centerFreq1,channelWidth) [@props essboyer](https://github.com/hoerresb/WifiWizard/pull/102)
- Added getConnectedBSSID to Android platform [@props caiocruz](https://github.com/hoerresb/WifiWizard/pull/82)
- Added isWiFiEnabled implementation for ios [@props caiocruz](https://github.com/hoerresb/WifiWizard/pull/80)
- Android Marshmallow Location Permissions Request [@props jimcortez](https://github.com/hoerresb/WifiWizard/pull/77)
- Only return connected SSID if supplicantState is COMPLETED [@props admund1](https://github.com/hoerresb/WifiWizard/pull/75)
- Added support for WEP [@props adamtegen](https://github.com/hoerresb/WifiWizard/pull/62)
- Fix null issues with getConnectedSSID [@props jeffcharles](https://github.com/hoerresb/WifiWizard/pull/56)
- Added `scan` method to return networks in callback [@props jeffcharles](https://github.com/hoerresb/WifiWizard/pull/55)
- Call success callback after checking connection (on connect to network) [@props jeffcharles](https://github.com/hoerresb/WifiWizard/pull/46)

**Changelog below this line, is from original WifiWizard**


#### v0.2.9

`isWifiEnabled` bug fixed. `level` in `getScanResults` object now refers to raw RSSI value. The function now accepts an options object, and by specifiying `{ numLevels: value }` you can get the old behavior.

#### v0.2.8

`getScanResults` now returns the BSSID along with the SSID and strength of the network.

#### v0.2.7

- Clobber WifiWizard.js automatically via Cordova plugin architecture

#### v0.2.6 

- Added `isWifiEnabled`, `setWifiEnabled`

#### v0.2.5 

- Fixes `getConnectedSSID` error handlers

#### v0.2.4 

- Added `getConnectedSSID` method

#### v0.2.3 

- Added `disconnect` that does disconnection on current WiFi

#### v0.2.2 

- Added `startScan` and `getScanResults`

#### v0.2.1 

- Fixed reference problem in `formatWPAConfig`

#### v0.2.0 

- Changed format of wifiConfiguration object to allow more extensibility.

#### v0.1.1 

- `addNetwork` will now update the network if the SSID already exists.

#### v0.1.0 

- All functions now work!

#### v0.0.3 

- Fixed errors in native implementation. Currently, Add and Remove networks aren't working, but others are working as expected.

#### v0.0.2 

- Changed plugin.xml and WifiWizard.js to attach WifiWizard directly to the HTML.

#### v0.0.1 

- Initial commit
