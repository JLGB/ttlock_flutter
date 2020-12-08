#import "TtlockFlutterPlugin.h"
#import "TtlockPluginConfig.h"
#import <TTLock/TTLock.h>
#import <TTLock/TTGateway.h>

typedef NS_ENUM(NSInteger, ResultState) {
    ResultStateSuccess,
    ResultStateProgress,
    ResultStateFail,
};

@interface TtlockFlutterPlugin()

@property (nonatomic,strong) FlutterEventSink eventSink;

@end

@implementation TtlockFlutterPlugin


+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
 
    //1.初始化接收对象
    FlutterMethodChannel *channel = [FlutterMethodChannel methodChannelWithName:TTLOCK_CHANNEL_COMMAND binaryMessenger:[registrar messenger]];
    [registrar addMethodCallDelegate:[self sharedInstance] channel:channel];
    
    //2.初始化发送对象
    FlutterEventChannel *eventChannel = [FlutterEventChannel eventChannelWithName:TTLOCK_CHANNEL_LISTEN binaryMessenger:registrar.messenger];
    [eventChannel setStreamHandler:[self sharedInstance]];
}

+ (instancetype)sharedInstance{
    static TtlockFlutterPlugin *instance = nil;
    if (!instance) {
        instance = [[self alloc] init];
        [TTLock setupBluetooth:^(TTBluetoothState state) {
            if (state != TTBluetoothStatePoweredOn) {
                NSLog(@"####### Bluetooth is off or un unauthorized ########");
            }
        }];
    }
    return instance;
}

