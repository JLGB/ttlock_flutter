//
//  TtlockModel.h
//  Runner
//
//  Created by Jinbo Lu on 2020/7/24.
//

#import <Foundation/Foundation.h>
//#import <TTOnPremiseLock/TTLock.h>


@interface TtlockModel : NSObject
//参数
@property (nonatomic, strong) NSString * lockData;
@property (nonatomic, strong) NSString * mac;
@property (nonatomic, strong) NSNumber * controlAction;
@property (nonatomic, strong) NSString *passcode;
@property (nonatomic, strong) NSString *passcodeOrigin;
@property (nonatomic, strong) NSString *passcodeNew;
@property (nonatomic, strong) NSNumber *startDate;
@property (nonatomic, strong) NSNumber *endDate;

// 0、null - 读取最近的操作记录,  1-读取锁内存中的全部操作记录
@property (nonatomic, strong) NSNumber *logType;
@property (nonatomic, strong) NSString *records;

// 1-每周  2-每月
@property (nonatomic, strong) NSNumber *passageModeType;
@property (nonatomic, strong) NSArray<NSNumber *> *weekly;
@property (nonatomic, strong) NSArray<NSNumber *> *monthly;
@property (nonatomic, strong) NSString *cycleJsonList;
@property (nonatomic, strong) NSNumber *lockConfig;

@property (nonatomic, strong) NSString *wifi;
@property (nonatomic, strong) NSString *wifiPassword;
@property (nonatomic, strong) NSString *ttlockLoginPassword;
@property (nonatomic, strong) NSString *ttlockUid;
@property (nonatomic, strong) NSString *gatewayName;
@property (nonatomic, strong) NSNumber *supportFunction;
@property (nonatomic, strong) NSNumber *isSupport;



/*************      返回值     ****************/


//蓝牙状态
@property (nonatomic, strong) NSNumber *state;
//蓝牙扫描状态 0-未开启扫描  1-扫描中
@property (nonatomic, strong) NSNumber *scanState;

@property (nonatomic, strong) NSNumber *specialValue;
@property (nonatomic, strong) NSNumber *lockTime;
@property (nonatomic, strong) NSNumber * uniqueId;
@property (nonatomic, strong) NSNumber * electricQuantity;
@property (nonatomic, strong) NSString * lockName;
@property (nonatomic, strong) NSString * lockMac;
@property (nonatomic, strong) NSNumber * isInited;
@property (nonatomic, strong) NSNumber * isAllowUnlock;
@property (nonatomic, strong) NSString * lockVersion;
// 0-锁住  1-解锁  2-未知 3-解锁（车位锁）
@property (nonatomic, strong) NSNumber * lockSwitchState;
@property (nonatomic, strong) NSNumber * rssi;
@property (nonatomic, strong) NSNumber * oneMeterRssi;
@property (nonatomic, strong) NSNumber *timestamp;
@property (nonatomic, strong) NSNumber *isOn;



@property (nonatomic, strong) NSString *passcodeInfo;
//添加指纹时候 剩余手指按压次数
@property (nonatomic, strong) NSNumber *currentCount;
//添加指纹时候 剩余手指按压次数
@property (nonatomic, strong) NSNumber *totalCount;
//指纹码
@property (nonatomic, strong) NSString *fingerprintNumber;
@property (nonatomic, strong) NSString *cardNumber;
@property (nonatomic, strong) NSString *passageModes;

//最大可设置自动闭锁时间

@property (nonatomic, strong) NSNumber *maxTime;
//最小可设置自动闭锁时间
@property (nonatomic, strong) NSNumber *minTime;
//当前自动闭锁时间
@property (nonatomic, strong) NSNumber *currentTime;

//管理员密码
@property (nonatomic, strong) NSString *adminPasscode;
@property (nonatomic, strong) NSString *erasePasscode;

@property (nonatomic, strong) NSString *floors;

@property (nonatomic, strong) NSNumber *elevatorWorkActiveType;

@property (nonatomic, strong) NSNumber *savePowerType;

@property (nonatomic, strong) NSArray<NSNumber *> *nbAwakeModes;

@property (nonatomic, strong) NSArray<NSDictionary *> *nbAwakeTimeList;

@property (nonatomic, strong) NSString *hotelData;
@property (nonatomic, strong) NSNumber *building;
@property (nonatomic, strong) NSNumber *floor;

@property (nonatomic, strong) NSString *sector;



+ (TtlockModel *)modelWithDict:(NSDictionary *)dict;
- (NSDictionary *)toDictionary;
@end


