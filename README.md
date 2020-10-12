# WiFiWizard2 - 3.1.1<!-- omit in toc -->

Table of Contents<!-- omit in toc -->
---
- [About](#about)
- [Basics](#basics)
  - [Async Handling](#async-handling)
  - [Demo Meteor Project](#demo-meteor-project)
  - [Android and IOS Permissions and Notes](#android-and-ios-permissions-and-notes)
  - [IOS Notes](#ios-notes)
  - [Ionic/Angular Notes](#ionicangular-notes)
- [Global Functions](#global-functions)
- [iOS Functions](#ios-functions)
- [Android Functions](#android-functions)
  - [Connect vs Enable](#connect-vs-enable)
  - [Disconnect vs Disable](#disconnect-vs-disable)
  - [New to 3.1.1+](#new-to-311)
  - [New to 3.0.0+](#new-to-300)
- [Installation](#installation)
  - [Master](#master)
  - [Releases](#releases)
  - [Meteor](#meteor)
- [Errors/Rejections](#errorsrejections)
  - [Generic **Thrown Errors**](#generic-thrown-errors)
- [Examples](#examples)
  - [Ionic/Angular Example (User Provided)](#ionicangular-example-user-provided)
- [Changelog:](#changelog)

# About
WifiWizard2 enables Wifi management for both Android and iOS applications within Cordova/Phonegap projects.

This project is a fork of the [WifiWizard](https://github.com/hoerresb/WifiWizard) plugin with fixes and updates, as well as patches taken from the [Cordova Network Manager](https://github.com/arsenal942/Cordova-Network-Manager) plugin.

Version 3.0.0+ has undergone a TON of changes from the original fork (version 2.0), and the majority of method/functions have been changed.  You can find the latest release of version 2 on the [2.1.x branch](https://github.com/tripflex/WifiWizard2/tree/2.1.x)

The recommended version to use is the latest 3+ as that is the version that is actively maintained.

*iOS has limited functionality as Apple's WifiManager equivalent is only available  as a private API. Any app that used these features would not be allowed on the app store.*

**If you are an iOS developer, please consider helping us to resolve the [open iOS issues](https://github.com/tripflex/WifiWizard2/issues/6)**

**If you are an Android developer, please consider helping us to [refactor the current code base](https://github.com/tripflex/WifiWizard2/issues/28)**

**If you're a Cordova developer, please consider helping out this project, open a new issue, a PR, or contact me directly**

# Basics

This plugin creates the object `WifiWizard2` and is accessible after `deviceready` has been fired, see [Cordova deviceready Event Docs](https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready)

```js
document.addEventListener('deviceready', function () {
    // Call some WifiWizard2.method after deviceready fired
}, false);
```

This is really only necessary if you plan on immediately invoking one of this plugin's methods/functions, as the majority of the time you would probably just call the method/function on say, a button click to scan for networks, etc.  Basically if you are going to call something immediately when a webview is shown, make sure to add the event listener for `deviceready` before making that call, otherwise you probably don't need to.

## Async Handling
Because Cordova `exec` calls are made asynchronously, all methods/functions return async promises.  These functions will return the results, or a JavaScript error.  You should use `await` and `try/catch` blocks (or `.then` and `.catch`).  See below for more details, and examples.

**Callbacks are not longer supported in this plugin**

Promises are handled by the [Cordova PromisesPlugin](https://github.com/vstirbu/PromisesPlugin) as an ES6 polyfill if your application does not already define `window.Promise` 

## Demo Meteor Project
To test this plugin as well as provide some example code for others to work off of, I have created an example Meteor project you can find here:

[https://github.com/tripflex/WifiWizard2Demo](https://github.com/tripflex/WifiWizard2Demo)

This demo has examples of using both async functions (with `async/await` and `try/catch` blocks), as well as non async functions with `.then` and `.catch`

## Android and IOS Permissions and Notes
In order to obtain scan results (to call `scan` or `startScan` then `getScanResults`) your application must have the `ACCESS_FINE_LOCATION` Android Permission.  You can do this by calling the `requestPermission` method detailed below, or
this plugin will automagically do this for you when you call `scan` or `startScan` functions.

Newer versions of Android will **not** allow you to `remove`, update existing configuration, or `disable` networks that were not created by your application.  If you are having issues using this features, with your device connected to your computer, run `adb logcat` to view Android Logs for specific error.

## IOS Notes
iOS 12 and later, enable the Access WiFi Information capability for your app in Xcode. If you also want to use the [iOS specific connection functions](#ios-functions) the Hotspot Configuration capability.  When you enable this capability, Xcode automatically adds the Access WiFi Information entitlement to your entitlements file and App ID.

## Ionic/Angular Notes
This plugin does not have @ionic/native typings (yet), in order to use it add this to just below your import list on your service:
`declare var WifiWizard2: any;`

# Global Functions
These are functions that can be used by both Android and iOS applications
```javascript
WifiWizard2.getConnectedSSID()
```
 - Returns connected network SSID (only if connected) in success callback, otherwise fail callback will be called (if not connected or unable to retrieve)
 - This does **NOT** return the BSSID if unable to obtain SSID (like original WifiWizard did)
```javascript
WifiWizard2.getConnectedBSSID()
```
 - Same as above, except BSSID (mac) is returned

```javascript
WifiWizard2.timeout(delay)
```
 - `delay` should be time in milliseconds to delay
 - Helper async timeout delay, `delay` is optional, default is 2000ms = 2 seconds
 - This method always returns a resolved promise after the delay, it will never reject or throw an error

**Example inside async function**
```javascript
async function example(){
    await WifiWizard2.timeout(4000);
    // do something after 4 seconds
}
```

**Example inside standard non-async function**
```javascript
function example(){
    WifiWizard2.timeout(4000).then( function(){
        // do something after waiting 4 seconds
    }):
}
```

 **Thrown Errors**

- `TIMEOUT_WAITING_FOR_SCAN` on timeout waiting for scan 10 seconds +
- `SCAN_FAILED` if unable to start scan

# iOS Functions
For functionality, you need to note the following:
 - Connect/Disconnect only works for iOS11+
 - Can not run in the simulator so you need to attach an actual device when building with Xcode
 - Make sure 'HotspotConfiguration' and 'NetworkExtensions' capabilities are added to your Xcode project
 - To connect to open network omit `ssidPassword` or call with `false`

```javascript
WifiWizard2.iOSConnectNetwork(ssid, ssidPassword)
```
```javascript
WifiWizard2.iOSDisconnectNetwork(ssid)
```

# Android Functions
 - **WifiWizard2** *will automagically try to enable WiFi if it's disabled when calling any android related methods that require WiFi to be enabled*

## Connect vs Enable
When writing Android Java code, there is no `connect` methods, you basically either `enable` or `disable` a network. In the original versions of WifiWizard the `connect` method would basically just call `enable` in Android.
I have changed the way this works in WifiWizard2 version 3.0.0+, converting it to a helper method to eliminate having to call `formatWifiConfig` then `add` and then `enable` ... the `connect` method will now automatically call `formatWifiConfig`, then call `add` to either add or update the network configuration, and then call `enable`.
If the connect method is unable to update existing network configuration (added by user or other apps), but there is a valid network ID, it will still attempt to enable that network ID.

```javascript
WifiWizard2.connect(ssid, bindAll, password, algorithm, isHiddenSSID)
```
 - `ssid` should be the SSID to connect to *required*
 - `bindAll` should be set to `true` to tell Android to route all connections from your Android app, through the wifi connection (default is `false`) *optional*
   - See `WifiWizard2.enable` for more details regarding `bindAll` feature
 - `algorithm` and `password` is not required if connecting to an open network
 - Currently `WPA` and `WEP` are only supported algorithms
 - For `WPA2` just pass `WPA` as the algorithm
 - Set `isHiddenSSID` to `true` if the network you're connecting to is hidden
 - These arguments are the same as for `formatWifiConfig`
 - This method essentially calls `formatWifiConfig` then `add` then `enable`
 - If unable to update network configuration (was added by user or other app), but a valid network ID exists, this method will still attempt to enable the network
 - Promise will not be returned until method has verified that connection to WiFi was in completed state (waits up to 60 seconds)

**Thrown Errors**


 - `CONNECT_FAILED_TIMEOUT` unable to verify connection, timed out after 60 seconds
 - `INVALID_NETWORK_ID_TO_CONNECT` Unable to connect based on generated wifi config
 - `INTERPUT_EXCEPT_WHILE_CONNECTING` Interupt exception while waiting for connection



## Disconnect vs Disable
Same as above for Connect vs Enable, except in this situation, `disconnect` will first disable the network, and then attempt to remove it (if SSID is passed)

```javascript
WifiWizard2.disconnect(ssid)
```
 - `ssid` can either be an SSID (string) or a network ID (integer)
 - `ssid` is **OPTIONAL** .. if not passed, will disconnect current WiFi (almost all Android versions now will just automatically reconnect to last wifi after disconnecting)
 - If `ssid` is provided, this method will first attempt to `disable` and then `remove` the network
 - If you do not want to remove network configuration, use `disable` instead

**Thrown Errors**

 - `DISCONNECT_NET_REMOVE_ERROR` Android returned error when removing wifi configuration
 - `DISCONNECT_NET_DISABLE_ERROR` Unable to connect based on generated wifi config
 - `DISCONNECT_NET_ID_NOT_FOUND` Unable to determine network ID to disconnect/remove (from passed SSID)
 - `ERROR_DISCONNECT` - Android error disconnecting wifi (only when SSID is not passed)

```javascript
WifiWizard2.formatWifiConfig(ssid, password, algorithm, isHiddenSSID)
```
 - `algorithm` and `password` is not required if connecting to an open network
 - Currently `WPA` and `WEP` are only supported algorithms
 - For `WPA2` just pass `WPA` as the algorithm
 - Set `isHiddenSSID` to `true` if the network you're connecting to is hidden
```javascript
WifiWizard2.formatWPAConfig(ssid, password, isHiddenSSID)
```
 - This is just a helper method that calls `WifiWizard2.formatWifiConfig( ssid, password, 'WPA', isHiddenSSID );`

```javascript
WifiWizard2.add(wifi)
```
 - `wifi` must be an object formatted by `formatWifiConfig`, this **must** be done before calling `enable`

**Thrown Errors**

- `AUTH_TYPE_NOT_SUPPORTED` - Invalid auth type specified
- `ERROR_ADDING_NETWORK` - Android returned `-1` specifying error adding network
- `ERROR_UPDATING_NETWORK` - Same as above, except an existing network ID was found, and unable to update it

```javascript
WifiWizard2.remove(ssid)
```
 - `ssid` can either be an SSID (string) or a network ID (integer)
 - Please note, most newer versions of Android will only allow wifi to be removed if created by your application

**Thrown Errors**

 - `UNABLE_TO_REMOVE` Android returned failure in removing network
 - `REMOVE_NETWORK_NOT_FOUND` Unable to determine network ID from passed SSID


```javascript
WifiWizard2.listNetworks()
```

```javascript
WifiWizard2.scan([options])
```
- Same as calling `startScan` and then `getScanResults`, except this method will only resolve the promise after the scan completes and returns the results.

```javascript
WifiWizard2.startScan()
```
 - It is recommended to just use the `scan` method instead of `startScan`

**Thrown Errors**

 - `STARTSCAN_FAILED` Android returned failure in starting scan


```javascript
WifiWizard2.getScanResults([options])
```
- `getScanResults` should only be called after calling `startScan` (it is recommended to use `scan` instead as this starts the scan, then returns the results)
- `[options]` is optional, if you do not want to specify, just pass `success` callback as first parameter, and `fail` callback as second parameter
- Retrieves a list of the available networks as an array of objects and passes them to the function listHandler. The format of the array is:
```javascript
networks = [
    {   "level": signal_level, // raw RSSI value
        "SSID": ssid, // SSID as string, with escaped double quotes: "\"ssid name\""
        "BSSID": bssid // MAC address of WiFi router as string
        "frequency": frequency of the access point channel in MHz
        "capabilities": capabilities // Describes the authentication, key management, and encryption schemes supported by the access point.
        "timestamp": timestamp // timestamp of when the scan was completed
        "channelWidth":
        "centerFreq0":
        "centerFreq1":
    }
]
```
- `channelWidth` `centerFreq0` and `centerFreq1` are only supported on API > 23 (Marshmallow), any older API will return null for these values

An options object may be passed. Currently, the only supported option is `numLevels`, and it has the following behavior: 

- if `(n == true || n < 2)`, `*.getScanResults({numLevels: n})` will return data as before, split in 5 levels;
- if `(n > 1)`, `*.getScanResults({numLevels: n})` will calculate the signal level, split in n levels;
- if `(n == false)`, `*.getScanResults({numLevels: n})` will use the raw signal level;

```javascript
WifiWizard2.isWifiEnabled()
```
 - Returns boolean value of whether Wifi is enabled or not
```javascript
WifiWizard2.setWifiEnabled(enabled)
```
 - Pass `true` for `enabled` parameter to set Wifi enabled
 - You do not need to call this function to set WiFi enabled to call other methods that require wifi enabled.  This plugin will automagically enable WiFi if a method is called that requires WiFi to be enabled.

**Thrown Errors**

 - `ERROR_SETWIFIENABLED` wifi state does not match call (enable or disable)

```javascript
WifiWizard2.getConnectedNetworkID()
```
 - Returns currently connected network ID in success callback (only if connected), otherwise fail callback will be called

**Thrown Errors**

 - `GET_CONNECTED_NET_ID_ERROR` Unable to determine currently connected network ID (may not be connected)

## New to 3.1.1+
```javascript
WifiWizard2.resetBindAll()
```
 - Disable bindAll to WiFi network without disconnecting from WiFi

```javascript
WifiWizard2.setBindAll()
```
 - Enable bindAll to WiFi network without disconnecting from WiFi

```javascript
WifiWizard2.canConnectToInternet()
```

 - Returns boolean, true or false, if device is able to connect to https://www.google.com via HTTP connection (since ping is unreliable)
 - Unknown errors will still be thrown like all other async functions
 - If you called `connect` or `enable` and passed `true` for `bindAll`, your application will force the ping through wifi connection.
 - If you did not pass `true` (or passed `false`) for `bindAll`, and the wifi does not have internet connection, Android Lollipop+ (API 21+) will use cell connection to ping (due to Android using cell connection when wifi does not have internet) [More Details](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)


```javascript
WifiWizard2.canConnectToRouter()
```
 - As `canPingWifiRouter` is notoriously unreliable, this method uses HTTP connection to test if able to connect to router (as most routers should have web server running on port 80)
 - Unknown errors will still be thrown like all other async functions
 - This is useful for testing to make sure that your Android app is able to connect to the private network after connecting to WiFi
 - This was added for testing the `bindAll` feature to support issues with Android Lollipop+ (API 21+) not routing calls through WiFi if WiFi does not have internet connection [See Android Blog](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)
 - Attempts to connect router IP HTTP server on port 80 (example: `http://192.168.0.1/` where `192.168.0.1` is the automatically detected IP address)

## New to 3.0.0+
```javascript
WifiWizard2.isConnectedToInternet()
```

 - Returns boolean, true or false, if device is able to ping 8.8.8.8
 - Unknown errors will still be thrown like all other async functions
 - If you called `connect` or `enable` and passed `true` for `bindAll`, your application will force the ping through wifi connection.
 - If you did not pass `true` (or passed `false`) for `bindAll`, and the wifi does not have internet connection, Android Lollipop+ (API 21+) will use cell connection to ping (due to Android using cell connection when wifi does not have internet) [More Details](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)

```javascript
WifiWizard2.canPingWifiRouter()
```

 - Returns boolean, true or false, if device is able to ping the connected WiFi router IP (obtained from DHCP info)
 - Version 3.1.1+ uses HTTP connection to test if able to connect to router (as ping previous did not work)
 - Unknown errors will still be thrown like all other async functions
 - This is useful for testing to make sure that your Android app is able to connect to the private network after connecting to WiFi
 - This was added for testing the `bindAll` feature to support issues with Android Lollipop+ (API 21+) not routing calls through WiFi if WiFi does not have internet connection [See Android Blog](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)

```javascript
WifiWizard2.enableWifi()
```

```javascript
WifiWizard2.disableWifi()
```

```javascript
WifiWizard2.getWifiIP()
```
 - Returns IPv4 address of currently connected WiFi, or rejects promise if IP not found or wifi not connected

```javascript
WifiWizard2.getWifiRouterIP()
```
 - Returns IPv4 WiFi router IP from currently connected WiFi, or rejects promise if unable to determine, or wifi not connected

**Thrown Errors**

 - `NO_VALID_IP_IDENTIFIED` if unable to determine a valid IP (ip returned from device is `0.0.0.0`)

```javascript
WifiWizard2.getWifiIPInfo()
```
 - Returns a JSON object with IPv4 address and subnet `{"ip": "192.168.1.2", "subnet": "255.255.255.0" }` or rejected promise if not found or not connected
**Thrown Errors**

 - `NO_VALID_IP_IDENTIFIED` if unable to determine a valid IP (ip returned from device is `0.0.0.0`)

```javascript
WifiWizard2.reconnect()
```
 - Reconnect to the currently active access point, **if we are currently disconnected.**

**Thrown Errors**

 - `ERROR_RECONNECT` Android returned error when reconnecting

```javascript
WifiWizard2.reassociate()
```
 - Reconnect to the currently active access point, **even if we are already connected.**

**Thrown Errors**

 - `ERROR_REASSOCIATE` Android returned error when reassociating


```javascript
WifiWizard2.getSSIDNetworkID(ssid)
```
 - Get Android Network ID from passed SSID

```javascript
WifiWizard2.disable(ssid)
```
 - `ssid` can either be an SSID (string) or a network ID (integer)
 - Disable the passed SSID network
 - Please note that most newer versions of Android will only allow you to disable networks created by your application

**Thrown Errors**

 - `UNABLE_TO_DISABLE` Android returned failure in disabling network
 - `DISABLE_NETWORK_NOT_FOUND` Unable to determine network ID from passed SSID to disable


```javascript
WifiWizard2.requestPermission()
```
 - Request `ACCESS_FINE_LOCATION` permssion
 - This Android permission is required to run `scan`, `startStart` and `getScanResults`
 - You can request permission by running this function manually, or WifiWizard2 will automagically request permission when one of the functions above is called

**Thrown Errors**

 - `PERMISSION_DENIED` user denied permission on device


```javascript
WifiWizard2.enable(ssid, bindAll, waitForConnection)
```
 - `ssid` can either be an SSID (string) or a network ID (integer)
 - `bindAll` should be set to `true` to tell Android to route all connections from your Android app, through the wifi connection
    - Android Lollipop+ (API 21+) will not route connections to the WiFi device if it does not have internet connection.  Passing `true` to `bindAll` will force Android to route connections from your Android app through Wifi, regardless of internet connection.
    - If you are having problems connecting to a local IP through WiFi because it does not have internet, try enabling `bindAll` and this should fix the problem.
    - During my testing, some versions of Android (5.0 - 7.1.2) would still route connections through WiFi without internet, but it was random that some versions would and would not work.
    - Testing Android Oreo+ (8.0.0+) if wifi does not have internet, 100% of the time it would NOT route connections through WiFi, so you *must* enable this for Oreo or newer to route connections from your application through wifi without internet.
    - When `bindAll` is enabled, *ALL* connections from your app will be routed through WiFi, until you call `disconnect` or `disable`
    - See the Google Android Blog for [More Details](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)
    - This feature *ONLY* works for Android Lollipop+ (API 21+), if device is running API older than 21, `bindall` will be ignored (as API older than 21 does this by default)
 - Enable the passed SSID network
 - You **MUST** call `WifiWizard2.add(wifi)` before calling `enable` as the wifi configuration must exist before you can enable it (or previously used `connect` without calling `disconnect`)
 - This method does NOT wait or verify connection to wifi network, pass `true` to `waitForConnection` to only return promise once connection is verified in COMPLETED state to specific `ssid`

**Thrown Errors**

`UNABLE_TO_ENABLE` - Android returned `-1` signifying failure enabling

# Installation

## Master

Run ```cordova plugin add https://github.com/tripflex/wifiwizard2```

To install from the master branch (latest on GitHub)

To install a specific branch (add `#tag` replacing `tag` with tag from this repo, example:
```cordova plugin add https://github.com/tripflex/wifiwizard2#v3.1.1```

Find available tags here:
https://github.com/tripflex/WifiWizard2/tags


If you are wanting to have the latest and greatest stable version, then run the 'Releases' command below.

## Releases
Run ```cordova plugin add cordova-plugin-wifiwizard2```

## Meteor
To install and use this plugin in a Meteor project, you have to specify the exact version from NPM repository:
[https://www.npmjs.com/package/cordova-plugin-wifiwizard2](https://www.npmjs.com/package/cordova-plugin-wifiwizard2)

As of April 4th 2019, the latest version is 3.1.1:

```meteor add cordova:cordova-plugin-wifiwizard2@3.1.1```

# Errors/Rejections
Methods now return formatted string errors as detailed below, instead of returning generic error messages.  This allows you to check yourself what specific error was returned, and customize the error message.
In an upcoming release I may add easy ways to override generic messages, or set your own, but for now, errors returned can be found below each method/function.

## Generic **Thrown Errors**
`WIFI_NOT_ENABLED`

# Examples

Please see demo Meteor project for code examples:
[https://github.com/tripflex/WifiWizard2Demo](https://github.com/tripflex/WifiWizard2Demo)

## Ionic/Angular Example (User Provided)
Props @13546777510 (Angelo Fan) has provided a basic Ionic/Angluar demo app:
[https://github.com/13546777510/WifiWizard2-Demo](https://github.com/13546777510/WifiWizard2-Demo)
See issue [#69](https://github.com/tripflex/WifiWizard2/issues/69) regarding this

I recommend using [ES6 arrow functions](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/Arrow_functions) to maintain `this` reference.  This is especially useful if you're using Blaze and Meteor.

```javascript
this.FirstName = 'John';

wifiConnection.then( result => {
   // Do something after connecting!
   // Using arrow functions, you still have access to `this`
   console.log( this.FirstName + ' connected to wifi!' );
});
```

#License
Apache 2.0

# Changelog:
**3.1.1** - April 4, 2019
- Fixed/Added location services check for Android 9+ for any method that utilises the getConnectionInfo method. Issue #71 (@arsenal942)
- Move verifyWifiEnabled() to after methods that do not require wifi to be enabled. Issue #54 (@props seanyang1984)
- Added `canConnectToRouter()` and `canConnectToInternet()` to use HTTP to test connection (since ping is notoriously unreliable)
- Added `canConnectToRouter()`, `canConnectToInternet()`, `canPingWifiRouter()`, `isConnectedToInternet()` to iOS fn return not supported
- Added `resetBindAll()` and `setBindAll()` for Android (props @saoron PR #74)
- Use `JSONObject.NULL` instead of just `null` when scan results Android older than Marshmallow (props @seanyang1984) Issue #51

**3.1.0** - August 28, 2018
- Fixed/Added compatibility with iOS to connect to open network
- Fixed Uncaught SyntaxError in JavaScript from using let (changed to var)
- Fixed main thread blocked while connecting to a network by connecting asynchronously (props @jack828)
- Add isHiddenSSID config for connecting Android devices to hidden networks (props @jack828)
- Update iOSConnectNetwork to return real response in promise (props @saoron)
- Correct iOS SSID comparison (props @saoron)
- iOS Add HotspotConfiguration & NetworkExtension capabilities automatically (props @saoron)
- Removed `jdk.nashorn.internal.codegen.CompilerConstants` import (props @jack828)
- Reduce connection retry count from 60 -> 15 (props @jack828)
- Fixed `Object cannot be converted to int` in Android code (props @xLarry)
- Props to @arsenal942 @jack828 @xLarry @saoron for contributions

**3.0.0** - April 12, 2018
- Completely refactored JS methods, all now return Promises
- Added `getWifiIP` and `getWifiIPInfo` functions
- Added `getWifiRouterIP` function
- Changed method names to be more generalized (`connect` instead of `androidConnectNetwork`, etc)
- Added `requestPermission` and automatic request permission when call method that requires them
- Added `isConnectedToInternet` to ping `8.8.8.8` and verify if wifi has internet connection
- Added `canPingWifiRouter` to ping wifi router IP obtained through DHCP information
- Added `bindAll` feature to use `bindProcessToNetwork` for Marshmallow (API 23+) and `setProcessDefaultNetwork` for Lollipop (API 21-22) [More Details](https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html)
- Converted `connect` to helper method that calls `formatWifiConfig` then `add` then `enable`
- Converted `disconnect` to helper method that calls `disable` then `remove`
- Updated `add` method to set priority of added wifi to highest priority (locates max priority on existing networks and sets to +1)
- Completely refactored and updated all documentation and examples
- Added `ping` Android Java code for possible new methods to ping custom IP/URL (in upcoming releases)
- Updated all error callbacks to use detectable strings (for custom error messages, instead of generic ones)
- DO NOT upgrade from version 2.x.x without reading ENTIRE README! Method/Function names have all been changed!

**2.1.1** - *1/9/2018*
- **Added Async Promise based methods**
- Fix issue with thread running before wifi is fully enabled
- Added thread sleep for up to 10 seconds waiting for wifi to enable

**2.1.0** - *1/8/2018*
- **Added Async Promise based methods**
- Fixed incorrect Android Cordova exec methods incorrectly being called and returned (fixes INVALID ACTION errors/warnings)
- Updated javascript code to call `fail` callback when error detected in JS (before calling Cordova)
- Moved automagically enabling WiFi to `exec` actions (before actions called that require wifi enabled)
- Added `es6-promise-plugin` cordova dependency to plugin.xml
- Only return `false` in [Cordova Android](https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html) `execute` when invalid action is called
 [Issue #1](https://github.com/tripflex/WifiWizard2/issues/1)
- Added JS doc blocks to JS methods
- Added Async example code

**2.0.0** - *1/5/2018*
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


**v0.2.9**

`isWifiEnabled` bug fixed. `level` in `getScanResults` object now refers to raw RSSI value. The function now accepts an options object, and by specifiying `{ numLevels: value }` you can get the old behavior.

**v0.2.8**

`getScanResults` now returns the BSSID along with the SSID and strength of the network.

**v0.2.7**

- Clobber WifiWizard.js automatically via Cordova plugin architecture

**v0.2.6** 

- Added `isWifiEnabled`, `setWifiEnabled`

**v0.2.5** 

- Fixes `getConnectedSSID` error handlers

**v0.2.4** 

- Added `getConnectedSSID` method

**v0.2.3** 

- Added `disconnect` that does disconnection on current WiFi

**v0.2.2** 

- Added `startScan` and `getScanResults`

**v0.2.1** 

- Fixed reference problem in `formatWPAConfig`

**v0.2.0** 

- Changed format of wifiConfiguration object to allow more extensibility.

**v0.1.1** 

- `addNetwork` will now update the network if the SSID already exists.

**v0.1.0** 

- All functions now work!

**v0.0.3** 

- Fixed errors in native implementation. Currently, Add and Remove networks aren't working, but others are working as expected.

**v0.0.2** 

- Changed plugin.xml and WifiWizard.js to attach WifiWizard directly to the HTML.

**v0.0.1** 

- Initial commit
