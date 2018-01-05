#import "WifiWizard2.h"
#import <SystemConfiguration/CaptiveNetwork.h>
#import <NetworkExtension/NetworkExtension.h>  

@implementation WifiWizard2

- (id)fetchSSIDInfo {
    // see http://stackoverflow.com/a/5198968/907720
    NSArray *ifs = (__bridge_transfer NSArray *)CNCopySupportedInterfaces();
    NSLog(@"Supported interfaces: %@", ifs);
    NSDictionary *info;
    for (NSString *ifnam in ifs) {
        info = (__bridge_transfer NSDictionary *)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
        NSLog(@"%@ => %@", ifnam, info);
        if (info && [info count]) { break; }
    }
    return info;
}

- (void)iOSConnectNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;

	NSString * ssidString;
	NSString * passwordString;
	NSDictionary* options = [[NSDictionary alloc]init];

	options = [command argumentAtIndex:0];
	ssidString = [options objectForKey:@"Ssid"];
	passwordString = [options objectForKey:@"Password"];

	if (@available(iOS 11.0, *)) {
	    if (ssidString && [ssidString length]) {
			NEHotspotConfiguration *configuration = [[NEHotspotConfiguration
				alloc] initWithSSID:ssidString 
					passphrase:passwordString 
						isWEP:(BOOL)false];

			configuration.joinOnce = YES;
			[[NEHotspotConfigurationManager sharedManager] applyConfiguration:configuration completionHandler:nil];
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:ssidString];
		} else {
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"SSID Not provided"];
		}
	} else {
		pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"iOS 11+ not available"];
	}

    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)iOSDisconnectNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;

	NSString * ssidString;
	NSDictionary* options = [[NSDictionary alloc]init];

	options = [command argumentAtIndex:0];
	ssidString = [options objectForKey:@"Ssid"];

	if (@available(iOS 11.0, *)) {
	    if (ssidString && [ssidString length]) {
			[[NEHotspotConfigurationManager sharedManager] removeConfigurationForSSID:ssidString];
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:ssidString];
		} else {
			pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"SSID Not provided"];
		}
	} else {
		pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"iOS 11+ not available"];
	}

    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)getConnectedSSID:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    NSDictionary *r = [self fetchSSIDInfo];

    NSString *ssid = [r objectForKey:(id)kCNNetworkInfoKeySSID]; //@"SSID"

    if (ssid && [ssid length]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:ssid];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not available"];
    }

    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)getConnectedBSSID:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    NSDictionary *r = [self fetchSSIDInfo];
    
    NSString *bssid = [r objectForKey:(id)kCNNetworkInfoKeyBSSID]; //@"SSID"
    
    if (bssid && [bssid length]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:bssid];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not available"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

// Android functions

- (void)addNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)removeNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)androidConnectNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)androidDisconnectNetwork:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)listNetworks:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)getScanResults:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)startScan:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;

    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];

    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}

- (void)disconnect:(CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult = nil;
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Not supported"];
    
    [self.commandDelegate sendPluginResult:pluginResult
                                callbackId:command.callbackId];
}


@end
