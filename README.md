# WifiWizard2

WifiWizard2 enables Wifi management for both Android and iOS applications within Cordova/Phonegap projects.

This project is a fork of the [WifiWizard](https://github.com/hoerresb/WifiWizard) plugin with fixes and updates, as well as patches taken from the [Cordova Network Manager](https://github.com/arsenal942/Cordova-Network-Manager) plugin.  Majority of credit for code base goes to those projects.

iOS has limited functionality as Apple's WifiManager equivalent is only available  as a private API. Any app that used these features would not be allowed on the app store.

# Global Functions
These are functions that can be used by both Android and iOS applications
```javascript
WifiWizard2.getConnectedSSID(success, fail)
```
```javascript
WifiWizard2.getConnectedBSSID(success, fail)
```

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
Based off the original [WifiWizard](https://github.com/hoerresb/WifiWizard) however will undergo a rework. 

```javascript
WifiWizard2.androidConnectNetwork(ssid, ssidToDisable, success, fail)
```
 - 'ssidToDisable' is a parameter that tells the method what network we need to disable.
```javascript
WifiWizard2.androidDisconnectNetwork(ssid, ssidToEnable, success, fail)
```
 - 'ssidToEnable' is a parameter that tells the method what network we need to enable. 'androidConnectNetwork' disables the devices current SSID to avoid the issue whereby Android OS just connects back to the already specified network.
```javascript
WifiWizard2.formatWifiConfig(ssid, password, algorithm)
```
```javascript
WifiWizard2.formatWPAConfig(ssid, password)
```
```javascript
WifiWizard2.addNetwork(wifi, success, fail)
```
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
```javascript
WifiWizard2.isWifiEnabled(success, fail)
```
```javascript
WifiWizard2.setWifiEnabled(enabled, success, fail)
```

### Installation

##### Master
Run ```cordova plugin add https://github.com/tripflex/wifiwizard2``` 

This plugin is in active development. If you are wanting to have the latest and greatest stable version, then run the 'Releases' command below.

##### Releases
Run ```cordova plugin add wifiwizard2```

License
----

MIT
