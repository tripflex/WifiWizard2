/*
 * Copyright 2018 Myles McNamara
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

var WifiWizard2 = {

    /*iOS and Android functions*/

    /**
     * Get currently connected network SSID
     *
     * @param win
     * @param fail
     * @returns {boolean}
     */
    getConnectedSSID: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail( "getConnectedSSID first parameter must be a function to handle SSID.", fail );
            return false;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedSSID", []);
    },
    /**
     * Get currently connnected BSSID (mac)
     * @param win
     * @param fail
     * @returns {boolean}
     */
    getConnectedBSSID: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("getConnectedBSSID first parameter must be a function to handle BSSID.", fail);
            return false;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedBSSID", []);
    },

    /*iOS only functions*/

    /**
     * Connect to network in iOS
     * @param ssid
     * @param ssidPassword
     * @param win
     * @param fail
     */
    iOSConnectNetwork: function (ssid, ssidPassword, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "iOSConnectNetwork", [
            {
                "Ssid": ssid,
                "Password": ssidPassword
            }]);
    },

    /**
     * Disconnect from network in iOS
     * @param ssid
     * @param win
     * @param fail
     */
    iOSDisconnectNetwork: function (ssid, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "iOSDisconnectNetwork", [
            {
                "Ssid": ssid
            }]);
    },

    /*Android only functions*/

    /**
     * Format WiFi configuration for Android Devices
     * @param SSID
     * @param password
     * @param algorithm
     * @returns {*}
     */
    formatWifiConfig: function (SSID, password, algorithm) {
        var wifiConfig = {
            SSID: WifiWizard2.formatWifiString(SSID)
        };
        if (!algorithm && !password) {
            // open network
            wifiConfig.auth = {
                algorithm: "NONE"
            };
        } else if (algorithm === "WPA") {
            wifiConfig.auth = {
                algorithm: algorithm,
                password: WifiWizard2.formatWifiString(password)
                // Other parameters can be added depending on algorithm.
            };
        }
        else if (algorithm === 'WEP') {
            wifiConfig.auth = {
                algorithm : algorithm,
                password : password
                // Other parameters can be added depending on algorithm.
            };
        }
        else if (algorithm === "New network type") {
            wifiConfig.auth = {
                algorithm: algorithm
                // Etc...
            };
        }
        else {
            console.log("Algorithm incorrect");
            return false;
        }
        return wifiConfig;
    },

    /**
     * Format WPA WiFi configuration for Android Devices
     * @param SSID
     * @param password
     * @returns {*}
     */
    formatWPAConfig: function (SSID, password) {
        return WifiWizard2.formatWifiConfig(SSID, password, "WPA");
    },

    /**
     * Format WiFi SSID String
     * @param ssid
     * @returns {*}
     */
    formatWifiString: function (ssid) {
        if (ssid === undefined || ssid === null || ssid === false) {
            ssid = "";
        }
        ssid = ssid.trim();

        if (ssid.charAt(0) != '"') {
            ssid = '"' + ssid;
        }

        if (ssid.charAt(ssid.length - 1) != '"') {
            ssid = ssid + '"';
        }

        return ssid;
    },

    /**
     * Add WiFi Network to Android Device
     *
     * @param wifi  Must be an object formatted by formatWifiConfig
     * @param win
     * @param fail
     * @returns {boolean}
     */
    addNetwork: function (wifi, win, fail) {
        if (wifi !== null && typeof wifi === "object") {
            // Ok to proceed!
        } else {
            this.maybeCallFail("WifiWizard2: Invalid parameter. Wifi not an object.", fail);
            return false;
        }

        var networkInformation = [];

        if (wifi.SSID !== undefined && wifi.SSID !== "") {
            networkInformation.push(wifi.SSID);
        } else {
            this.maybeCallFail("WifiWizard2: No SSID given.", fail);
            return false;
        }

        if (typeof wifi.auth == "object") {

            switch (wifi.auth.algorithm) {
                case "WPA":
                    networkInformation.push("WPA");
                    networkInformation.push(wifi.auth.password);
                    break;
                case 'WEP':
                    networkInformation.push('WEP');
                    networkInformation.push(wifi.auth.password);
                    break;
                case "NONE":
                    networkInformation.push("NONE");
                    break;
                case "Newly supported type":
                    break;
                default:
                    console.log("WifiWizard2: authentication invalid.");
            }

        } else {
            this.maybeCallFail("WifiWizard2: No authentication algorithm given.", fail);
            return false;
        }

        cordova.exec(win, fail, "WifiWizard2", "addNetwork", networkInformation);

        return true;
    },

    /**
     * Remove Network from Android Device
     * @param SSID
     * @param win
     * @param fail
     */
    removeNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "removeNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    /**
     * Connect to SSID on Android Device
     * @param SSID
     * @param win
     * @param fail
     */
    androidConnectNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "androidConnectNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    /**
     * Disconnect from SSID on Android Device
     * @param SSID
     * @param win
     * @param fail
     */
    androidDisconnectNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "androidDisconnectNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    /**
     * List Networks from Android Device
     * @param win
     * @param fail
     * @returns {boolean}
     */
    listNetworks: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("listNetworks first parameter must be a function to handle list.", fail);
            return false;
        }
        cordova.exec(win, fail, "WifiWizard2", "listNetworks", []);
    },

    /**
     * Get scan results from Android Device (must call startScan first)
     * @param options
     * @param win
     * @param fail
     * @returns {boolean}
     */
    getScanResults: function (options, win, fail) {
        if (typeof options === "function") {
            fail = win;
            win = options;
            options = {};
        }

        if (typeof win != "function") {
            this.maybeCallFail("getScanResults first parameter must be a function to handle list.", fail);
            return false;
        }

        cordova.exec(win, fail, "WifiWizard2", "getScanResults", [options]);
    },

    /**
     * Start Wifi scan on Android Device
     * @param win
     * @param fail
     * @returns {boolean}
     */
    startScan: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("startScan first parameter must be a function to handle list.",fail);
            return false;
        }
        cordova.exec(win, fail, "WifiWizard2", "startScan", []);
    },

    /**
     * Disconnect from any network on Android Device
     * @param win
     * @param fail
     * @returns {boolean}
     */
    disconnect: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("disconnect first parameter must be a function to handle list.", fail);
            return false;
        }
        cordova.exec(win, fail, "WifiWizard2", "disconnect", []);
    },

    /**
     * Check if WiFi is enabled
     * @param win
     * @param fail
     * @returns {boolean}
     */
    isWifiEnabled: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("isWifiEnabled first parameter must be a function to handle wifi status.", fail);
            return false;
        }
        cordova.exec(
            // Cordova can only return strings to JS, and the underlying plugin
            // sends a "1" for true and "0" for false.
            function (result) {
                win(result == "1");
            },
            fail, "WifiWizard2", "isWifiEnabled", []
        );
    },

    /**
     * Enable wifi on device
     * @param enabled
     * @param win
     * @param fail
     */
    setWifiEnabled: function (enabled, win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("setWifiEnabled second parameter must be a function to handle enable result.",fail);
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "setWifiEnabled", [enabled]);
    },
    /**
     * Get currently connected network ID on Android Device
     * @param win
     * @param fail
     */
    getConnectedNetworkID: function (win, fail) {
        if (typeof win != "function") {
            this.maybeCallFail("getConnectedNetworkID first parameter must be a function to handle network ID.", fail);
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedNetworkID", []);
    },

    /**
     * Scan WiFi networks and return results
     *
     * @param options
     * @param win callback function
     * @param fail callback function if error
     */
    scan: function(options, win, fail) {
        if (typeof options === 'function') {
            fail = win;
            win = options;
            options = {};
        }

        if (typeof win != 'function' ) {
            this.maybeCallFail("scan first parameters must be a function to handle list.",fail);
            return false;
        }

        cordova.exec(win, fail, 'WifiWizard2', 'scan', [options]);
    },

    /**
     * Call fail callback when there's an error (detected in JS before calling Cordova)
     * @param msg
     * @param cb
     */
    maybeCallFail: function( msg, cb ){
        console.log( msg );
        if(typeof cb == "function") {
            cb( msg );
        }
    },

    // Start ASYNC Promise Functions

    /**
     * Get current SSID Async
     */
    getConnectedSSIDAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.getConnectedSSID( resolve, reject );
        });
    },
    /**
     * Get current BSSID (mac) Async
     */
    getConnectedBSSIDAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.getConnectedBSSID( resolve, reject );
        });
    },
    /**
     * iOS Connect to network SSID Async
     * @param ssid
     * @param ssidPassword
     */
    iOSConnectNetworkAsync: function (ssid, ssidPassword) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.iOSConnectNetwork( ssid, ssidPassword, resolve, reject );
        });
    },
    /**
     * iOS disconnect from SSID Async
     * @param ssid
     */
    iOSDisconnectNetworkAsync: function (ssid) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.iOSDisconnectNetwork( ssid, resolve, reject );
        });
    },
    /**
     * Add network to Android Device Async
     * @param wifi  Must be object formatted by formatWifiConfig()
     */
    addNetworkAsync: function (wifi) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.addNetwork( wifi, resolve, reject );
        });
    },
    /**
     * Remove SSID from Android Device
     * @param ssid
     */
    removeNetworkAsync: function (ssid) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.removeNetwork( ssid, resolve, reject );
        });
    },
    /**
     * Disconnect from SSID on Android Device
     * @param ssid
     */
    androidDisconnectNetworkAsync: function (ssid) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.androidDisconnectNetwork( ssid, resolve, reject );
        });
    },
    /**
     * Connect to SSID on Android Device
     * @param ssid
     */
    androidConnectNetworkAsync: function (ssid) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.androidConnectNetwork( ssid, resolve, reject );
        });
    },
    /**
     * List networks Async
     */
    listNetworksAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.listNetworks( resolve, reject );
        });
    },
    /**
     * Start network scan Async
     */
    startScanAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.startScan( resolve, reject );
        });
    },
    /**
     * Disconnect from any network Async
     */
    disconnectAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.disconnect( resolve, reject );
        });
    },
    /**
     * Check if Wifi is enabled Async
     */
    isWifiEnabledAsync: function () {
        return new Promise(function(resolve, reject) {
            WifiWizard2.isWifiEnabled( resolve, reject );
        });
    },
    /**
     * Enable WiFi Async
     * @param enabled
     */
    setWifiEnabledAsync: function (enabled) {
        return new Promise(function(resolve, reject) {
            WifiWizard2.setWifiEnabled( enabled, resolve, reject );
        });
    },
    /**
     * Get Connected Network ID Async
     */
    getConnectedNetworkIDAsync: function(){
        return new Promise(function(resolve, reject) {
            WifiWizard2.getConnectedNetworkID( options, resolve, reject );
        });
    },
    /**
     * Scan for networks Async
     * @param options
     */
    scanAsync: function( options ){
        return new Promise(function(resolve, reject) {
            WifiWizard2.scan( options, resolve, reject );
        });
    }
};

module.exports = WifiWizard2;
