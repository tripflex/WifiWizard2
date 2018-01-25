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

package wifiwizard2;

import org.apache.cordova.*;
import java.util.List;
import java.util.concurrent.Future;
import java.lang.InterruptedException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.content.Context;
import android.util.Log;
import android.os.Build.VERSION;


public class WifiWizard2 extends CordovaPlugin {

    private static final String ADD_NETWORK = "addNetwork";
    private static final String REMOVE_NETWORK = "removeNetwork";
    private static final String CONNECT_NETWORK = "androidConnectNetwork";
    private static final String DISCONNECT_NETWORK = "androidDisconnectNetwork";
    private static final String DISCONNECT = "disconnect";
    private static final String LIST_NETWORKS = "listNetworks";
    private static final String START_SCAN = "startScan";
    private static final String GET_SCAN_RESULTS = "getScanResults";
    private static final String GET_CONNECTED_SSID = "getConnectedSSID";
    private static final String GET_CONNECTED_BSSID = "getConnectedBSSID";
    private static final String GET_CONNECTED_NETWORKID = "getConnectedNetworkID";
    private static final String IS_WIFI_ENABLED = "isWifiEnabled";
    private static final String SET_WIFI_ENABLED = "setWifiEnabled";
    private static final String TAG = "WifiWizard2";
    private static final int    API_VERSION = VERSION.SDK_INT;
    private static final String SCAN = "scan";

