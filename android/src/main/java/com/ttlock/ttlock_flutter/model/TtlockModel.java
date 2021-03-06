package com.ttlock.ttlock_flutter.model;

import com.ttlock.bl.sdk.constant.ControlAction;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TTLock on 2020/8/24.
 */
public class TtlockModel {

    public static final int CONTROL_ACTION_UNLOCK = 0;
    public static final int CONTROL_ACTION_LOCK = 1;

    //参数
    public String lockData;
    public int controlAction;
    public String passcode;
    public String passcodeOrigin;
    public String passcodeNew;
    public long startDate;
    public long endDate;

    // 0、null - 读取最近的操作记录,  1-读取锁内存中的全部操作记录
    public int logType;
    public String records;

    // 1-每周  2-每月
    public int passageModeType;

    public ArrayList<Integer> weekly;
    public ArrayList<Integer> monthly;



/*************      返回值     ****************/


    /**
     * //蓝牙状态
     */
    public int state;
    //蓝牙扫描状态 0-未开启扫描  1-扫描中
    public int scanState;

    public int specialValue;
    public Long lockTime;
    public int uniqueId;
    public int electricQuantity;
    public String lockName;
    public String lockMac;
    public boolean isInited;
    public boolean isAllowUnlock;
    public String lockVersion;
    // 0-锁住  1-解锁  2-未知 3-解锁（车位锁）
    public int lockSwitchState;
    public int rssi;
    public int oneMeterRssi;//todo:
    public Long timestamp;
    public boolean isOn;


    public String passcodeInfo;
//添加指纹时候 剩余手指按压次数
    public int currentCount;
//添加指纹时候 剩余手指按压次数
    public int totalCount;
//指纹码
    public String fingerprintNumber;
    public String cardNumber;
//    @property (nonatomic, strong) NSString *passageModes;

//最大可设置自动闭锁时间

    public int maxTime;
//最小可设置自动闭锁时间
    public int minTime;
//当前自动闭锁时间
    public int currentTime;

//管理员密码
    public String adminPasscode;
    public String erasePasscode;

    /**
     * ttlock switch config type
     */
    public int lockConfig;
    public int feature;
    public boolean isSupportFeature;

    public String cycleJsonList;

    public boolean isSupport;
    public int supportFunction;

    @Override
    public String toString() {
        return "TtlockModel{" +
                "lockData='" + lockData + '\'' +
                ", controlAction=" + controlAction +
                ", passcode='" + passcode + '\'' +
                ", passcodeOrigin='" + passcodeOrigin + '\'' +
                ", passcodeNew='" + passcodeNew + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", logType=" + logType +
                ", records='" + records + '\'' +
                ", passageModeType=" + passageModeType +
                ", state=" + state +
                ", scanState=" + scanState +
                ", specialValue=" + specialValue +
                ", lockTime=" + lockTime +
                ", uniqueId=" + uniqueId +
                ", electricQuantity=" + electricQuantity +
                ", lockName='" + lockName + '\'' +
                ", lockMac='" + lockMac + '\'' +
                ", isInited=" + isInited +
                ", isAllowUnlock=" + isAllowUnlock +
                ", lockVersion='" + lockVersion + '\'' +
                ", lockSwitchState=" + lockSwitchState +
                ", rssi=" + rssi +
                ", oneMeterRssi=" + oneMeterRssi +
                ", timestamp=" + timestamp +
                ", isOn=" + isOn +
                ", passcodeInfo='" + passcodeInfo + '\'' +
                ", currentCount=" + currentCount +
                ", totalCount=" + totalCount +
                ", fingerprintNumber=" + fingerprintNumber +
                ", cardNumber=" + cardNumber +
                ", maxTime=" + maxTime +
                ", minTime=" + minTime +
                ", currentTime=" + currentTime +
                ", adminPasscode='" + adminPasscode + '\'' +
                ", erasePasscode='" + erasePasscode + '\'' +
                '}';
    }

    public TtlockModel toObject(Map<String, Object> params) {
        Field[] fields = this.getClass().getDeclaredFields();
        try {
            for(int i = 0 ; i < fields.length ; i++) {
                //设置是否允许访问，不是修改原来的访问权限修饰词。
                fields[i].setAccessible(true);
                if (params.get(fields[i].getName()) != null) {
                    fields[i].set(this, params.get(fields[i].getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> hashMap = new HashMap<>();
        Field[] fields = this.getClass().getDeclaredFields();
        try {
            for(int i = 0 ; i < fields.length ; i++) {
                //设置是否允许访问，不是修改原来的访问权限修饰词。
                fields[i].setAccessible(true);
                hashMap.put(fields[i].getName(), fields[i].get(this));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hashMap;
    }

    public int getControlActionValue() {
        switch (controlAction) {
            case CONTROL_ACTION_UNLOCK:
                return ControlAction.UNLOCK;
            case CONTROL_ACTION_LOCK:
                return ControlAction.LOCK;
        }
        return ControlAction.UNLOCK;
    }

}
