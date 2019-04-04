#import <Cordova/CDV.h>

@interface WifiWizard2 : CDVPlugin

// Main functions
- (void)iOSConnectNetwork:(CDVInvokedUrlCommand *)command;
- (void)iOSConnectOpenNetwork:(CDVInvokedUrlCommand *)command;
- (void)iOSDisconnectNetwork:(CDVInvokedUrlCommand *)command;
- (void)getConnectedSSID:(CDVInvokedUrlCommand *)command;
- (void)getConnectedBSSID:(CDVInvokedUrlCommand *)command;
- (void)isWifiEnabled:(CDVInvokedUrlCommand *)command;
- (void)setWifiEnabled:(CDVInvokedUrlCommand *)command;
- (void)scan:(CDVInvokedUrlCommand *)command;

// Android Functions
- (void)addNetwork:(CDVInvokedUrlCommand *)command;
- (void)removeNetwork:(CDVInvokedUrlCommand *)command;
- (void)androidConnectNetwork:(CDVInvokedUrlCommand *)command;
- (void)androidDisconnectNetwork:(CDVInvokedUrlCommand *)command;
- (void)listNetworks:(CDVInvokedUrlCommand *)command;
- (void)getScanResults:(CDVInvokedUrlCommand *)command;
- (void)startScan:(CDVInvokedUrlCommand *)command;
- (void)disconnect:(CDVInvokedUrlCommand *)command;
- (void)isConnectedToInternet:(CDVInvokedUrlCommand *)command;
- (void)canConnectToInternet:(CDVInvokedUrlCommand *)command;
- (void)canPingWifiRouter:(CDVInvokedUrlCommand *)command;
- (void)canConnectToRouter:(CDVInvokedUrlCommand *)command;

@end