#pragma mark  - FlutterPlugin
- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result{
    __weak TtlockFlutterPlugin *weakSelf = self;
    NSString *command = call.method;
    NSObject *arguments = call.arguments;
    TtlockModel *lockModel = nil;
    if ([arguments isKindOfClass:NSDictionary.class]) {
        lockModel = [TtlockModel modelWithDict:(NSDictionary *)arguments];
    }else if ([arguments isKindOfClass:NSString.class]) {
        lockModel = [TtlockModel new];
        lockModel.lockData = (NSString *)arguments;
    }
    
    if (TTLock.bluetoothState != TTBluetoothStatePoweredOn) {
        NSLog(@"####### Bluetooth is off or un unauthorized ########");
    }
    
    if ([command isEqualToString:command_start_scan_lock]) {
        [TTLock startScan:^(TTScanModel *scanModel) {
            TtlockModel *data = [TtlockModel new];
            data.electricQuantity = @(scanModel.electricQuantity);
            data.lockName = scanModel.lockName;
            data.lockMac = scanModel.lockMac;
            data.isInited = @(scanModel.isInited);
            data.isAllowUnlock = @(scanModel.isAllowUnlock);
            data.lockVersion = scanModel.lockVersion;
            data.lockSwitchState = @(scanModel.lockSwitchState);
            data.rssi = @(scanModel.RSSI);
            data.oneMeterRssi = @(scanModel.oneMeterRSSI);
            
            data.timestamp = @(@(scanModel.date.timeIntervalSince1970 * 1000).longLongValue);
            data.lockSwitchState = @(scanModel.lockSwitchState);
            [weakSelf successCallbackCommand:command data: [data toDictionary]];
        }];
    }else if ([command isEqualToString:command_stop_scan_lock]) {
        [TTLock stopScan];
    }else if ([command isEqualToString:command_get_bluetooth_state]) {
        TTBluetoothState state = [TTLock bluetoothState];
        TtlockModel *data = [TtlockModel new];
        data.state = @(state);
        [self successCallbackCommand:command data:data];
    }else if ([command isEqualToString:command_get_blutetooth_scan_state]) {
        int scanState = [TTLock isScanning];
        TtlockModel *data = [TtlockModel new];
        data.scanState = @(scanState);
        [self successCallbackCommand:command data:data];
    }else if ([command isEqualToString:command_init_lock]) {
        NSDictionary *dict = (NSDictionary *)arguments;
     
        [TTLock initLockWithDict:dict success:^(NSString *lockData) {
            TtlockModel *data = [TtlockModel new];
            data.lockData = lockData;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_reset_lock]) {
        [TTLock resetLockWithLockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_control_lock]) {
        [TTLock controlLockWithControlAction:lockModel.controlAction.intValue + 1 lockData:lockModel.lockData success:^(long long lockTime, NSInteger electricQuantity, long long uniqueId) {
            TtlockModel *data = [TtlockModel new];
            data.lockTime = @(lockTime);
            data.electricQuantity = @(electricQuantity);
            data.uniqueId = @(uniqueId);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_reset_ekey]){
        [TTLock resetEkeyWithLockData:lockModel.lockData success:^(NSString *lockData) {
            TtlockModel *data = [TtlockModel new];
            data.lockData = lockData;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];

    }else if ([command isEqualToString:command_create_custom_passcode]) {
         
        [TTLock createCustomPasscode:lockModel.passcode startDate:lockModel.startDate.longLongValue endDate:lockModel.endDate.longLongValue lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_modify_passcode]) {
        [TTLock modifyPasscode:lockModel.passcodeOrigin
                   newPasscode:lockModel.passcodeNew
                     startDate:lockModel.startDate.longLongValue
                       endDate:lockModel.endDate.longLongValue
                      lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_delete_passcode]) {
        [TTLock deletePasscode:lockModel.passcode lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_reset_passcodes]) {
        [TTLock resetPasscodesWithLockData:lockModel.lockData success:^(NSString *lockData) {
            TtlockModel *data = [TtlockModel new];
            data.lockData = lockData;
            [weakSelf successCallbackCommand:command data: data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_add_ic_card]) {
        NSArray *cycleConfigArray = (NSArray *)[self objectFromJsonString:lockModel.cycleJsonList];
        if (cycleConfigArray.count) {
            [TTLock addICCardWithCyclicConfig:cycleConfigArray startDate:lockModel.startDate.longLongValue
                               endDate:lockModel.endDate.longLongValue
                              lockData:lockModel.lockData
                              progress:^(TTAddICState state) {
                TtlockModel *progressData = [TtlockModel new];
                [weakSelf progressCallbackCommand:command data:progressData];
            } success:^(NSString *cardNumber) {
                TtlockModel *successData = [TtlockModel new];
                successData.cardNumber = cardNumber;
                [weakSelf successCallbackCommand:command data:successData];
            } failure:^(TTError errorCode, NSString *errorMsg) {
                [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
            }];
        }else{
            [TTLock addICCardStartDate:lockModel.startDate.longLongValue
                               endDate:lockModel.endDate.longLongValue
                              lockData:lockModel.lockData
                              progress:^(TTAddICState state) {
                TtlockModel *progressData = [TtlockModel new];
                [weakSelf progressCallbackCommand:command data:progressData];
            } success:^(NSString *cardNumber) {
                TtlockModel *successData = [TtlockModel new];
                successData.cardNumber = cardNumber;
                [weakSelf successCallbackCommand:command data:successData];
            } failure:^(TTError errorCode, NSString *errorMsg) {
                [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
            }];
        }
    }else if ([command isEqualToString:command_modify_ic_card]) {
        NSArray *cycleConfigArray = (NSArray *)[self objectFromJsonString:lockModel.cycleJsonList];
        if (cycleConfigArray.count) {
            [TTLock modifyICCardValidityPeriodWithCyclicConfig:cycleConfigArray cardNumber:lockModel.cardNumber
                                                     startDate:lockModel.startDate.longLongValue
                                                     endDate:lockModel.endDate.longLongValue
                                                    lockData:lockModel.lockData success:^{
                [weakSelf successCallbackCommand:command data:nil];
            } failure:^(TTError errorCode, NSString *errorMsg) {
                [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
            }];
        }else{
            [TTLock modifyICCardValidityPeriodWithCardNumber:lockModel.cardNumber
                                                          startDate:lockModel.startDate.longLongValue
                                                            endDate:lockModel.endDate.longLongValue
                                                           lockData:lockModel.lockData success:^{
                       [weakSelf successCallbackCommand:command data:nil];
                   } failure:^(TTError errorCode, NSString *errorMsg) {
                       [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
                   }];
        }
    }else if ([command isEqualToString:command_delete_ic_card]) {
        [TTLock deleteICCardNumber:lockModel.cardNumber lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_clear_all_ic_card]) {
        [TTLock clearAllICCardsWithLockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_add_fingerprint]) {
        
        NSArray *cycleConfigArray = (NSArray *)[self objectFromJsonString:lockModel.cycleJsonList];
        if (cycleConfigArray.count) {
            [TTLock addFingerprintWithCyclicConfig:cycleConfigArray
                                         startDate:lockModel.startDate.longLongValue
                                           endDate:lockModel.endDate.longLongValue
                                          lockData:lockModel.lockData
                                          progress:^(int currentCount, int totalCount) {
                TtlockModel *progressData = [TtlockModel new];
                progressData.totalCount = @(totalCount);
                progressData.currentCount = @(currentCount);
                [weakSelf progressCallbackCommand:command data:progressData];
            } success:^(NSString *fingerprintNumber) {
                TtlockModel *successData = [TtlockModel new];
                successData.fingerprintNumber = fingerprintNumber;
                [weakSelf successCallbackCommand:command data:successData];
            } failure:^(TTError errorCode, NSString *errorMsg) {
                 [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
            }];
        }else{
            [TTLock addFingerprintStartDate:lockModel.startDate.longLongValue
                                    endDate:lockModel.endDate.longLongValue
                                   lockData:lockModel.lockData progress:^(int currentCount, int totalCount) {
                TtlockModel *progressData = [TtlockModel new];
                progressData.totalCount = @(totalCount);
                progressData.currentCount = @(currentCount);
                [weakSelf progressCallbackCommand:command data:progressData];
            } success:^(NSString *fingerprintNumber) {
                TtlockModel *successData = [TtlockModel new];
                successData.fingerprintNumber = fingerprintNumber;
                [weakSelf successCallbackCommand:command data:successData];
            } failure:^(TTError errorCode, NSString *errorMsg) {
                [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
            }];
        }
    }else if ([command isEqualToString:command_modify_fingerprint]) {
          NSArray *cycleConfigArray = (NSArray *)[self objectFromJsonString:lockModel.cycleJsonList];
          if (cycleConfigArray.count) {
              [TTLock modifyFingerprintValidityPeriodWithCyclicConfig:cycleConfigArray fingerprintNumber:lockModel.fingerprintNumber startDate:lockModel.startDate.longLongValue endDate:lockModel.endDate.longLongValue lockData:lockModel.lockData success:^{
                  [weakSelf successCallbackCommand:command data:nil];
              } failure:^(TTError errorCode, NSString *errorMsg) {
                  [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
              }];
          }else{
              [TTLock modifyFingerprintValidityPeriodWithFingerprintNumber:lockModel.fingerprintNumber startDate:lockModel.startDate.longLongValue endDate:lockModel.endDate.longLongValue lockData:lockModel.lockData success:^{
                  [weakSelf successCallbackCommand:command data:nil];
              } failure:^(TTError errorCode, NSString *errorMsg) {
                  [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
              }];
          }
    }else if ([command isEqualToString:command_delete_fingerprint]) {
        [TTLock deleteFingerprintNumber:lockModel.fingerprintNumber lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_clear_all_fingerprint]) {
        [TTLock clearAllFingerprintsWithLockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_modify_admin_passcode]) {
        [TTLock modifyAdminPasscode:lockModel.adminPasscode lockData:lockModel.lockData success:^() {
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_lock_time]) {
        [TTLock setLockTimeWithTimestamp:lockModel.timestamp.longLongValue lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_get_lock_time]) {
        [TTLock getLockTimeWithLockData:lockModel.lockData success:^(long long lockTimestamp) {
            TtlockModel *data = [TtlockModel new];
            data.lockTime = @(lockTimestamp);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_operate_record]) {
        int logType = lockModel.logType.intValue;
        TTOperateLogType type = logType == 1 ? TTOperateLogTypeLatest :TTOperateLogTypeAll;
        [TTLock getOperationLogWithType:type lockData:lockModel.lockData success:^(NSString *operateRecord) {
            TtlockModel *data = [TtlockModel new];
            data.records = operateRecord;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_power]) {
        [TTLock getElectricQuantityWithLockData:lockModel.lockData success:^(NSInteger electricQuantity) {
            TtlockModel *data = [TtlockModel new];
            data.electricQuantity = @(electricQuantity);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_version]) {
        NSString *lockMac = (NSString *)arguments;
        [TTLock getLockVersionWithWithLockMac:lockMac success:^(NSDictionary *lockVersion) {
            [weakSelf successCallbackCommand:command data:lockVersion];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_switch_state]) {
        [TTLock getLockSwitchStateWithLockData:lockModel.lockData success:^(TTLockSwitchState state) {
            TtlockModel *data = [TtlockModel new];
            data.lockSwitchState = @(state);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_automatic_locking_periodic_time]) {
        [TTLock getAutomaticLockingPeriodicTimeWithLockData:lockModel.lockData success:^(int currentTime, int minTime, int maxTime) {
            TtlockModel *data = [TtlockModel new];
            data.maxTime = @(maxTime);
            data.minTime = @(minTime);
            data.currentTime = @(currentTime);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_set_lock_automatic_locking_periodic_time]) {
        [TTLock setAutomaticLockingPeriodicTime:lockModel.currentTime.intValue lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_get_lock_remote_unlock_switch_state]) {
        [TTLock getRemoteUnlockSwitchWithLockData:lockModel.lockData success:^(BOOL isOn) {
            TtlockModel *data = [TtlockModel new];
            data.isOn = @(isOn) ;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
        
    }else if ([command isEqualToString:command_set_lock_remote_unlock_switch_state]) {
        BOOL switchOn = lockModel.isOn.boolValue;
        [TTLock setRemoteUnlockSwitchOn:switchOn lockData:lockModel.lockData success:^(NSString *lockData) {
            TtlockModel *data = [TtlockModel new];
            data.lockData = lockData;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_lock_config]) {
        TTLockConfigType lockConfigType = lockModel.lockConfig.intValue + 1;
        [TTLock getLockConfigWithType:lockConfigType lockData:lockModel.lockData success:^(TTLockConfigType type, BOOL isOn) {
            TtlockModel *data = [TtlockModel new];
            data.isOn = @(isOn);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];

        
    }else if ([command isEqualToString:command_set_lock_config]) {
        BOOL switchOn = lockModel.isOn.intValue;
        TTLockConfigType lockConfigType = lockModel.lockConfig.intValue + 1;
        [TTLock setLockConfigWithType:lockConfigType on:switchOn lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_all_passage_modes]) {
        [TTLock getPassageModesWithLockData:lockModel.lockData success:^(NSString *passageModes) {
            TtlockModel *data = [TtlockModel new];
            data.passageModes = passageModes;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_add_passage_mode]) {
        // startDate  endDate 分钟为单位
        TTPassageModeType type = lockModel.passageModeType.intValue + 1;
        NSArray *weekly = lockModel.weekly;
        NSArray *monthly = lockModel.monthly;
        int startDate = lockModel.startDate.intValue;
        int endDate = lockModel.endDate.intValue;
        NSString *lockData = lockModel.lockData;
        [TTLock configPassageModeWithType:type
                                   weekly:weekly
                                  monthly:monthly
                                startDate:startDate
                                  endDate:endDate
                                 lockData:lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_clear_all_passage_modes]) {
        [TTLock clearPassageModeWithLockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_start_scan_gateway]) {
        [TTGateway startScanGatewayWithBlock:^(TTGatewayScanModel *model) {
            NSMutableDictionary *dict = @{}.mutableCopy;
            dict[@"gatewayMac"] = model.gatewayMac;
            dict[@"gatewayName"] = model.gatewayName;
            dict[@"rssi"] = @(model.RSSI);
            dict[@"isDfuMode"] = @(model.isDfuMode);
            [weakSelf successCallbackCommand:command data:dict];
        }];
        
    }else if ([command isEqualToString:command_stop_scan_gateway]) {
        [TTGateway stopScanGateway];
        
    }else if ([command isEqualToString:command_get_surround_wifi]) {
        [TTGateway scanWiFiByGatewayWithBlock:^(BOOL isFinished, NSArray *WiFiArr, TTGatewayStatus status) {
            if (status == TTGatewaySuccess) {
                NSMutableArray *wifiList = @[].mutableCopy;
                for (NSDictionary *dict in WiFiArr) {
                    NSMutableDictionary *wifiDict = @{}.mutableCopy;
                    wifiDict[@"wifi"] = dict[@"SSID"];
                    wifiDict[@"rssi"] = dict[@"RSSI"];
                    [wifiList addObject:wifiDict];
                }
                
                NSMutableDictionary *dict = @{}.mutableCopy;
                dict[@"wifiList"] = wifiList;
                dict[@"finished"] = @(isFinished);
                [weakSelf successCallbackCommand:command data:dict];
            }else{
                NSInteger errorCode = [self getTTGatewayErrorCode:status];
                [weakSelf errorCallbackCommand:command code:errorCode details:nil];
            }
        }];
    }else if ([command isEqualToString:command_connect_gateway]) {
        [TTGateway connectGatewayWithGatewayMac:lockModel.mac block:^(TTGatewayConnectStatus connectStatus) {
            NSMutableDictionary *dict = @{}.mutableCopy;
            dict[@"status"] = @(connectStatus);
            [weakSelf successCallbackCommand:command data:dict];
        }];
    }else if ([command isEqualToString:command_disconnect_gateway]) {
        [TTGateway disconnectGatewayWithGatewayMac:lockModel.mac block:^(TTGatewayStatus status) {
            [weakSelf successCallbackCommand:command data:nil];
        }];
    }else if ([command isEqualToString:command_init_gateway]) {
        NSMutableDictionary *dict = @{}.mutableCopy;
        dict[@"SSID"] = lockModel.wifi;
        dict[@"wifiPwd"] = lockModel.wifiPassword;
        dict[@"gatewayName"] = lockModel.gatewayName;
        dict[@"uid"] = lockModel.ttlockUid;
        dict[@"userPwd"] = lockModel.ttlockLoginPassword;
        [TTGateway initializeGatewayWithInfoDic:dict block:^(TTSystemInfoModel *systemInfoModel, TTGatewayStatus status) {
             if (status == TTGatewaySuccess) {
                 NSMutableDictionary *resultDict = @{}.mutableCopy;
                 resultDict[@"modelNum"] = systemInfoModel.modelNum;
                 resultDict[@"hardwareRevision"] = systemInfoModel.hardwareRevision;
                 resultDict[@"firmwareRevision"] = systemInfoModel.firmwareRevision;
                 [weakSelf successCallbackCommand:command data:resultDict];
             }else{
                NSInteger errorCode = [self getTTGatewayErrorCode:status];
                [weakSelf errorCallbackCommand:command code:errorCode details:nil];
             }
        }];
    }else if ([command isEqualToString:command_function_support]) {
        NSInteger supportFunction = lockModel.supportFunction.integerValue;
        if (supportFunction > 28) {
            supportFunction += 4;
        }else if (supportFunction > 26) {
            supportFunction += 3;
        }else if (supportFunction > 16) {
            supportFunction += 2;
        }else if (supportFunction > 5) {
            supportFunction += 1;
        }
        
        bool isSupport = [TTUtil lockFeatureValue:lockModel.lockData suportFunction:supportFunction];
        TtlockModel *data = [TtlockModel new];
        data.isSupport = @(isSupport);
        [weakSelf successCallbackCommand:command data:data];
    }
    
    else if ([command isEqualToString:command_active_elevator_floors]) {
        [TTLock activateLiftFloors:lockModel.floors lockData:lockModel.lockData success:^(long long lockTime, NSInteger electricQuantity, long long uniqueId) {
            TtlockModel *data = [TtlockModel new];
            data.lockTime = @(lockTime);
            data.electricQuantity = @(electricQuantity);
            data.uniqueId = @(uniqueId);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_elevator_controlable_floors]) {
        [TTLock setLiftControlableFloors:lockModel.floors lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_elevator_work_mode]) {
        [TTLock setLiftWorkMode:lockModel.elevatorWorkActiveType.intValue lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_power_saver_work_mode]) {
//      lockModel.savePowerType
        
        //TODO
        
    }else if ([command isEqualToString:command_set_power_saver_controlable]) {
        
        //TODO
        
    }else if ([command isEqualToString:command_set_nb_awake_modes]) {
        
        [TTLock setNBAwakeModes:lockModel.nbAwakeModes lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_nb_awake_modes]) {
        [TTLock getNBAwakeModesWithLockData:lockModel.lockData success:^(NSArray<NSNumber *> *awakeModes) {
            TtlockModel *data = [TtlockModel new];
            data.nbAwakeModes = awakeModes;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_nb_awake_times]) {
        
        [TTLock setNBAwakeTimes:lockModel.nbAwakeTimeList lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_get_nb_awake_times]) {
        [TTLock getNBAwakeTimesWithLockData:lockModel.lockData success:^(NSArray<NSDictionary *> *awakeTimes) {
            TtlockModel *data = [TtlockModel new];
            data.nbAwakeTimeList = awakeTimes;
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_door_sensor_switch]) {
        [TTLock setDoorSensorLockingSwitchOn:lockModel.isOn lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_get_door_sensor_switch]) {
        [TTLock getDoorSensorLockingSwitchStateWithLockData:lockModel.lockData success:^(BOOL isOn) {
            TtlockModel *data = [TtlockModel new];
            data.isOn = @(isOn);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_get_door_sensor_state]) {
        [TTLock getDoorSensorStateWithLockData:lockModel.lockData success:^(BOOL isOn) {
            TtlockModel *data = [TtlockModel new];
            data.isOn = @(isOn);
            [weakSelf successCallbackCommand:command data:data];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }else if ([command isEqualToString:command_set_hotel_info]) {
        [TTLock setHotelDataWithHotelInfo:lockModel.hotelData buildingNumber:lockModel.building.intValue floorNumber:lockModel.floor.intValue lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
        
    }else if ([command isEqualToString:command_set_hotel_card_sector]) {
        [TTLock setHotelCardSector:lockModel.sector lockData:lockModel.lockData success:^{
            [weakSelf successCallbackCommand:command data:nil];
        } failure:^(TTError errorCode, NSString *errorMsg) {
            [weakSelf errorCallbackCommand:command code:errorCode details:errorMsg];
        }];
    }
    
}

- (void)successCallbackCommand:(NSString *)command data:(NSObject *)data {
    [self callbackCommand:command
      resultState: ResultStateSuccess
            data:data
       errorCode:nil
    errorMessage:nil];
}

- (void)progressCallbackCommand:(NSString *)command data:(NSObject *)data {
    [self callbackCommand:command
      resultState:ResultStateProgress
            data:data
       errorCode:nil
    errorMessage:nil];
}

- (void)errorCallbackCommand:(NSString *)command code:(NSInteger)code details:(NSString *)errorMessage {

    [self callbackCommand:command
               resultState:ResultStateFail
                     data:nil
                errorCode:@(code)
             errorMessage:errorMessage];
}

- (void)callbackCommand:(NSString *)command resultState:(ResultState)resultState data:(NSObject *)data errorCode:(NSNumber *)code errorMessage:(NSString *)errorMessage {
    NSMutableDictionary *resultDict = @{}.mutableCopy;
    resultDict[@"command"] = command;
    resultDict[@"errorMessage"] = errorMessage;
    resultDict[@"errorCode"] = [self getTTLockErrorCode:code];
    resultDict[@"resultState"] = @(resultState);
    if (data) {
        if ([data isKindOfClass:TtlockModel.class]) {
            resultDict[@"data"] = [((TtlockModel *)data) toDictionary];
        }else{
            resultDict[@"data"] = data;
        }
    }
    FlutterEventSink eventSink = [TtlockFlutterPlugin sharedInstance].eventSink;
    if (eventSink == nil) {
        NSLog(@"TTLock iOS native errro eventSink is nil");
    }else{
        eventSink(resultDict);
    }
    
}


#pragma mark  - FlutterStreamHandler
- (FlutterError *)onListenWithArguments:(id)arguments eventSink:(FlutterEventSink)eventSink{
    _eventSink = eventSink;
    return  nil;
}

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    return nil;
}



#pragma private
- (NSObject *)objectFromJsonString:(NSString *) jsonString{
    if (jsonString == nil) {
        return nil;
    }
    NSData *jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
    return [NSJSONSerialization JSONObjectWithData:jsonData
                                                               options:NSJSONReadingMutableContainers
                                                                 error:nil];
}


- (NSInteger)getTTGatewayErrorCode:(TTGatewayStatus) status{
//    NSLog(@"Native errorCode:%d",status);
    NSDictionary *codeMap = @{
        @(1):@0,
        @(3):@1,
        @(4):@2,
        @(-1):@3,
        @(-2):@4,
        @(-3):@5,
        @(-4):@6,
        @(-5):@7,
        @(-6):@8,
        @(-7):@9,
    };
    return [codeMap[@(status)] integerValue];
}


- (NSNumber *)getTTLockErrorCode:(NSNumber *) code{
//    NSLog(@"Native errorCode:%@",code);
    
    if (code == nil) {
        return code;
    }
    
    if (code.integerValue > TTErrorRecordNotExist) {
        return @(code.integerValue - 65);
    }else if(code.integerValue > TTErrorWrongDynamicCode){
        return @(code.integerValue - 1);
    }
    return code;
}

@end