    private WifiManager wifiManager;
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.wifiManager = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext)
                            throws JSONException {

        boolean wifiIsEnabled = verifyWifiEnabled();
        this.callbackContext = callbackContext;

        // Actions that do not require WiFi to be enabled
        if(action.equals(IS_WIFI_ENABLED)) {
            this.isWifiEnabled(callbackContext);
            return true;
        }
        else if(action.equals(SET_WIFI_ENABLED)) {
            this.setWifiEnabled(callbackContext, data);
            return true;
        }

        if( ! wifiIsEnabled ) {
            callbackContext.error("Wifi is not enabled.");
            return true; // Even though enable wifi failed, we still return true and handle error in callback
        }

        // Actions that DO require WiFi to be enabled
        if(action.equals(ADD_NETWORK)) {
            this.addNetwork(callbackContext, data);
        }
        else if(action.equals(SCAN)) {
            this.scan(callbackContext, data);
        }
        else if(action.equals(REMOVE_NETWORK)) {
            this.removeNetwork(callbackContext, data);
        }
        else if(action.equals(CONNECT_NETWORK)) {
            this.androidConnectNetwork(callbackContext, data);
        }
        else if(action.equals(DISCONNECT_NETWORK)) {
            this.androidDisconnectNetwork(callbackContext, data);
        }
        else if(action.equals(LIST_NETWORKS)) {
            this.listNetworks(callbackContext);
        }
        else if(action.equals(START_SCAN)) {
            this.startScan(callbackContext);
        }
        else if(action.equals(GET_SCAN_RESULTS)) {
            this.getScanResults(callbackContext, data);
        }
        else if(action.equals(DISCONNECT)) {
            this.disconnect(callbackContext);
        }
        else if(action.equals(GET_CONNECTED_SSID)) {
            this.getConnectedSSID(callbackContext);
        }
        else if(action.equals(GET_CONNECTED_BSSID)) {
            this.getConnectedBSSID(callbackContext);
        }
        else if(action.equals(GET_CONNECTED_NETWORKID)) {
            this.getConnectedNetworkID(callbackContext);
        }
        else {
            callbackContext.error("Incorrect action parameter: " + action);
            // The ONLY time to return FALSE is when action does not exist that was called
            // Returning false results in an INVALID_ACTION error, which translates to an error callback invoked on the JavaScript side
            // All other errors should be handled with the fail callback (callbackContext.error)
            // @see https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html
            return false;
        }

        return true;
    }

    /**
     * This method will check if WiFi is enabled, and enable it if not, waiting up to 10 seconds for it to enable
     *
     * @return True if wifi is enabled, false if unable to enable wifi
     */
    private boolean verifyWifiEnabled(){

        Log.d(TAG, "WifiWizard2: verifyWifiEnabled entered.");

        if (!wifiManager.isWifiEnabled()) {

            Log.i(TAG, "Enabling wi-fi...");

            if (wifiManager.setWifiEnabled(true)) {
                Log.i(TAG, "Wi-fi enabled");
            } else {
                Log.e(TAG, "Wi-fi could not be enabled!");
                return false;
            }

            // This happens very quickly, but need to wait for it to enable. A little busy wait?
            int count = 0;

			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ie) {
				// continue
			}
		
            while (!wifiManager.isWifiEnabled()) {
                if (count >= 10) {
                    Log.i(TAG, "Took too long to enable wi-fi, quitting");
                    return false;
                }

                Log.i(TAG, "Still waiting for wi-fi to enable...");

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ie) {
                    // continue
                }

                count++;
            }

            // If we make it this far, wifi should be enabled by now
            return true;

        } else {

            return true;

        }

    }

    /**
     * WEP has two kinds of password, a hex value that specifies the key or
     * a character string used to generate the real hex. This checks what kind of
     * password has been supplied. The checks correspond to WEP40, WEP104 & WEP232
     * @param s
     * @return
     */
    private static boolean getHexKey(String s) {
        if (s == null) {
            return false;
        }

        int len = s.length();
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        for (int i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Class to store finished boolean in
     */
    private class ScanSyncContext {
        public boolean finished = false;
    }

    /**
     * Scans networks and sends the list back on the success callback
     * @param callbackContext   A Cordova callback context
     * @param data  JSONArray with [0] == JSONObject
     * @return true
     */
    private boolean scan(final CallbackContext callbackContext, final JSONArray data) {
        Log.v(TAG, "Entering startScan");
        final ScanSyncContext syncContext = new ScanSyncContext();

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "Entering onReceive");

                synchronized (syncContext) {
                    if (syncContext.finished) {
                        Log.v(TAG, "In onReceive, already finished");
                        return;
                    }
                    syncContext.finished = true;
                    context.unregisterReceiver(this);
                }

                Log.v(TAG, "In onReceive, success");
                getScanResults(callbackContext, data);
            }
        };

        final Context context = cordova.getActivity().getApplicationContext();

        Log.v(TAG, "Submitting timeout to threadpool");

        cordova.getThreadPool().submit(new Runnable() {

            public void run() {

                Log.v(TAG, "Entering timeout");

                final int TEN_SECONDS = 10000;

                try {
                    Thread.sleep(TEN_SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Received InterruptedException e, " + e);
                    // keep going into error
                }

                Log.v(TAG, "Thread sleep done");

                synchronized (syncContext) {
                    if (syncContext.finished) {
                        Log.v(TAG, "In timeout, already finished");
                        return;
                    }
                    syncContext.finished = true;
                    context.unregisterReceiver(receiver);
                }

                Log.v(TAG, "In timeout, error");
                callbackContext.error("Timed out waiting for scan to complete");
            }

        });

        Log.v(TAG, "Registering broadcastReceiver");
        context.registerReceiver(
                receiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        );

        if (!wifiManager.startScan()) {
            Log.v(TAG, "Scan failed");
            callbackContext.error("Scan failed");
            return false;
        }

        Log.v(TAG, "Starting wifi scan");
        return true;
    }

    /**
     * This methods adds a network to the list of available WiFi networks.
     * If the network already exists, then it updates it.
     *
     * @params callbackContext     A Cordova callback context.
     * @params data                JSON Array with [0] == SSID, [1] == password
     * @return true    if add successful, false if add fails
     */
    private boolean addNetwork(CallbackContext callbackContext, JSONArray data) {

        Log.d(TAG, "WifiWizard2: addNetwork entered.");

        // Initialize the WifiConfiguration object
        WifiConfiguration wifi = new WifiConfiguration();

        try {
            // data's order for ANY object is 0: ssid, 1: authentication algorithm,
            // 2+: authentication information.
            String authType = data.getString(1);


            if (authType.equals("WPA")) {
                // WPA Data format:
                // 0: ssid
                // 1: auth
                // 2: password
                String newSSID = data.getString(0);
                wifi.SSID = newSSID;
                String newPass = data.getString(2);
                wifi.preSharedKey = newPass;

                wifi.status = WifiConfiguration.Status.ENABLED;
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifi.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifi.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                wifi.networkId = ssidToNetworkId(newSSID);

                if ( wifi.networkId == -1 ) {
                    wifiManager.addNetwork(wifi);
                    callbackContext.success(newSSID + " successfully added.");
                }
                else {
                    wifiManager.updateNetwork(wifi);
                    callbackContext.success(newSSID + " successfully updated.");
                }

                wifiManager.saveConfiguration();
                return true;
            }
            else if (authType.equals("WEP")) {
                // WEP Data format:
                // 0: ssid
                // 1: auth
                // 2: password
                String newSSID = data.getString(0);
                wifi.SSID = newSSID;
                String newPass = data.getString(2);

                if (getHexKey(newPass)) {
                    wifi.wepKeys[0] = newPass;
                }
                else {
                    wifi.wepKeys[0] = "\"" + newPass + "\"";
                }
                wifi.wepTxKeyIndex = 0;

                wifi.status = WifiConfiguration.Status.ENABLED;
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifi.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifi.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifi.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifi.allowedProtocols.set(WifiConfiguration.Protocol.WPA);


                wifi.networkId = ssidToNetworkId(newSSID);

                if ( wifi.networkId == -1 ) {
                    wifiManager.addNetwork(wifi);
                    callbackContext.success(newSSID + " successfully added.");
                }
                else {
                    wifiManager.updateNetwork(wifi);
                    callbackContext.success(newSSID + " successfully updated.");
                }

                wifiManager.saveConfiguration();
                return true;
            }
            else if (authType.equals("NONE")) {
                String newSSID = data.getString(0);
                wifi.SSID = newSSID;
                wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wifi.networkId = ssidToNetworkId(newSSID);

                if ( wifi.networkId == -1 ) {
                    wifiManager.addNetwork(wifi);
                    callbackContext.success(newSSID + " successfully added.");
                }
                else {
                    wifiManager.updateNetwork(wifi);
                    callbackContext.success(newSSID + " successfully updated.");
                }

                wifiManager.saveConfiguration();
                return true;
            }
            // TODO: Add more authentications as necessary
            else {
                Log.d(TAG, "Wifi Authentication Type Not Supported.");
                callbackContext.error("Wifi Authentication Type Not Supported: " + authType);
                return false;
            }
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
            Log.d(TAG,e.getMessage());
            return false;
        }
    }

    /**
     *    This method removes a network from the list of configured networks.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @param    data                JSON Array, with [0] being SSID to remove
     *    @return    true if network removed, false if failed
     */
    private boolean removeNetwork(CallbackContext callbackContext, JSONArray data) {
        Log.d(TAG, "WifiWizard2: removeNetwork entered.");

        if(!validateData(data)) {
            callbackContext.error("WifiWizard2: removeNetwork data invalid");
            Log.d(TAG, "WifiWizard2: removeNetwork data invalid");
            return false;
        }

        // TODO: Verify the type of data!
        try {
            String ssidToDisconnect = data.getString(0);

            int networkIdToRemove = ssidToNetworkId(ssidToDisconnect);

            if (networkIdToRemove >= 0) {
                wifiManager.removeNetwork(networkIdToRemove);
                wifiManager.saveConfiguration();
                callbackContext.success("Network removed.");
                return true;
            }
            else {
                callbackContext.error("Network not found.");
                Log.d(TAG, "WifiWizard2: Network not found, can't remove.");
                return false;
            }
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
            Log.d(TAG, e.getMessage());
            return false;
        }
    }

    /**
     *    This method connects a network.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @param    data                JSON Array, with [0] being SSID to connect
     *    @return    true if network connected, false if failed
     */
    private boolean androidConnectNetwork(CallbackContext callbackContext, JSONArray data) {
        Log.d(TAG, "WifiWizard2: connectNetwork entered.");

        if(!validateData(data)) {
            callbackContext.error("WifiWizard2: connectNetwork invalid data");
            Log.d(TAG, "WifiWizard2: connectNetwork invalid data.");
            return false;
        }

        String ssidToConnect = "";

        try {
            ssidToConnect = data.getString(0);
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
            Log.d(TAG, e.getMessage());
            return false;
        }

        int networkIdToConnect = ssidToNetworkId(ssidToConnect);

        // Attempt to get currently connected network ID
        int networkIdToDisable = getConnectedNetId();

        // If not currently connected to a a wifi network, use network id we want to connect to for disable
        if( networkIdToDisable == -1 ){
            networkIdToDisable = networkIdToConnect;
        }

        if (networkIdToConnect >= 0) {
            // We disable the network before connecting, because if this was the last connection before
            // a disconnect(), this will not reconnect.

            Log.d(TAG, "Valid networkIdToConnect: attempting connection");

            wifiManager.disconnect();
            wifiManager.disableNetwork(networkIdToDisable);
            wifiManager.enableNetwork(networkIdToConnect, true);
            wifiManager.reconnect();

            //
            final int TIMES_TO_RETRY = 30;
            for(int i = 0; i < TIMES_TO_RETRY; i++) {

                WifiInfo info = wifiManager.getConnectionInfo();
                NetworkInfo.DetailedState connectionState = info.getDetailedStateOf(info.getSupplicantState());

                boolean isConnected =
                        // need to ensure we're on correct network because sometimes this code is
                        // reached before the initial network has disconnected
                        info.getNetworkId() == networkIdToConnect && (
                                connectionState == NetworkInfo.DetailedState.CONNECTED ||
                                // Android seems to sometimes get stuck in OBTAINING_IPADDR after it has received one
                                (connectionState == NetworkInfo.DetailedState.OBTAINING_IPADDR && info.getIpAddress() != 0)
                        );
                if (isConnected) {
                    callbackContext.success("Network " + ssidToConnect + " connected!");
                    return true;
                }

                Log.d(TAG, "WifiWizard: Got " + connectionState.name() + " on " + (i + 1) + " out of " + TIMES_TO_RETRY);
                final int ONE_SECOND = 1000;
                try {
                    Thread.sleep(ONE_SECOND);
                }
                catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage());
                    callbackContext.error("Received InterruptedException while connecting");
                    return false;
                }
            }
            callbackContext.error("Network " + ssidToConnect + " failed to finish connecting within the timeout");
            Log.d(TAG, "WifiWizard: Network failed to finish connecting within the timeout");
            return false;


        }else{
            callbackContext.error("WifiWizard2: Cannot connect to network");
            return false;
        }
    }

    /**
     *    This method disconnects a network.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @param    data                JSON Array, with [0] being SSID to connect
     *    @return    true if network disconnected, false if failed
     */
    private boolean androidDisconnectNetwork(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: androidDisconnectNetwork entered.");
        if(!validateData(data)) {
            callbackContext.error("WifiWizard2: androidDisconnectNetwork invalid data");
            Log.d(TAG, "WifiWizard2: androidDisconnectNetwork invalid data");
            return false;
        }

        String ssidToDisconnect = "";

        // TODO: Verify type of data here!
        try {
            ssidToDisconnect = data.getString(0);
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
            Log.d(TAG, e.getMessage());
            return false;
        }

        int networkIdToDisconnect = ssidToNetworkId(ssidToDisconnect);

        if (networkIdToDisconnect > 0) {
            wifiManager.disableNetwork(networkIdToDisconnect);
            callbackContext.success("Network " + ssidToDisconnect + " disconnected!");
            return true;
        }
        else {
            callbackContext.error("Network " + ssidToDisconnect + " not found!");
            Log.d(TAG, "WifiWizard2: Network not found to disconnect.");
            return false;
        }
    }

    /**
     *    This method disconnects current network.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    true if network disconnected, false if failed
     */
    private boolean disconnect(CallbackContext callbackContext) {
        Log.d(TAG, "WifiWizard2: disconnect entered.");

        if (wifiManager.disconnect()) {
            callbackContext.success("Disconnected from current network");
            return true;
        } else {
            callbackContext.error("Unable to disconnect from the current network");
            return false;
        }
    }

    /**
     *    This method uses the callbackContext.success method to send a JSONArray
     *    of the currently configured networks.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    true if network disconnected, false if failed
     */
    private boolean listNetworks(CallbackContext callbackContext) {
        Log.d(TAG, "WifiWizard2: listNetworks entered.");
        List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();

        JSONArray returnList = new JSONArray();

        for (WifiConfiguration wifi : wifiList) {
            returnList.put(wifi.SSID);
        }

        callbackContext.success(returnList);

        return true;
    }

    /**
       *    This method uses the callbackContext.success method to send a JSONArray
       *    of the scanned networks.
       *
       *    @param    callbackContext        A Cordova callback context
       *    @param    data                   JSONArray with [0] == JSONObject
       *    @return    true
       */
    private boolean getScanResults(CallbackContext callbackContext, JSONArray data) {
        List<ScanResult> scanResults = wifiManager.getScanResults();

        JSONArray returnList = new JSONArray();

        Integer numLevels = null;

        if(!validateData(data)) {
            callbackContext.error("WifiWizard2: androidDisconnectNetwork invalid data");
            Log.d(TAG, "WifiWizard2: androidDisconnectNetwork invalid data");
            return false;
        }else if (!data.isNull(0)) {
            try {
                JSONObject options = data.getJSONObject(0);

                if (options.has("numLevels")) {
                    Integer levels = options.optInt("numLevels");

                    if (levels > 0) {
                        numLevels = levels;
                    } else if (options.optBoolean("numLevels", false)) {
                        // use previous default for {numLevels: true}
                        numLevels = 5;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                callbackContext.error(e.toString());
                return false;
            }
        }

        for (ScanResult scan : scanResults) {
            /*
             * @todo - breaking change, remove this notice when tidying new release and explain changes, e.g.:
             *   0.y.z includes a breaking change to WifiWizard2.getScanResults().
             *   Earlier versions set scans' level attributes to a number derived from wifiManager.calculateSignalLevel.
             *   This update returns scans' raw RSSI value as the level, per Android spec / APIs.
             *   If your application depends on the previous behaviour, we have added an options object that will modify behaviour:
             *   - if `(n == true || n < 2)`, `*.getScanResults({numLevels: n})` will return data as before, split in 5 levels;
             *   - if `(n > 1)`, `*.getScanResults({numLevels: n})` will calculate the signal level, split in n levels;
             *   - if `(n == false)`, `*.getScanResults({numLevels: n})` will use the raw signal level;
             */

            int level;

            if (numLevels == null) {
              level = scan.level;
            } else {
              level = wifiManager.calculateSignalLevel(scan.level, numLevels);
            }

            JSONObject lvl = new JSONObject();
            try {
                lvl.put("level", level);
                lvl.put("SSID", scan.SSID);
                lvl.put("BSSID", scan.BSSID);
                lvl.put("frequency", scan.frequency);
                lvl.put("capabilities", scan.capabilities);
                lvl.put("timestamp", scan.timestamp);

                if (API_VERSION >= 23) { // Marshmallow
                    lvl.put("channelWidth", scan.channelWidth);
                    lvl.put("centerFreq0", scan.centerFreq0);
                    lvl.put("centerFreq1", scan.centerFreq1);
                } else {
                    lvl.put("channelWidth", null);
                    lvl.put("centerFreq0", null);
                    lvl.put("centerFreq1", null);
                }

                returnList.put(lvl);
            } catch (JSONException e) {
                e.printStackTrace();
                callbackContext.error(e.toString());
                return false;
            }
        }

        callbackContext.success(returnList);
        return true;
    }

    /**
       *    This method uses the callbackContext.success method. It starts a wifi scanning
       *
       *    @param    callbackContext        A Cordova callback context
       *    @return    true if started was successful
       */
    private boolean startScan(CallbackContext callbackContext) {

        if (wifiManager.startScan()) {
            callbackContext.success();
            return true;
        }
        else {
            callbackContext.error("Scan failed");
            return false;
        }
    }

    /**
     * This method returns the connected WiFi network ID (if connected)
     *
     *    @return    -1 if no network connected, or network id if connected
    */
    private int getConnectedNetId(){
        int networkId = -1;

        WifiInfo info = wifiManager.getConnectionInfo();

        if(info == null){
            Log.d(TAG, "Unable to read wifi info");
            return networkId;
        }

        networkId = info.getNetworkId();

        if( networkId == -1 ){
            Log.d(TAG, "No currently connected net id found");
        }

        return networkId;
    }

    /**
     * This method returns the connected WiFi network ID (if connected)
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    -1 if no network connected, or network id if connected
    */
    private boolean getConnectedNetworkID(CallbackContext callbackContext){
        int networkId = getConnectedNetId();

        if( networkId == -1 ){
            callbackContext.error("Not connected to WiFi or unable to get network ID");
            return false;
        }

        callbackContext.success(networkId);
        return true;
    }

    /**
     * This method retrieves the SSID for the currently connected network
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    true if SSID found, false if not.
    */
    private boolean getConnectedSSID(CallbackContext callbackContext){
        return  getWifiServiceInfo(callbackContext, false);
    }

    /**
     * This method retrieves the BSSID for the currently connected network
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    true if SSID found, false if not.
    */
    private boolean getConnectedBSSID(CallbackContext callbackContext){
        return getWifiServiceInfo(callbackContext, true);
    }

    /**
     * This method retrieves the WifiInformation for the (SSID or BSSID)
     * currently connected network.
     *
     *    @param    callbackContext        A Cordova callback context
     *    @param    basicIdentifier        A flag to get BSSID if true or SSID if false.
     *    @return    true if SSID found, false if not.
    */
    private boolean getWifiServiceInfo(CallbackContext callbackContext, boolean basicIdentifier){

        WifiInfo info = wifiManager.getConnectionInfo();

        if(info == null){
            callbackContext.error("Unable to read wifi info");
            return false;
        }

        // Only return SSID or BSSID when actually connected to a network
        SupplicantState state = info.getSupplicantState();
        if(!state.equals(SupplicantState.COMPLETED)) {
            callbackContext.error("Connection not in COMPLETED state");
            return false;
        }

        String serviceInfo;
        if(basicIdentifier) {
            serviceInfo = info.getBSSID();
        } else {
            serviceInfo = info.getSSID();
        }

        if(serviceInfo == null || serviceInfo.isEmpty() || serviceInfo == "0x"){
            callbackContext.error("Wifi information is empty");
            return false;
        }

        // http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
        if(serviceInfo.startsWith("\"") && serviceInfo.endsWith("\"")){
            serviceInfo = serviceInfo.substring(1, serviceInfo.length()-1);
        }

        callbackContext.success(serviceInfo);
        return true;
    }

    /**
     * This method retrieves the current WiFi status
     *
     *    @param    callbackContext        A Cordova callback context
     *    @return    true if WiFi is enabled, fail will be called if not.
    */
    private boolean isWifiEnabled(CallbackContext callbackContext) {
        boolean isEnabled = wifiManager.isWifiEnabled();
        callbackContext.success(isEnabled ? "1" : "0");
        return isEnabled;
    }

    /**
     *    This method takes a given String, searches the current list of configured WiFi
     *     networks, and returns the networkId for the network if the SSID matches. If not,
     *     it returns -1.
     */
    private int ssidToNetworkId(String ssid) {
        List<WifiConfiguration> currentNetworks = wifiManager.getConfiguredNetworks();
        int networkId = -1;

        // For each network in the list, compare the SSID with the given one
        for (WifiConfiguration test : currentNetworks) {
			if (test.SSID != null && test.SSID.equals(ssid)) {
				networkId = test.networkId;
			}
        }

        return networkId;
    }

    /**
     *    This method enables or disables the wifi
     */
    private boolean setWifiEnabled(CallbackContext callbackContext, JSONArray data) {
        if(!validateData(data)) {
            callbackContext.error("WifiWizard2: androidDisconnectNetwork invalid data");
            Log.d(TAG, "WifiWizard2: androidDisconnectNetwork invalid data");
            return false;
        }

        String status = "";

        try {
            status = data.getString(0);
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
            Log.d(TAG, e.getMessage());
            return false;
        }

        if (wifiManager.setWifiEnabled(status.equals("true"))) {
            callbackContext.success();
            return true;
        }
        else {
            callbackContext.error("Cannot enable wifi");
            return false;
        }
    }

    private boolean validateData(JSONArray data) {
        try {
            if (data == null || data.get(0) == null) {
                callbackContext.error("Data is null.");
                return false;
            }
            return true;
        }
        catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
        return false;
    }

}
