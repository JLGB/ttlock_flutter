package com.ttlock.ttlock_flutter.constant;

/**
 * Created by TTLock on 2020/8/21.
 */
public class Constant {
    public static final String METHOD_CHANNEL_NAME = "com.ttlock/command/ttlock";
    public static final String EVENT_CHANNEL_NAME = "com.ttlock/listen/ttlock";


    public static final String COMMAND_SETUP_PUGIN = "setupPlugin";
    public static final String COMMAND_START_SCAN_LOCK = "startScanLock";
    public static final String COMMAND_STOP_SCAN_LOCK = "stopScanLock";
    public static final String COMMAND_GET_BLUETOOTH_STATE = "getBluetoothState";
    public static final String COMMAND_GET_BLUETOOTH_SCAN_STATE =
            "getBluetoothScanState";
    public static final String COMMAND_INIT_LOCK = "initLock";
    public static final String COMMAND_RESET_LOCK = "resetLock";

    public static final String COMMAND_CONTROL_LOCK = "controlLock";
    public static final String COMMAND_RESET_EKEY = "resetEkey";
    public static final String COMMAND_CREATE_CUSTOM_PASSCODE = "createCustomPasscode";
    public static final String COMMAND_MODIFY_PASSCODE = "modifyPasscode";
    public static final String COMMAND_DELETE_PASSCODE = "deletePasscode";
    public static final String COMMAND_RESET_PASSCODE = "resetPasscodes";

    public static final String COMMAND_ADD_CARD = "addCard";
    public static final String COMMAND_MODIFY_CARD = "modifyIcCard";
    public static final String COMMAND_DELETE_CARD = "deleteIcCard";
    public static final String COMMAND_CLEAR_ALL_CARD = "clearAllIcCard";

    public static final String COMMAND_ADD_FINGERPRINT = "addFingerprint";
    public static final String COMMAND_MODIFY_FINGERPRINT = "modifyFingerprint";
    public static final String COMMAND_DELETE_FINGERPRINT = "deleteFingerprint";
    public static final String COMMAND_CLEAR_ALL_FINGERPRINT = "clearAllFingerprint";
    public static final String COMMAND_GET_ADMIN_PASSCODE = "getAdminPasscode";
    public static final String COMMAND_MODIFY_ADMIN_PASSCODE = "modifyAdminPasscode";
    public static final String COMMAND_SET_ADMIN_ERASE_PASSCODE =
            "setAdminErasePasscode";

    public static final String COMMAND_SET_LOCK_TIME = "setLockTime";
    public static final String COMMAND_GET_LOCK_TIME = "getLockTime";
    public static final String COMMAND_GET_LOCK_OPERATE_RECORD = "getLockOperateRecord";
    public static final String COMMAND_GET_LOCK_POWER = "getLockPower";
    public static final String COMMAND_GET_LOCK_SWITCH_STATE = "getLockSwitchState";

    public static final String COMMAND_GET_AUTOMATIC_LOCK_PERIODIC_TIME =
            "getLockAutomaticLockingPeriodicTime";
    public static final String COMMAND_SET_AUTOMATIC_LOCK_PERIODIC_TIME =
            "setLockAutomaticLockingPeriodicTime";

    public static final String COMMAND_GET_LOCK_REMOTE_UNLOCK_SWITCH_STATE =
            "getLockRemoteUnlockSwitchState";
    public static final String COMMAND_SET_LOCK_REMOTE_UNLOCK_SWITCH_STATE =
            "setLockRemoteUnlockSwitchState";

    public static final String COMMAND_GET_LOCK_AUDIO_SWITCH_STATE =
            "getLockAudioSwitchState";
    public static final String COMMAND_SET_LOCK_AUDIO_SWITCH_STATE =
            "setLockAudioSwitchState";

    public static final String COMMAND_ADD_PASSAGE_MODE = "addPassageMode";
    public static final String COMMAND_DELETE_PASSAGE_MODE = "deletePassageMode";
    public static final String COMMAND_CLEAR_ALL_PASSAGE_MODE = "clearAllPassageModes";

    public static final String COMMAND_SET_LOCK_CONFIG = "setLockConfig";
    public static final String COMMAND_GET_LOCK_CONFIG = "getLockConfig";

    public static final String COMMAND_SUPPORT_FEATURE = "functionSupport";
}
