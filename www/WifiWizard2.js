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

	/**
	 * Get currently connected network SSID
	 * @returns {Promise<any>}
	 */
	getCurrentSSID: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "getConnectedSSID", []);
		});

	},

	/**
	 * Get currently connected network BSSID/MAC
	 * @returns {Promise<any>}
	 */
	getCurrentBSSID: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "getConnectedBSSID", []);
		});
	},

	iOSConnectNetwork: function (ssid, ssidPassword) {

		return new Promise( function( resolve, reject ){

			cordova.exec(resolve, reject, "WifiWizard2", "iOSConnectNetwork", [
				{
					"Ssid": ssid,
					"Password": ssidPassword
				}]
			);

		});

	},

	iOSDisconnectNetwork: function (ssid) {
		return new Promise( function( resolve, reject ){

			cordova.exec(resolve, reject, "WifiWizard2", "iOSDisconnectNetwork", [
				{
					"Ssid": ssid
				}]);

		});
	},

	/**
	 * Add wifi network configuration
	 * @param wifi  Must be object created by formatWifiConfig()
	 * @returns {Promise<any>}
	 */
	addNetwork: function (wifi) {

		return new Promise( function( resolve, reject ){

			if (wifi !== null && typeof wifi === "object") {
				// Ok to proceed!

				let networkInformation = [];

				if (wifi.SSID !== undefined && wifi.SSID !== "") {
					networkInformation.push(wifi.SSID);
				} else {
					reject("No SSID given.");
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
					reject("WifiWizard2: No authentication algorithm given.");
					return false;
				}

				cordova.exec(resolve, reject, "WifiWizard2", "addNetwork", networkInformation);

			} else {
				reject("Invalid parameter. Wifi not an object.");
				return false;
			}
		});
	},

	/**
	 * Remove wifi network configuration
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	removeNetwork: function (SSID) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "removeNetwork", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Connect network with specified SSID
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	connectNetwork: function (SSID) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "connectNetwork", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Disconnect a network based on SSID
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	disconnectNetwork: function (SSID) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "disconnectNetwork", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Returns currently configured networks
	 * @returns {Promise<any>}
	 */
	listNetworks: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "listNetworks", []);
		});
	},

	/**
	 * Get wifi scan results (must call startScan first, or just use scan())
	 * @param options
	 * @returns {Promise<any>}
	 */
	getScanResults: function (options) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "getScanResults", [options]);
		});
	},

	/**
	 * Start wifi network scan (results can be retrieved with getScanResults)
	 * @returns {Promise<any>}
	 */
	startScan: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "startScan", []);
		});
	},

	/**
	 * Disconnect currently connected network
	 * @returns {Promise<any>}
	 */
	disconnect: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "disconnect", []);
		});
	},

	/**
	 * Check if WiFi is enabled
	 * @returns {Promise<any>}
	 */
	isWifiEnabled: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(
				// Cordova can only return strings to JS, and the underlying plugin
				// sends a "1" for true and "0" for false.
				function (result) {
					resolve(result == "1");
				},
				reject, "WifiWizard2", "isWifiEnabled", []
			);
		});
	},

	/**
	 * Enable or Disable WiFi
	 * @param enabled
	 * @returns {Promise<any>}
	 */
	setWifiEnabled: function (enabled) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "setWifiEnabled", [enabled]);
		});
	},

	/**
	 * Get currently connected network ID
	 * @returns {Promise<any>}
	 */
	getConnectedNetworkID: function () {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "getConnectedNetworkID", []);
		});
	},

	/**
	 * Start network scan and return results
	 * @param options
	 * @returns {Promise<any>}
	 */
	scan: function(options) {
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, 'WifiWizard2', 'scan', [options]);
		});
	},

	/**
	 * Get Network ID from SSID
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	getSSIDNetworkID: function( SSID ){
		return new Promise( function( resolve, reject ){
			cordova.exec(resolve, reject, "WifiWizard2", "getSSIDNetworkID", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Enable Network
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	enableNetwork: function( SSID ){
		return new Promise(function(resolve, reject) {
			cordova.exec(resolve, reject, "WifiWizard2", "enableNetwork", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Disable Network
	 * @param SSID
	 * @returns {Promise<any>}
	 */
	disableNetwork: function( SSID ){
		return new Promise(function(resolve, reject) {
			cordova.exec(resolve, reject, "WifiWizard2", "disableNetwork", [WifiWizard2.formatWifiString(SSID)]);
		});
	},

	/**
	 * Reconnect to the currently active access point, even if we are already connected.
	 * @returns {Promise<any>}
	 */
	reassociate: function(){
		return new Promise(function(resolve, reject) {
			cordova.exec(resolve, reject, "WifiWizard2", "reassociate", []);
		});
	},

	/**
	 * Reconnect to the currently active access point, if we are currently disconnected.
	 * @returns {Promise<any>}
	 */
	reconnect: function(){
		return new Promise(function(resolve, reject) {
			cordova.exec(resolve, reject, "WifiWizard2", "reconnect", []);
		});
	},

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

		if( ssid === parseInt(ssid, 10) ){
			return ssid;
		}

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
};

module.exports = WifiWizard2;