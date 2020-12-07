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

import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.DhcpInfo;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkSpecifier;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.os.Build.VERSION;
import android.os.PatternMatcher;

import java.net.URL;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.HttpURLConnection;

import java.net.UnknownHostException;

public class WifiWizard2 extends CordovaPlugin {

  private static final String TAG = "WifiWizard2";
  private static final int API_VERSION = VERSION.SDK_INT;

  private static final String ADD_NETWORK = "add";
  private static final String REMOVE_NETWORK = "remove";
  private static final String CONNECT_NETWORK = "connect";
  private static final String DISCONNECT_NETWORK = "disconnectNetwork";
  private static final String DISCONNECT = "disconnect";
  private static final String LIST_NETWORKS = "listNetworks";
  private static final String START_SCAN = "startScan";
  private static final String GET_SCAN_RESULTS = "getScanResults";
  private static final String GET_CONNECTED_SSID = "getConnectedSSID";
  private static final String GET_CONNECTED_BSSID = "getConnectedBSSID";
  private static final String GET_CONNECTED_NETWORKID = "getConnectedNetworkID";
  private static final String IS_WIFI_ENABLED = "isWifiEnabled";
  private static final String SET_WIFI_ENABLED = "setWifiEnabled";
  private static final String SCAN = "scan";
  private static final String ENABLE_NETWORK = "enable";
  private static final String DISABLE_NETWORK = "disable";
  private static final String GET_SSID_NET_ID = "getSSIDNetworkID";
  private static final String REASSOCIATE = "reassociate";
  private static final String RECONNECT = "reconnect";
  private static final String REQUEST_FINE_LOCATION = "requestFineLocation";
  private static final String GET_WIFI_IP_ADDRESS = "getWifiIP";
  private static final String GET_WIFI_ROUTER_IP_ADDRESS = "getWifiRouterIP";
  private static final String CAN_PING_WIFI_ROUTER = "canPingWifiRouter";
  private static final String CAN_CONNECT_TO_ROUTER = "canConnectToRouter";
  private static final String CAN_CONNECT_TO_INTERNET = "canConnectToInternet";
  private static final String IS_CONNECTED_TO_INTERNET = "isConnectedToInternet";
  private static final String RESET_BIND_ALL = "resetBindAll";
  private static final String SET_BIND_ALL = "setBindAll";
  private static final String GET_WIFI_IP_INFO = "getWifiIPInfo";


  
  private static final int SCAN_RESULTS_CODE = 0; // Permissions request code for getScanResults()
  private static final int SCAN_CODE = 1; // Permissions request code for scan()
  private static final int LOCATION_REQUEST_CODE = 2; // Permissions request code
  private static final int WIFI_SERVICE_INFO_CODE = 3;
  private static final String ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;

  private static int LAST_NET_ID = -1;
  // This is for when SSID or BSSID is requested but permissions have not been granted for location
  // we store whether or not BSSID was requested, to recall the getWifiServiceInfo fn after permissions are granted
  private static boolean bssidRequested = false;

  private WifiManager wifiManager;
  private CallbackContext callbackContext;
  private JSONArray passedData;

  private ConnectivityManager connectivityManager;
  private ConnectivityManager.NetworkCallback networkCallback;

  // Store AP, previous, and desired wifi info
  private AP previous, desired;

  private final BroadcastReceiver networkChangedReceiver = new NetworkChangedReceiver();
  private static final IntentFilter NETWORK_STATE_CHANGED_FILTER = new IntentFilter();

  static {
    NETWORK_STATE_CHANGED_FILTER.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
  }

