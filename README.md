# WifiWizard2

WifiWizard2 enables Wifi management for both Android and iOS applications within Cordova/Phonegap projects.

This project is a fork of the [WifiWizard](https://github.com/hoerresb/WifiWizard) plugin with fixes and updates, as well as patches taken from the [Cordova Network Manager](https://github.com/arsenal942/Cordova-Network-Manager) plugin.  Majority of credit for code base goes to those projects.

*iOS has limited functionality as Apple's WifiManager equivalent is only available  as a private API. Any app that used these features would not be allowed on the app store.*

## 2.1.x No Longer Supported/Maintained

Version 2.1.x of this plugin is no longer supported or maintained (but pull requests are welcomed), as this plugin has progressed to version 3.x.x+ since the origin fork.  This includes a complete switch to promise based functions, new features, and much more to come.

## Async Handling
Because Cordova `exec` calls are made asynchronously, there are helper functions you can call for each function, by appending `Async` to the end of the function name.  Those functions will return the results, or a JavaScript error.  You should use `await` and `try/catch` blocks.  See below for more details, and examples.

Promises are handled by the [Cordova PromisesPlugin](https://github.com/vstirbu/PromisesPlugin) as an ES6 polyfill if your application does not already define `window.Promise`

It is strongly recommended to use the async functions instead of callbacks, but the choice is up to you, callbacks are still supported and will remain supported for backwards compatibility.

# Global Functions
These are functions that can be used by both Android and iOS applications
```javascript
WifiWizard2.getConnectedSSID(success, fail)
```
```javascript
WifiWizard2.getConnectedSSIDAsync()
```
 - Returns connected network SSID (only if connected) in success callback, otherwise fail callback will be called (if not connected or unable to retrieve)
 - This does **NOT** return the BSSID if unable to obtain SSID (like original WifiWizard did)
```javascript
WifiWizard2.getConnectedBSSID(success, fail)
```
```javascript
WifiWizard2.getConnectedBSSIDAsync()
```
 - Same as above, except BSSID (mac) is returned
```javascript
WifiWizard2.scan([options], success, fail)
```
```javascript
WifiWizard2.scanAsync([options])
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
WifiWizard2.iOSConnectNetworkAsync(ssid, ssidPassword)
```
```javascript
WifiWizard2.iOSDisconnectNetwork(ssid, success, fail)
```
```javascript
WifiWizard2.iOSDisconnectNetworkAsync(ssid)
```

# Android Functions
 - Based off the original [WifiWizard](https://github.com/hoerresb/WifiWizard) however will undergo a rework.
 - **WifiWizard2** *will automagically try to enable WiFi if it's disabled when calling any android related methods that require WiFi to be enabled*

```javascript
WifiWizard2.androidConnectNetwork(ssid, success, fail)
```
```javascript
WifiWizard2.androidConnectNetworkAsync(ssid)
```
 - WifiWizard will automatically disable/disconnect from currently connected networks to connect to SSID passed

```javascript
WifiWizard2.androidDisconnectNetwork(ssid, success, fail)
```
```javascript
WifiWizard2.androidDisconnectNetworkAsync(ssid)
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
```javascript
WifiWizard2.addNetworkAsync(wifi)
```
 - `wifi` must be an object formatted by `formatWifiConfig`
```javascript
WifiWizard2.removeNetwork(wifi, success, fail)
```
```javascript
WifiWizard2.removeNetworkAsync(wifi)
```
```javascript
WifiWizard2.listNetworks(success, fail)
```
```javascript
WifiWizard2.listNetworksAsync()
```
```javascript
WifiWizard2.startScan(success, fail)
```
```javascript
WifiWizard2.startScanAsync()
```
```javascript
WifiWizard2.getScanResults([options], success, fail)
```
```javascript
WifiWizard2.getScanResultsAsync([options])
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
WifiWizard2.isWifiEnabledAsync()
```
```javascript
WifiWizard2.setWifiEnabled(enabled, success, fail)
```
```javascript
WifiWizard2.setWifiEnabledAsync(enabled)
```
 - Pass `true` for `enabled` parameter to set Wifi enabled
 - You do not need to call this function to set WiFi enabled to call other methods that require wifi enabled.  This plugin will automagically enable WiFi if a method is called that requires WiFi to be enabled.
```javascript
WifiWizard2.getConnectedNetworkID(success, fail)
```
```javascript
WifiWizard2.getConnectedNetworkIDAsync()
```
 - Returns currently connected network ID in success callback (only if connected), otherwise fail callback will be called

### Installation

##### 2.1.x Version
Run ```cordova plugin add https://github.com/tripflex/wifiwizard2.git#2.1.x```

This plugin is in active development. If you are wanting to have the latest and greatest stable version, then run the 'Releases' command below.

##### Releases
Run ```cordova plugin add wifiwizard2```

##### Meteor
To install and use this plugin in a Meteor project, you have to specify the exact version from NPM repository:
[https://www.npmjs.com/package/wifiwizard2](https://www.npmjs.com/package/wifiwizard2)

As of 1/9/2017, the latest version (2.x.x version) is 2.1.1 (3.x.x+ is the latest):

```meteor add cordova:wifiwizard2@2.1.1```

### Examples

##### Async WiFi Class

Below is an example class you can use for connecting to WiFi networks on Android.

You will notice there is a `timeout` method that simulates a synchronous timeout/delay/pause, as well as calls to `SUIBlock` which is from my [Meteor Semantic UI Blocker plugin](https://github.com/tripflex/meteor-suiblocker), and is used to provide feedback to the user on their device.  That is what the `timeout` method is used for, to provide a better UI experience for the user by "slowing" down the process by "pausing" for 2 seconds (2000ms) between each call.  You can remove the timeout and calls to `SUIBlock` if you don't need them.

```javascript
class ExampleWiFi {

    constructor( SSID ){
        this.SSID = SSID;
        this.delay = 2000; // delay in ms for timeout
    }

    async connect(){

        try {

            SUIBlock.block( 'Attempting to connect...' ); // Example is using my Semantic UI Blocker Meteor plugin ( https://github.com/tripflex/meteor-suiblocker )
            await this.timeout(); // Timeouts are just used to simulate better UI experience when showing messages on screen

            this.config  = WifiWizard2.formatWifiConfig(this.SSID);

            await this.add();
            await this.doConnect();

            SUIBlock.unblock();

            return true;

        } catch( error ){

            console.log( 'Wifi connect catch error: ', error );
            throw new Error( error.message ); // Throw new error to allow async handling calling this method
        }
    }

    async add(){

        SUIBlock.block( 'Adding ' + this.SSID + ' to mobile device...' );
        await this.timeout();

        try {

            await WifiWizard2.addNetworkAsync( this.config );
            SUIBlock.block( "Successfully added " + this.SSID );
            return true;

        } catch( e ) {

            throw new Error( "Failed to add device WiFi network to your mobile device! Please try again, or manually connect to the device, disconnect, and then return here and try again." );

        }
    }

    async doConnect(){

        SUIBlock.block('Attempting connection to ' + this.SSID + ' ...' );

        await this.timeout();

        try {

            await WifiWizard2.androidConnectNetworkAsync( this.SSID );
            SUIBlock.block( "Successfully connected to " + this.SSID );
            return true;

        } catch( e ){

            throw new Error( "Failed to connect to device WiFi SSID " + this.SSID );

        }
    }


    /**
     * Synchronous Sleep/Timeout `await this.timeout()`
     */
    timeout() {
        let delay = parseInt( this.delay );
        return new Promise(function(resolve, reject) {
            setTimeout(resolve, delay);
        });
    }
}

module.exports = ExampleWiFi; // Not needed if using Meteor
```

##### Calling class from Async method

```javascript
async connectToWiFi() {

    try {
        let wifi = new ExampleWiFi( 'my-ssid' );
        await wifi.connect();

        // Do something after WiFi has connected!

    } catch ( error ){

        console.log( 'Error connecting to WiFi!', error.message );

    }
}
```

##### Calling class from Blaze, or non-async methods

If you're not calling the class from an async function (required to use `await`), you can use `then` and `catch`:

```javascript
var wifi = new ExampleWiFi( 'my-ssid' );
var wifiConnection = wifi.connect();
wifiConnection.then( function( result ){
   // Do something after connecting!
});

wifiConnection.catch( function( error ){
   // Oh no there was an error!
});
```

I recommend using [ES6 arrow functions](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Functions/Arrow_functions) to maintain `this` reference.  This is especially useful if you're using Blaze and Meteor.

```javascript
this.FirstName = 'John';

wifiConnection.then( result => {
   // Do something after connecting!
   // Using arrow functions, you still have access to `this`
   console.log( this.FirstName + ' connected to wifi!' );
});
```

License
----

Apache 2.0

## Changelog:

#### 2.1.1 - *1/9/2017*
- **Added Async Promise based methods**
- Fix issue with thread running before wifi is fully enabled
- Added thread sleep for up to 10 seconds waiting for wifi to enable

#### 2.1.0 - *1/8/2017*
- **Added Async Promise based methods**
- Fixed incorrect Android Cordova exec methods incorrectly being called and returned (fixes INVALID ACTION errors/warnings)
- Updated javascript code to call `fail` callback when error detected in JS (before calling Cordova)
- Moved automagically enabling WiFi to `exec` actions (before actions called that require wifi enabled)
- Added `es6-promise-plugin` cordova dependency to plugin.xml
- Only return `false` in [Cordova Android](https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html) `execute` when invalid action is called
 [Issue #1](https://github.com/tripflex/WifiWizard2/issues/1)
- Added JS doc blocks to JS methods
- Added Async example code

#### 2.0.0 - *1/5/2017*
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
