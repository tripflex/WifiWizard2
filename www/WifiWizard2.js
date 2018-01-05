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

    getCurrentSSID: function (win, fail) {
        if (typeof win != "function") {
            console.log("getCurrentSSID first parameter must be a function to handle SSID.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedSSID", []);
    },

    getCurrentBSSID: function (win, fail) {
        if (typeof win != "function") {
            console.log("getCurrentSSID first parameter must be a function to handle BSSID.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedBSSID", []);
    },

/*iOS only functions*/

    iOSConnectNetwork: function (ssid, ssidPassword, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "iOSConnectNetwork", [
            {
                "Ssid": ssid,
                "Password": ssidPassword
            }]);
    },

    iOSDisconnectNetwork: function (ssid, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "iOSDisconnectNetwork", [
            {
                "Ssid": ssid
            }]);
    },

/*Android only functions*/

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

    formatWPAConfig: function (SSID, password) {
        return WifiWizard2.formatWifiConfig(SSID, password, "WPA");
    },

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

    addNetwork: function (wifi, win, fail) {
        if (wifi !== null && typeof wifi === "object") {
            // Ok to proceed!
        }
        else {
            console.log("WifiWizard2: Invalid parameter. Wifi not an object.");
        }

        var networkInformation = [];

        if (wifi.SSID !== undefined && wifi.SSID !== "") {
            networkInformation.push(wifi.SSID);
        }
        else {
            console.log("WifiWizard2: No SSID given.");
            return false;
        }

        if (typeof wifi.auth == "object") {

            switch (wifi.auth.algorithm) {
            case "WPA":
                networkInformation.push("WPA");
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

        }
        else {
            console.log("WifiWizard2: No authentication algorithm given.");
            return false;
        }

        cordova.exec(win, fail, "WifiWizard2", "addNetwork", networkInformation);
    },

    removeNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "removeNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    androidConnectNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "androidConnectNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    androidDisconnectNetwork: function (SSID, win, fail) {
        cordova.exec(win, fail, "WifiWizard2", "androidDisconnectNetwork", [WifiWizard2.formatWifiString(SSID)]);
    },

    listNetworks: function (win, fail) {
        if (typeof win != "function") {
            console.log("listNetworks first parameter must be a function to handle list.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "listNetworks", []);
    },

    getScanResults: function (options, win, fail) {
        if (typeof options === "function") {
            fail = win;
            win = options;
            options = {};
        }

        if (typeof win != "function") {
            console.log("getScanResults first parameter must be a function to handle list.");
            return;
        }

        cordova.exec(win, fail, "WifiWizard2", "getScanResults", [options]);
    },

    startScan: function (win, fail) {
        if (typeof win != "function") {
            console.log("startScan first parameter must be a function to handle list.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "startScan", []);
    },

    disconnect: function (win, fail) {
        if (typeof win != "function") {
            console.log("disconnect first parameter must be a function to handle list.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "disconnect", []);
    },

    isWifiEnabled: function (win, fail) {
        if (typeof win != "function") {
            console.log("isWifiEnabled first parameter must be a function to handle wifi status.");
            return;
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

    setWifiEnabled: function (enabled, win, fail) {
        if (typeof win != "function") {
            console.log("setWifiEnabled second parameter must be a function to handle enable result.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "setWifiEnabled", [enabled]);
    },
    getConnectedNetworkID: function (win, fail) {
        if (typeof win != "function") {
            console.log("getConnectedNetworkID first parameter must be a function to handle network ID.");
            return;
        }
        cordova.exec(win, fail, "WifiWizard2", "getConnectedNetworkID", []);
    }


};

module.exports = WifiWizard2;