  /**
   * WEP has two kinds of password, a hex value that specifies the key or a character string used to
   * generate the real hex. This checks what kind of password has been supplied. The checks
   * correspond to WEP40, WEP104 & WEP232
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

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.wifiManager = (WifiManager) cordova.getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    this.connectivityManager = (ConnectivityManager) cordova.getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  @Override
  public boolean execute(String action, JSONArray data, CallbackContext callbackContext)
      throws JSONException {

    this.callbackContext = callbackContext;
    this.passedData = data;

    // Actions that do not require WiFi to be enabled
    if (action.equals(IS_WIFI_ENABLED)) {
      this.isWifiEnabled(callbackContext);
      return true;
    } else if (action.equals(SET_WIFI_ENABLED)) {
      this.setWifiEnabled(callbackContext, data);
      return true;
    } else if (action.equals(REQUEST_FINE_LOCATION)) {
      this.requestLocationPermission(LOCATION_REQUEST_CODE);
      return true;
    } else if (action.equals(GET_WIFI_ROUTER_IP_ADDRESS)) {

      String ip = getWiFiRouterIP();

      if ( ip == null || ip.equals("0.0.0.0")) {
        callbackContext.error("NO_VALID_ROUTER_IP_FOUND");
        return true;
      } else {
        callbackContext.success(ip);
        return true;
      }

    } else if (action.equals(GET_WIFI_IP_ADDRESS) || action.equals(GET_WIFI_IP_INFO)) {
      String[] ipInfo = getWiFiIPAddress();
      String ip = ipInfo[0];
      String subnet = ipInfo[1];
      if (ip == null || ip.equals("0.0.0.0")) {
        callbackContext.error("NO_VALID_IP_IDENTIFIED");
        return true;
      }

      // Return only IP address
      if( action.equals( GET_WIFI_IP_ADDRESS ) ){
        callbackContext.success(ip);
        return true;
      }

      // Return Wifi IP Info (subnet and IP as JSON object)
      JSONObject result = new JSONObject();

      result.put("ip", ip);
      result.put("subnet", subnet);

      callbackContext.success(result);
      return true;
    }

    boolean wifiIsEnabled = verifyWifiEnabled();
    if (!wifiIsEnabled) {
      callbackContext.error("WIFI_NOT_ENABLED");
      return true; // Even though enable wifi failed, we still return true and handle error in callback
    }

    // Actions that DO require WiFi to be enabled
    if (action.equals(ADD_NETWORK)) {
      this.add(callbackContext, data);
    } else if (action.equals(IS_CONNECTED_TO_INTERNET)) {
      this.canConnectToInternet(callbackContext, true);
    } else if (action.equals(CAN_CONNECT_TO_INTERNET)) {
      this.canConnectToInternet(callbackContext, false);
    } else if (action.equals(CAN_PING_WIFI_ROUTER)) {
      this.canConnectToRouter(callbackContext, true);
    } else if (action.equals(CAN_CONNECT_TO_ROUTER)) {
      this.canConnectToRouter(callbackContext, false);
    } else if (action.equals(ENABLE_NETWORK)) {
      this.enable(callbackContext, data);
    } else if (action.equals(DISABLE_NETWORK)) {
      this.disable(callbackContext, data);
    } else if (action.equals(GET_SSID_NET_ID)) {
      this.getSSIDNetworkID(callbackContext, data);
    } else if (action.equals(REASSOCIATE)) {
      this.reassociate(callbackContext);
    } else if (action.equals(RECONNECT)) {
      this.reconnect(callbackContext);
    } else if (action.equals(SCAN)) {
      this.scan(callbackContext, data);
    } else if (action.equals(REMOVE_NETWORK)) {
      this.remove(callbackContext, data);
    } else if (action.equals(CONNECT_NETWORK)) {
      this.connect(callbackContext, data);
    } else if (action.equals(DISCONNECT_NETWORK)) {
      this.disconnectNetwork(callbackContext, data);
    } else if (action.equals(LIST_NETWORKS)) {
      this.listNetworks(callbackContext);
    } else if (action.equals(START_SCAN)) {
      this.startScan(callbackContext);
    } else if (action.equals(GET_SCAN_RESULTS)) {
      this.getScanResults(callbackContext, data);
    } else if (action.equals(DISCONNECT)) {
      this.disconnect(callbackContext);
    } else if (action.equals(GET_CONNECTED_SSID)) {
      this.getConnectedSSID(callbackContext);
    } else if (action.equals(GET_CONNECTED_BSSID)) {
      this.getConnectedBSSID(callbackContext);
    } else if (action.equals(GET_CONNECTED_NETWORKID)) {
      this.getConnectedNetworkID(callbackContext);
    } else if (action.equals(RESET_BIND_ALL)) {
      this.resetBindAll(callbackContext);
    } else if (action.equals(SET_BIND_ALL)) {
      this.setBindAll(callbackContext);
    } else {
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
   * Scans networks and sends the list back on the success callback
   *
   * @param callbackContext A Cordova callback context
   * @param data JSONArray with [0] == JSONObject
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
        callbackContext.error("TIMEOUT_WAITING_FOR_SCAN");
      }

    });

    Log.v(TAG, "Registering broadcastReceiver");
    context.registerReceiver(
        receiver,
        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
    );

    if (!wifiManager.startScan()) {
      Log.v(TAG, "Scan failed");
      callbackContext.error("SCAN_FAILED");
      return false;
    }

    Log.v(TAG, "Starting wifi scan");
    return true;
  }

  /**
   * This methods adds a network to the list of available WiFi networks. If the network already
   * exists, then it updates it.
   *
   * @return true    if add successful, false if add fails
   * @params callbackContext     A Cordova callback context.
   * @params data                JSON Array with [0] == SSID, [1] == password
   */
  private boolean add(CallbackContext callbackContext, JSONArray data) {

    Log.d(TAG, "WifiWizard2: add entered.");

    // Initialize the WifiConfiguration object
    WifiConfiguration wifi = new WifiConfiguration();

    try {
      // data's order for ANY object is
      // 0: SSID
      // 1: authentication algorithm,
      // 2: authentication information
      // 3: whether or not the SSID is hidden
      String newSSID = data.getString(0);
      String authType = data.getString(1);
      String newPass = data.getString(2);
      boolean isHiddenSSID = data.getBoolean(3);

      wifi.hiddenSSID = isHiddenSSID;

      if (authType.equals("WPA") || authType.equals("WPA2")) {
       /**
        * WPA Data format:
        * 0: ssid
        * 1: auth
        * 2: password
        * 3: isHiddenSSID
        */
        wifi.SSID = newSSID;
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

      } else if (authType.equals("WEP")) {
       /**
        * WEP Data format:
        * 0: ssid
        * 1: auth
        * 2: password
        * 3: isHiddenSSID
        */
        wifi.SSID = newSSID;

        if (getHexKey(newPass)) {
          wifi.wepKeys[0] = newPass;
        } else {
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

      } else if (authType.equals("NONE")) {
       /**
        * OPEN Network data format:
        * 0: ssid
        * 1: auth
        * 2: <not used>
        * 3: isHiddenSSID
        */
        wifi.SSID = newSSID;
        wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifi.networkId = ssidToNetworkId(newSSID);

      } else {

        Log.d(TAG, "Wifi Authentication Type Not Supported.");
        callbackContext.error("AUTH_TYPE_NOT_SUPPORTED");
        return false;

      }

      // Set network to highest priority (deprecated in API >= 26)
      if(API_VERSION < 26) {
        wifi.priority = getMaxWifiPriority(wifiManager) + 1;
      }

      if(API_VERSION >= 29) {
        this.networkCallback = new ConnectivityManager.NetworkCallback() {
          @Override
          public void onAvailable(Network network) {
            connectivityManager.bindProcessToNetwork(network);
            Log.d(TAG, "WiFi connected");
            callbackContext.success("WiFi connected");
          }
          @Override
          public void onUnavailable() {
           super.onUnavailable();
           Log.d(TAG, "WiFi not available");
           callbackContext.error("WiFi not available");
          }
        };

        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        builder.setSsid(newSSID);
        builder.setWpa2Passphrase(newPass);

        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();

        NetworkRequest.Builder networkRequestBuilder1 = new NetworkRequest.Builder();
        networkRequestBuilder1.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        //removeCapability added for hotspots without internet
        networkRequestBuilder1.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        networkRequestBuilder1.setNetworkSpecifier(wifiNetworkSpecifier);

        NetworkRequest nr = networkRequestBuilder1.build();
        ConnectivityManager cm = (ConnectivityManager) cordova.getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        //timeout add because "No devices found" wasn't handled correct and doesn't throw Unavailable
        cm.requestNetwork(nr, this.networkCallback, 15000);
      } else {
        // After processing authentication types, add or update network
        if(wifi.networkId == -1) { // -1 means SSID configuration does not exist yet

          int newNetId = wifiManager.addNetwork(wifi);
          if( newNetId > -1 ){
            callbackContext.success( newNetId );
          } else {
            callbackContext.error( "ERROR_ADDING_NETWORK" );
          }

        } else {

          int updatedNetID = wifiManager.updateNetwork(wifi);

          if(updatedNetID > -1) {
            callbackContext.success( updatedNetID );
          } else {
            callbackContext.error("ERROR_UPDATING_NETWORK");
          }

        }
      }

      // WifiManager configurations are presistent for API 26+
      if(API_VERSION < 26) {
        wifiManager.saveConfiguration(); // Call saveConfiguration for older < 26 API
      }

      return true;


    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }
  }

  /**
   * This method connects a network.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to connect
   */
  private void enable(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: enable entered.");

    if (!validateData(data)) {
      callbackContext.error("ENABLE_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: enable invalid data.");
      return;
    }

    String ssidToEnable = "";
    String bindAll = "false";
    String waitForConnection = "false";

    try {
      ssidToEnable = data.getString(0);
      bindAll = data.getString(1);
      waitForConnection = data.getString(2);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return;
    }

    int networkIdToEnable = ssidToNetworkId(ssidToEnable);

    try {

      if(networkIdToEnable > -1) {

        Log.d(TAG, "Valid networkIdToEnable: attempting connection");

        // Bind all requests to WiFi network (only necessary for Lollipop+ - API 21+)
        if(bindAll.equals("true")) {
          registerBindALL(networkIdToEnable);
        }

        if(wifiManager.enableNetwork(networkIdToEnable, true)) {

          if( waitForConnection.equals("true") ){
            callbackContext.success("NETWORK_ENABLED");
            return;
          } else {
            new ConnectAsync().execute(callbackContext, networkIdToEnable);
            return;
          }

        } else {
          callbackContext.error("ERROR_ENABLING_NETWORK");
          return;
        }

      } else {
        callbackContext.error("UNABLE_TO_ENABLE");
        return;
      }

    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return;
    }

  }

  /**
   * This method disables a network.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to connect
   * @return true if network disconnected, false if failed
   */
  private boolean disable(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: disable entered.");

    if (!validateData(data)) {
      callbackContext.error("DISABLE_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: disable invalid data");
      return false;
    }

    String ssidToDisable = "";

    try {
      ssidToDisable = data.getString(0);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }

    int networkIdToDisconnect = ssidToNetworkId(ssidToDisable);

    try {

      if (networkIdToDisconnect > 0) {
        if(wifiManager.disableNetwork(networkIdToDisconnect)){
          maybeResetBindALL();
          callbackContext.success("Network " + ssidToDisable + " disabled!");
        } else {
          callbackContext.error("UNABLE_TO_DISABLE");
        }
        return true;
      } else {
        callbackContext.error("DISABLE_NETWORK_NOT_FOUND");
        Log.d(TAG, "WifiWizard2: Network not found to disable.");
        return false;
      }

    } catch (Exception e) {

      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;

    }
  }

  /**
   * This method removes a network from the list of configured networks.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to remove
   * @return true if network removed, false if failed
   */
  private boolean remove(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: remove entered.");

    if (!validateData(data)) {
      callbackContext.error("REMOVE_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: remove data invalid");
      return false;
    }

    // TODO: Verify the type of data!
    try {
      String ssidToDisconnect = data.getString(0);

      int networkIdToRemove = ssidToNetworkId(ssidToDisconnect);

      if (networkIdToRemove > -1) {

          if( wifiManager.removeNetwork(networkIdToRemove) ){

              // Configurations persist by default in API 26+
              if (API_VERSION < 26) {
                  wifiManager.saveConfiguration();
              }

              callbackContext.success("NETWORK_REMOVED");

          } else {

              callbackContext.error( "UNABLE_TO_REMOVE" );
          }

        return true;
      } else {
        callbackContext.error("REMOVE_NETWORK_NOT_FOUND");
        Log.d(TAG, "WifiWizard2: Network not found, can't remove.");
        return false;
      }
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }
  }

  /**
   * This method connects a network.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to connect
   */
  private void connect(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: connect entered.");

    if (!validateData(data)) {
      callbackContext.error("CONNECT_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: connect invalid data.");
      return;
    }

    String ssidToConnect = "";
    String bindAll = "false";

    try {
      ssidToConnect = data.getString(0);
      bindAll = data.getString(1);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return;
    }

    int networkIdToConnect = ssidToNetworkId(ssidToConnect);

    if (networkIdToConnect > -1) {
      // We disable the network before connecting, because if this was the last connection before
      // a disconnect(), this will not reconnect.

      Log.d(TAG, "Valid networkIdToConnect: attempting connection");

      // Bind all requests to WiFi network (only necessary for Lollipop+ - API 21+)
      if( bindAll.equals("true") ){
        registerBindALL(networkIdToConnect);
      }

      if (API_VERSION >= 26) {
//                wifiManager.disconnect();
      } else {
        wifiManager.disableNetwork(networkIdToConnect);
      }

      wifiManager.enableNetwork(networkIdToConnect, true);

      if (API_VERSION >= 26) {
//        wifiManager.reassociate();
      }

      new ConnectAsync().execute(callbackContext, networkIdToConnect);
      return;

    } else {
      callbackContext.error("INVALID_NETWORK_ID_TO_CONNECT");
      return;
    }
  }

  /**
   * Wait for connection before returning error or success
   *
   * This method will wait up to 60 seconds for WiFi connection to specified network ID be in COMPLETED state, otherwise will return error.
   *
   * @param callbackContext
   * @param networkIdToConnect
   * @return
   */
  private class ConnectAsync extends AsyncTask<Object, Void, String[]> {
    CallbackContext callbackContext;
    @Override
    protected void onPostExecute(String[] results) {
      String error = results[0];
      String success = results[1];
      if (error != null) {
        this.callbackContext.error(error);
      } else {
        this.callbackContext.success(success);
      }
    }

    @Override
    protected String[] doInBackground(Object... params) {
      this.callbackContext = (CallbackContext) params[0];
      int networkIdToConnect = (Integer) params[1];

      final int TIMES_TO_RETRY = 15;
      for (int i = 0; i < TIMES_TO_RETRY; i++) {

        WifiInfo info = wifiManager.getConnectionInfo();
        NetworkInfo.DetailedState connectionState = info
            .getDetailedStateOf(info.getSupplicantState());

        boolean isConnected =
            // need to ensure we're on correct network because sometimes this code is
            // reached before the initial network has disconnected
            info.getNetworkId() == networkIdToConnect && (
                connectionState == NetworkInfo.DetailedState.CONNECTED ||
                    // Android seems to sometimes get stuck in OBTAINING_IPADDR after it has received one
                    (connectionState == NetworkInfo.DetailedState.OBTAINING_IPADDR
                        && info.getIpAddress() != 0)
            );

        if (isConnected) {
          return new String[]{ null, "NETWORK_CONNECTION_COMPLETED" };
        }

        Log.d(TAG, "WifiWizard: Got " + connectionState.name() + " on " + (i + 1) + " out of " + TIMES_TO_RETRY);
        final int ONE_SECOND = 1000;

        try {
          Thread.sleep(ONE_SECOND);
        } catch (InterruptedException e) {
          Log.e(TAG, e.getMessage());
          return new String[]{ "INTERRUPT_EXCEPT_WHILE_CONNECTING", null };
        }
      }
      Log.d(TAG, "WifiWizard: Network failed to finish connecting within the timeout");
      return new String[]{ "CONNECT_FAILED_TIMEOUT", null };
    }
  }

  /**
   * This method disconnects a network.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to connect
   * @return true if network disconnected, false if failed
   */
  private boolean disconnectNetwork(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: disconnectNetwork entered.");
    if (!validateData(data)) {
      callbackContext.error("DISCONNECT_NET_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: disconnectNetwork invalid data");
      return false;
    }

    String ssidToDisconnect = "";

    // TODO: Verify type of data here!
    try {
      ssidToDisconnect = data.getString(0);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }

    if(API_VERSION < 29){
        int networkIdToDisconnect = ssidToNetworkId(ssidToDisconnect);

        if(networkIdToDisconnect > 0) {

        if(wifiManager.disableNetwork(networkIdToDisconnect)) {

          maybeResetBindALL();

          // We also remove the configuration from the device (use "disable" to keep config)
          if( wifiManager.removeNetwork(networkIdToDisconnect) ){
            callbackContext.success("Network " + ssidToDisconnect + " disconnected and removed!");
          } else {
            callbackContext.error("DISCONNECT_NET_REMOVE_ERROR");
            Log.d(TAG, "WifiWizard2: Unable to remove network!");
            return false;
          }

        } else {
          callbackContext.error("DISCONNECT_NET_DISABLE_ERROR");
          Log.d(TAG, "WifiWizard2: Unable to disable network!");
          return false;
        }

        return true;
    } else {
      callbackContext.error("DISCONNECT_NET_ID_NOT_FOUND");
      Log.d(TAG, "WifiWizard2: Network not found to disconnect.");
      return false;
    }
    } else {
      try{
          ConnectivityManager cm = (ConnectivityManager) cordova.getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
          cm.unregisterNetworkCallback(this.networkCallback);
          connectivityManager.bindProcessToNetwork(null);
          return true;
        }
        catch(Exception e) {
          callbackContext.error(e.getMessage());
          return false;
        }
      }
  }

  /**
   * This method disconnects the currently connected network.
   *
   * @param callbackContext A Cordova callback context
   * @return true if network disconnected, false if failed
   */
  private boolean disconnect(CallbackContext callbackContext) {
    Log.d(TAG, "WifiWizard2: disconnect entered.");

    if (wifiManager.disconnect()) {
      maybeResetBindALL();
      callbackContext.success("Disconnected from current network");
      return true;
    } else {
      callbackContext.error("ERROR_DISCONNECT");
      return false;
    }
  }

  /**
   * Reconnect Network
   * <p>
   * Reconnect to the currently active access point, if we are currently disconnected. This may
   * result in the asynchronous delivery of state change events.
   */
  private boolean reconnect(CallbackContext callbackContext) {
    Log.d(TAG, "WifiWizard2: reconnect entered.");

    if (wifiManager.reconnect()) {
      callbackContext.success("Reconnected network");
      return true;
    } else {
      callbackContext.error("ERROR_RECONNECT");
      return false;
    }
  }

  /**
   * Reassociate Network
   * <p>
   * Reconnect to the currently active access point, even if we are already connected. This may
   * result in the asynchronous delivery of state change events.
   */
  private boolean reassociate(CallbackContext callbackContext) {
    Log.d(TAG, "WifiWizard2: reassociate entered.");

    if (wifiManager.reassociate()) {
      callbackContext.success("Reassociated network");
      return true;
    } else {
      callbackContext.error("ERROR_REASSOCIATE");
      return false;
    }
  }

  /**
   * This method uses the callbackContext.success method to send a JSONArray of the currently
   * configured networks.
   *
   * @param callbackContext A Cordova callback context
   * @return true if network disconnected, false if failed
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
   * This method uses the callbackContext.success method to send a JSONArray of the scanned
   * networks.
   *
   * @param callbackContext A Cordova callback context
   * @param data JSONArray with [0] == JSONObject
   * @return true
   */
  private boolean getScanResults(CallbackContext callbackContext, JSONArray data) {

    if (cordova.hasPermission(ACCESS_FINE_LOCATION)) {

      List<ScanResult> scanResults = wifiManager.getScanResults();

      JSONArray returnList = new JSONArray();

      Integer numLevels = null;

      if (!validateData(data)) {
        callbackContext.error("GET_SCAN_RESULTS_INVALID_DATA");
        Log.d(TAG, "WifiWizard2: getScanResults invalid data");
        return false;
      } else if (!data.isNull(0)) {
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
            lvl.put("channelWidth", JSONObject.NULL);
            lvl.put("centerFreq0", JSONObject.NULL);
            lvl.put("centerFreq1", JSONObject.NULL);
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

    } else {

      requestLocationPermission(SCAN_RESULTS_CODE);
        return true;
    }

  }

  /**
   * This method uses the callbackContext.success method. It starts a wifi scanning
   *
   * @param callbackContext A Cordova callback context
   * @return true if started was successful
   */
  private boolean startScan(CallbackContext callbackContext) {

    if (wifiManager.startScan()) {
      callbackContext.success();
      return true;
    } else {
      callbackContext.error("STARTSCAN_FAILED");
      return false;
    }
  }

  /**
   * This method returns the connected WiFi network ID (if connected)
   *
   * @return -1 if no network connected, or network id if connected
   */
  private int getConnectedNetId() {
    int networkId = -1;

    WifiInfo info = wifiManager.getConnectionInfo();

    if (info == null) {
      Log.d(TAG, "Unable to read wifi info");
      return networkId;
    }

    networkId = info.getNetworkId();

    if (networkId == -1) {
      Log.d(TAG, "NO_CURRENT_NETWORK_FOUND");
    }

    return networkId;
  }

  /**
   * Get Network ID from SSID
   *
   * @param callbackContext A Cordova callback context
   * @param data JSON Array, with [0] being SSID to connect
   * @return true if network connected, false if failed
   */
  private boolean getSSIDNetworkID(CallbackContext callbackContext, JSONArray data) {
    Log.d(TAG, "WifiWizard2: getSSIDNetworkID entered.");

    if (!validateData(data)) {
      callbackContext.error("GET_SSID_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: getSSIDNetworkID invalid data.");
      return false;
    }

    String ssidToGetNetworkID = "";

    try {
      ssidToGetNetworkID = data.getString(0);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }

    int networkIdToConnect = ssidToNetworkId(ssidToGetNetworkID);
    callbackContext.success(networkIdToConnect);

    return true;
  }

  /**
   * This method returns the connected WiFi network ID (if connected)
   *
   * @param callbackContext A Cordova callback context
   * @return -1 if no network connected, or network id if connected
   */
  private boolean getConnectedNetworkID(CallbackContext callbackContext) {
    int networkId = getConnectedNetId();

    if (networkId == -1) {
      callbackContext.error("GET_CONNECTED_NET_ID_ERROR");
      return false;
    }

    callbackContext.success(networkId);
    return true;
  }

  /**
   * This method retrieves the SSID for the currently connected network
   *
   * @param callbackContext A Cordova callback context
   * @return true if SSID found, false if not.
   */
  private boolean getConnectedSSID(CallbackContext callbackContext) {
    return getWifiServiceInfo(callbackContext, false);
  }

  /**
   * This method retrieves the BSSID for the currently connected network
   *
   * @param callbackContext A Cordova callback context
   * @return true if SSID found, false if not.
   */
  private boolean getConnectedBSSID(CallbackContext callbackContext) {
    return getWifiServiceInfo(callbackContext, true);
  }

  /**
   * This method retrieves the WifiInformation for the (SSID or BSSID) currently connected network.
   *
   * @param callbackContext A Cordova callback context
   * @param basicIdentifier A flag to get BSSID if true or SSID if false.
   * @return true if SSID found, false if not.
   */
  private boolean getWifiServiceInfo(CallbackContext callbackContext, boolean basicIdentifier) {    
    if (API_VERSION >= 23 && !cordova.hasPermission(ACCESS_FINE_LOCATION)) { //Android 9 (Pie) or newer
      requestLocationPermission(WIFI_SERVICE_INFO_CODE);
      bssidRequested = basicIdentifier;
      return true;
    } else {
      WifiInfo info = wifiManager.getConnectionInfo();

      if (info == null) {
        callbackContext.error("UNABLE_TO_READ_WIFI_INFO");
        return false;
      }
  
      // Only return SSID or BSSID when actually connected to a network
      SupplicantState state = info.getSupplicantState();
      if (!state.equals(SupplicantState.COMPLETED)) {
        callbackContext.error("CONNECTION_NOT_COMPLETED");
        return false;
      }
  
      String serviceInfo;
      if (basicIdentifier) {
        serviceInfo = info.getBSSID();
      } else {
        serviceInfo = info.getSSID();
      }
  
      if (serviceInfo == null || serviceInfo.isEmpty() || serviceInfo == "0x") {
        callbackContext.error("WIFI_INFORMATION_EMPTY");
        return false;
      }
  
      // http://developer.android.com/reference/android/net/wifi/WifiInfo.html#getSSID()
      if (serviceInfo.startsWith("\"") && serviceInfo.endsWith("\"")) {
        serviceInfo = serviceInfo.substring(1, serviceInfo.length() - 1);
      }
  
      callbackContext.success(serviceInfo);
      return true;
    }
  }

  /**
   * This method retrieves the current WiFi status
   *
   * @param callbackContext A Cordova callback context
   * @return true if WiFi is enabled, fail will be called if not.
   */
  private boolean isWifiEnabled(CallbackContext callbackContext) {
    boolean isEnabled = wifiManager.isWifiEnabled();
    callbackContext.success(isEnabled ? "1" : "0");
    return isEnabled;
  }

  /**
   * This method takes a given String, searches the current list of configured WiFi networks, and
   * returns the networkId for the network if the SSID matches. If not, it returns -1.
   */
  private int ssidToNetworkId(String ssid) {

    try {
      
      int maybeNetId = Integer.parseInt(ssid);
      Log.d(TAG, "ssidToNetworkId passed SSID is integer, probably a Network ID: " + ssid);
      return maybeNetId;

    } catch (NumberFormatException e) {

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
  }

  /**
   * This method enables or disables the wifi
   */
  private boolean setWifiEnabled(CallbackContext callbackContext, JSONArray data) {
    if (!validateData(data)) {
      callbackContext.error("SETWIFIENABLED_INVALID_DATA");
      Log.d(TAG, "WifiWizard2: setWifiEnabled invalid data");
      return false;
    }

    String status = "";

    try {
      status = data.getString(0);
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }

    if (wifiManager.setWifiEnabled(status.equals("true"))) {
      callbackContext.success();
      return true;
    } else {
      callbackContext.error("ERROR_SETWIFIENABLED");
      return false;
    }
  }

  /**
   * This method will check if WiFi is enabled, and enable it if not, waiting up to 10 seconds for
   * it to enable
   *
   * @return True if wifi is enabled, false if unable to enable wifi
   */
  private boolean verifyWifiEnabled() {

    Log.d(TAG, "WifiWizard2: verifyWifiEnabled entered.");

    if (!wifiManager.isWifiEnabled()) {

      Log.i(TAG, "Enabling wi-fi...");

      if (wifiManager.setWifiEnabled(true)) {
        Log.i(TAG, "Wi-fi enabled");
      } else {
        Log.e(TAG, "VERIFY_ERROR_ENABLE_WIFI");
        return false;
      }

      // This happens very quickly, but need to wait for it to enable. A little busy wait?
      int count = 0;

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
   * Format and return WiFi IPv4 Address
   * @return
   */
  private String[] getWiFiIPAddress() {
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    int ip = wifiInfo.getIpAddress();

    String ipString = formatIP(ip);
    String subnet = "";

    try {
        InetAddress inetAddress = InetAddress.getByName(ipString);
        subnet = getIPv4Subnet(inetAddress);
    } catch (Exception e) {
    }

    return new String[]{ipString, subnet};
  }

  /**
   * Get WiFi Router IP from DHCP
   * @return
   */
  private String getWiFiRouterIP() {
    DhcpInfo dhcp = wifiManager.getDhcpInfo();
    int ip = dhcp.gateway;
    return formatIP(ip);
  }

  /**
   * Format IPv4 Address
   * @param ip
   * @return
   */
  private String formatIP(int ip) {
    return String.format(
        "%d.%d.%d.%d",
        (ip & 0xff),
        (ip >> 8 & 0xff),
        (ip >> 16 & 0xff),
        (ip >> 24 & 0xff)
    );
  }

  /**
   * Get IPv4 Subnet
   * @param inetAddress
   * @return
   */
  public static String getIPv4Subnet(InetAddress inetAddress) {
    try {
      NetworkInterface ni = NetworkInterface.getByInetAddress(inetAddress);
      List<InterfaceAddress> intAddrs = ni.getInterfaceAddresses();
      for (InterfaceAddress ia : intAddrs) {
        if (!ia.getAddress().isLoopbackAddress() && ia.getAddress() instanceof Inet4Address) {
          return getIPv4SubnetFromNetPrefixLength(ia.getNetworkPrefixLength()).getHostAddress()
              .toString();
        }
      }
    } catch (Exception e) {
    }
    return "";
  }

  /**
   * Get Subnet from Prefix Length
   * @param netPrefixLength
   * @return
   */
  public static InetAddress getIPv4SubnetFromNetPrefixLength(int netPrefixLength) {
    try {
      int shift = (1 << 31);
      for (int i = netPrefixLength - 1; i > 0; i--) {
        shift = (shift >> 1);
      }
      String subnet =
          Integer.toString((shift >> 24) & 255) + "." + Integer.toString((shift >> 16) & 255) + "."
              + Integer.toString((shift >> 8) & 255) + "." + Integer.toString(shift & 255);
      return InetAddress.getByName(subnet);
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * Validate JSON data
   */
  private boolean validateData(JSONArray data) {
    try {
      if (data == null || data.get(0) == null) {
        callbackContext.error("DATA_IS_NULL");
        return false;
      }
      return true;
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
    }
    return false;
  }

  /**
   * Request ACCESS_FINE_LOCATION Permission
   * @param requestCode
   */
  protected void requestLocationPermission(int requestCode) {
    cordova.requestPermission(this, requestCode, ACCESS_FINE_LOCATION);
  }

  /**
   * Handle Android Permission Requests
   */
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
      throws JSONException {

    for (int r : grantResults) {
      if (r == PackageManager.PERMISSION_DENIED) {
        callbackContext.error( "PERMISSION_DENIED" );
        return;
      }
    }

    switch (requestCode) {
      case SCAN_RESULTS_CODE:
        getScanResults(callbackContext, passedData); // Call method again after permissions approved
        break;
      case SCAN_CODE:
        scan(callbackContext, passedData); // Call method again after permissions approved
        break;
      case LOCATION_REQUEST_CODE:
        callbackContext.success("PERMISSION_GRANTED");
        break;
      case WIFI_SERVICE_INFO_CODE:
        getWifiServiceInfo(callbackContext, bssidRequested);
        break;
    }
  }

  /**
   * Figure out what the highest priority network in the network list is and return that priority
   */
  private static int getMaxWifiPriority(final WifiManager wifiManager) {
    final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
    int maxPriority = 0;
    for (WifiConfiguration config : configurations) {
      if (config.priority > maxPriority) {
        maxPriority = config.priority;
      }
    }

    Log.d(TAG, "WifiWizard: Found max WiFi priority of "
        + maxPriority);

    return maxPriority;
  }

  /**
   * Check if device is connected to Internet
   */
  private boolean canConnectToInternet(CallbackContext callbackContext, boolean doPing) {

    try {

      if ( hasInternetConnection(doPing) ) {
        // Send success as 1 to return true from Promise (handled in JS)
        callbackContext.success("1");
        return true;
      } else {
        callbackContext.success("0");
        return false;
      }

    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }
  }

  /**
   * Check if we can conenct to router via HTTP connection
   * 
   * @param callbackContext
   * @param doPing
   * @return boolean
   */
  private boolean canConnectToRouter(CallbackContext callbackContext, boolean doPing) {

    try {

      if (hasConnectionToRouter(doPing)) {
        // Send success as 1 to return true from Promise (handled in JS)
        callbackContext.success("1");
        return true;
      } else {
        callbackContext.success("0");
        return false;
      }

    } catch (Exception e) {
      callbackContext.error(e.getMessage());
      Log.d(TAG, e.getMessage());
      return false;
    }
  }

  /**
   * Check if The Device Is Connected to Internet
   *
   * @return true if device connect to Internet or return false if not
   */
  public boolean hasInternetConnection(boolean doPing) {
    if (connectivityManager != null) {
      NetworkInfo info = connectivityManager.getActiveNetworkInfo();
      if (info != null) {
        if (info.isConnected()) {
          if( doPing ){
            return pingCmd("8.8.8.8");
          } else {
            return isHTTPreachable("http://www.google.com/");
          }
        }
      }
    }
    return false;
  }

  /**
   * Check for connection to router by pinging router IP
   * @return
   */
  public boolean hasConnectionToRouter( boolean doPing ) {

    String ip = getWiFiRouterIP();

    if ( ip == null || ip.equals("0.0.0.0") || connectivityManager == null) {

      return false;

    } else {

      NetworkInfo info = connectivityManager.getActiveNetworkInfo();

      if (info != null && info.isConnected()) {

        if( doPing ){
          return pingCmd(ip);
        } else {
          return isHTTPreachable("http://" + ip + "/");
        }
      } else {
        return false;
      }

    }

  }

  /**
   * Check if HTTP connection to URL is reachable
   * 
   * @param checkURL
   * @return boolean
   */
  public static boolean isHTTPreachable(String checkURL) {
    try {
      // make a URL to a known source
      URL url = new URL(checkURL);

      // open a connection to that source
      HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();

      // trying to retrieve data from the source. If there
      // is no connection, this line will fail
      Object objData = urlConnect.getContent();

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * Method to Ping IP Address
   *
   * @param addr IP address you want to ping it
   * @return true if the IP address is reachable
   */
  public boolean pingCmd(String addr) {

    try {

      String ping = "ping  -c 1 -W 3 " + addr;
      Runtime run = Runtime.getRuntime();
      Process pro = run.exec(ping);

      try {
        pro.waitFor();
      } catch (InterruptedException e) {
        Log.e(TAG, "InterruptedException error.", e);
      }

      int exit = pro.exitValue();

      Log.d(TAG, "pingCmd exitValue" + exit);

      if (exit == 0) {
        return true;
      } else {
        // ip address is not reachable
        return false;
      }
    } catch (UnknownHostException e) {
      Log.d(TAG, "UnknownHostException: " + e.getMessage());
    } catch (Exception e) {
      Log.d(TAG, e.getMessage());
    }

    return false;
  }

  /**
   * Network Changed Broadcast Receiver
   */
  private class NetworkChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

      if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {

        Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION");

        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        WifiInfo info = WifiWizard2.this.wifiManager.getConnectionInfo();

        // Checks that you're connected to the desired network
        if (networkInfo.isConnected() && info.getNetworkId() > -1) {

          final String ssid = info.getSSID().replaceAll("\"", "");
          final String bssid = info.getBSSID();

          Log.d(TAG, "Connected to '" + ssid + "' @ " + bssid);

          // Verify the desired network ID is what we actually connected to
          if ( desired != null && info.getNetworkId() == desired.apId ) {
            onSuccessfulConnection();
          } else {
            Log.e(TAG, "Could not connect to the desired ssid: " + ssid);
          }

        }

      }

    }

  }

  /**
   * Register Receiver for Network Changed to handle BindALL
   * @param netID
   */
  private void registerBindALL(int netID){

    // Bind all requests to WiFi network (only necessary for Lollipop+ - API 21+)
    if( API_VERSION > 21 ){
      Log.d(TAG, "registerBindALL: registering net changed receiver");
      desired = new AP(netID,null,null);
      cordova.getActivity().getApplicationContext().registerReceiver(networkChangedReceiver, NETWORK_STATE_CHANGED_FILTER);
    } else {
      Log.d(TAG, "registerBindALL: API older than 21, bindall ignored.");
    }
  }

  /**
   * Maybe reset bind all after disconnect/disable
   *
   * This method unregisters the network changed receiver, as well as setting null for
   * bindProcessToNetwork or setProcessDefaultNetwork to prevent future sockets from application
   * being routed through Wifi.
   */
  private void maybeResetBindALL(){

    Log.d(TAG, "maybeResetBindALL");

    // desired should have a value if receiver is registered
    if( desired != null ){

      if( API_VERSION > 21 ){

        try {
          // Unregister net changed receiver -- should only be registered in API versions > 21
          cordova.getActivity().getApplicationContext().unregisterReceiver(networkChangedReceiver);
        } catch (Exception e) {}

      }

      // Lollipop OS or newer
      if ( API_VERSION >= 23 ) {
        connectivityManager.bindProcessToNetwork(null);
      } else if( API_VERSION >= 21 && API_VERSION < 23 ){
        connectivityManager.setProcessDefaultNetwork(null);
      }

      if ( API_VERSION > 21 && networkCallback != null) {

        try {
          // Same behavior as releaseNetworkRequest
          connectivityManager.unregisterNetworkCallback(networkCallback); // Added in API 21
        } catch (Exception e) {}
      }

      networkCallback = null;
      previous = null;
      desired = null;

    }

  }

  /**
   * Will un-bind to network (use Cellular network)
   *
   * @param callbackContext A Cordova callback context
   */
  private void resetBindAll(CallbackContext callbackContext) {
    Log.d(TAG, "WifiWizard2: resetBindALL");

    try {
      maybeResetBindALL();
      callbackContext.success("Successfully reset BindALL");
    } catch (Exception e) {
      Log.e(TAG, "InterruptedException error.", e);
      callbackContext.error("ERROR_NO_BIND_ALL");
    }
  }

  /**
   * Will bind to network (use Wifi network)
   *
   * @param callbackContext A Cordova callback context
   */
  private void setBindAll(CallbackContext callbackContext) {
    Log.d(TAG, "WifiWizard2: setBindALL");

    try {
      int networkId = getConnectedNetId();
      registerBindALL(networkId);
      callbackContext.success("Successfully bindAll to network");
    } catch (Exception e) {
      Log.e(TAG, "InterruptedException error.", e);
      callbackContext.error("ERROR_CANT_BIND_ALL");
    }
  }


  /**
   * Called after successful connection to WiFi when using BindAll feature
   *
   * This method is called by the NetworkChangedReceiver after network changed action, and confirming that we are in fact connected to wifi,
   * and the wifi we're connected to, is the correct network set in enable, or connect.
   */
  private void onSuccessfulConnection() {
    // On Lollipop+ the OS routes network requests through mobile data
    // when phone is attached to a wifi that doesn't have Internet connection
    // We use the ConnectivityManager to force bind all requests from our process
    // to the wifi without internet
    // see https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html

    // Marshmallow OS or newer
    if ( API_VERSION >= 23 ) {

      Log.d(TAG, "BindALL onSuccessfulConnection API >= 23");

      // Marshmallow (API 23+) or newer uses bindProcessToNetwork
      final NetworkRequest request = new NetworkRequest.Builder()
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build();

      networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
          if( connectivityManager.bindProcessToNetwork(network) ){
            Log.d(TAG, "bindProcessToNetwork TRUE onSuccessfulConnection");
          } else {
            Log.d(TAG, "bindProcessToNetwork FALSE onSuccessfulConnection");
          }
        }
      };

      connectivityManager.requestNetwork(request, networkCallback);

      // Only lollipop (API 21 && 22) use setProcessDefaultNetwork, API < 21 already does this by default
    } else if( API_VERSION >= 21 && API_VERSION < 23 ){

      Log.d(TAG, "BindALL onSuccessfulConnection API >= 21 && < 23");

      // Lollipop (API 21-22) use setProcessDefaultNetwork (deprecated in API 23 - Marshmallow)
      final NetworkRequest request = new NetworkRequest.Builder()
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build();

      networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
          connectivityManager.setProcessDefaultNetwork(network);
        }
      };

      connectivityManager.requestNetwork(request, networkCallback);

    } else {
      // Technically we should never reach this with older API, but just in case
      Log.d(TAG, "BindALL onSuccessfulConnection API older than 21, no need to do any binding");
      networkCallback = null;
      previous = null;
      desired = null;

    }
  }

  /**
   * Class to store finished boolean in
   */
  private class ScanSyncContext {

    public boolean finished = false;
  }

  /**
   * Used for storing access point information
   */
  private static class AP {
    final String ssid, bssid;
    final int apId;

    AP(int apId, final String ssid, final String bssid) {
      this.apId = apId;
      this.ssid = ssid;
      this.bssid = bssid;
    }

  }
}
