package com.ttlock.ttlock_flutter;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.reflect.TypeToken;
import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice;
import com.ttlock.bl.sdk.api.TTLockClient;
import com.ttlock.bl.sdk.callback.AddFingerprintCallback;
import com.ttlock.bl.sdk.callback.AddICCardCallback;
import com.ttlock.bl.sdk.callback.ClearAllICCardCallback;
import com.ttlock.bl.sdk.callback.ClearPassageModeCallback;
import com.ttlock.bl.sdk.callback.ControlLockCallback;
import com.ttlock.bl.sdk.callback.CreateCustomPasscodeCallback;
import com.ttlock.bl.sdk.callback.DeleteFingerprintCallback;
import com.ttlock.bl.sdk.callback.DeleteICCardCallback;
import com.ttlock.bl.sdk.callback.DeletePasscodeCallback;
import com.ttlock.bl.sdk.callback.GetAdminPasscodeCallback;
import com.ttlock.bl.sdk.callback.GetAutoLockingPeriodCallback;
import com.ttlock.bl.sdk.callback.GetBatteryLevelCallback;
import com.ttlock.bl.sdk.callback.GetLockConfigCallback;
import com.ttlock.bl.sdk.callback.GetLockStatusCallback;
import com.ttlock.bl.sdk.callback.GetLockTimeCallback;
import com.ttlock.bl.sdk.callback.GetOperationLogCallback;
import com.ttlock.bl.sdk.callback.GetRemoteUnlockStateCallback;
import com.ttlock.bl.sdk.callback.InitLockCallback;
import com.ttlock.bl.sdk.callback.ModifyAdminPasscodeCallback;
import com.ttlock.bl.sdk.callback.ModifyFingerprintPeriodCallback;
import com.ttlock.bl.sdk.callback.ModifyICCardPeriodCallback;
import com.ttlock.bl.sdk.callback.ModifyPasscodeCallback;
import com.ttlock.bl.sdk.callback.ResetKeyCallback;
import com.ttlock.bl.sdk.callback.ResetLockCallback;
import com.ttlock.bl.sdk.callback.ResetPasscodeCallback;
import com.ttlock.bl.sdk.callback.ScanLockCallback;
import com.ttlock.bl.sdk.callback.SetAutoLockingPeriodCallback;
import com.ttlock.bl.sdk.callback.SetLockConfigCallback;
import com.ttlock.bl.sdk.callback.SetLockTimeCallback;
import com.ttlock.bl.sdk.callback.SetPassageModeCallback;
import com.ttlock.bl.sdk.callback.SetRemoteUnlockSwitchCallback;
import com.ttlock.bl.sdk.constant.TTLockConfigType;
import com.ttlock.bl.sdk.entity.ControlLockResult;
import com.ttlock.bl.sdk.entity.CyclicConfig;
import com.ttlock.bl.sdk.entity.LockError;
import com.ttlock.bl.sdk.entity.LockVersion;
import com.ttlock.bl.sdk.entity.PassageModeConfig;
import com.ttlock.bl.sdk.entity.PassageModeType;
import com.ttlock.bl.sdk.entity.ValidityInfo;
import com.ttlock.bl.sdk.gateway.api.GatewayClient;
import com.ttlock.bl.sdk.gateway.callback.ConnectCallback;
import com.ttlock.bl.sdk.gateway.callback.InitGatewayCallback;
import com.ttlock.bl.sdk.gateway.callback.ScanGatewayCallback;
import com.ttlock.bl.sdk.gateway.callback.ScanWiFiByGatewayCallback;
import com.ttlock.bl.sdk.gateway.model.ConfigureGatewayInfo;
import com.ttlock.bl.sdk.gateway.model.DeviceInfo;
import com.ttlock.bl.sdk.gateway.model.GatewayError;
import com.ttlock.bl.sdk.gateway.model.WiFi;
import com.ttlock.bl.sdk.util.FeatureValueUtil;
import com.ttlock.bl.sdk.util.GsonUtil;
import com.ttlock.bl.sdk.util.LogUtil;
import com.ttlock.ttlock_flutter.constant.Constant;
import com.ttlock.ttlock_flutter.constant.GatewayCommand;
import com.ttlock.ttlock_flutter.constant.TTGatewayConnectStatus;
import com.ttlock.ttlock_flutter.model.GatewayErrorConverter;
import com.ttlock.ttlock_flutter.model.GatewayModel;
import com.ttlock.ttlock_flutter.model.TTBluetoothState;
import com.ttlock.ttlock_flutter.model.TTGatewayScanModel;
import com.ttlock.ttlock_flutter.model.TTLockConfigConverter;
import com.ttlock.ttlock_flutter.model.TTLockErrorConverter;
import com.ttlock.ttlock_flutter.model.TTLockFunction;
import com.ttlock.ttlock_flutter.model.TtlockModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** TtlockFlutterPlugin */
public class TtlockFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, EventChannel.StreamHandler {
  private static final int PERMISSIONS_REQUEST_CODE = 0;
  private MethodChannel channel;
  private EventChannel eventChannel;
  private static Activity activity;
  private EventChannel.EventSink events;
  private TtlockModel ttlockModel = new TtlockModel();
  private GatewayModel gatewayModel = new GatewayModel();

  //lock command que
  private Queue<String> commandQue = new ArrayDeque<>();


  public static final int ResultStateSuccess = 0;
  public static final int ResultStateProgress = 1;
  public static final int ResultStateFail = 2;

  private boolean isGatewayCommand;

  /**
   * time out: 15s
   */
  private static final long COMMAND_TIME_OUT = 30 * 1000;
  private Runnable commandTimeOutRunable = new Runnable() {
    @Override
    public void run() {
      //todo:disconnect the connect
      errorCallbackCommand(commandQue.poll(), LockError.Failed);
      clearCommand();
    }
  };

  private Handler handler = new Handler();

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), Constant.METHOD_CHANNEL_NAME);
    channel.setMethodCallHandler(this);
    eventChannel = new EventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), Constant.EVENT_CHANNEL_NAME);
    eventChannel.setStreamHandler(this);
    LogUtil.setDBG(true);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (GatewayCommand.isGatewayCommand(call.method)) {//gateway
      isGatewayCommand = true;
      gatewayCommand(call);
    } else {//door lock
      isGatewayCommand = false;
      doorLockCommand(call);
    }
  }

  public void doorLockCommand(MethodCall call) {
    Object arguments = call.arguments;
    if (arguments instanceof Map) {
      ttlockModel.toObject((Map<String, Object>) arguments);
    } else {
      ttlockModel.lockData = (String) arguments;
    }
    switch (call.method) {
      case Constant.COMMAND_SUPPORT_FEATURE:
        isSupportFeature();
        break;
      case Constant.COMMAND_SETUP_PUGIN:
        setupPlug();
        break;
      case Constant.COMMAND_START_SCAN_LOCK:
        if (initPermission()) {
          startScan();
        }
        break;
      case Constant.COMMAND_STOP_SCAN_LOCK:
        stopScan();
        break;
      default:
        commandQue.add(call.method);
        LogUtil.d("commandQue:" + commandQue.size());
        if (commandQue.size() == 1) {
          doNextCommandAction();
        }
        break;
    }
  }

  public void gatewayCommand(MethodCall call) {
    String command = call.method;
    gatewayModel.toObject((Map<String, Object>) call.arguments);
    switch (command) {
      case GatewayCommand.COMMAND_START_SCAN_GATEWAY:
        if (initPermission()) {
          startScanGateway();
        }
        break;
      case GatewayCommand.COMMAND_STOP_SCAN_GATEWAY:
        stopScanGateway();
        break;
      case GatewayCommand.COMMAND_CONNECT_GATEWAY:
        connectGateway(gatewayModel);
        break;
      case GatewayCommand.COMMAND_DISCONNECT_GATEWAY:

        break;
      case GatewayCommand.COMMAND_GET_SURROUND_WIFI:
        getSurroundWifi(gatewayModel);
        break;
      case GatewayCommand.COMMAND_INIT_GATEWAY:
        initGateway(gatewayModel);
        break;
    }
  }

  public void isSupportFeature() {
    boolean isSupport = FeatureValueUtil.isSupportFeature(ttlockModel.lockData, TTLockFunction.flutter2Native(ttlockModel.supportFunction));
    ttlockModel.isSupport = isSupport;
    LogUtil.d(TTLockFunction.flutter2Native(ttlockModel.supportFunction) + ":" + isSupport);
    successCallbackCommand(Constant.COMMAND_SUPPORT_FEATURE, ttlockModel.toMap());
  }

  public void startScanGateway() {
    GatewayClient.getDefault().prepareBTService(activity);
    GatewayClient.getDefault().startScanGateway(new ScanGatewayCallback() {
      @Override
      public void onScanGatewaySuccess(ExtendedBluetoothDevice device) {
        TTGatewayScanModel ttGatewayScanModel = new TTGatewayScanModel();
        ttGatewayScanModel.gatewayMac = device.getAddress();
        ttGatewayScanModel.gatewayName = device.getName();
        ttGatewayScanModel.isDfuMode = device.isDfuMode();
        ttGatewayScanModel.rssi = device.getRssi();
        successCallbackCommand(GatewayCommand.COMMAND_START_SCAN_GATEWAY, ttGatewayScanModel.toMap());
      }

      @Override
      public void onScanFailed(int errorCode) {

      }
    });
  }

  public void stopScanGateway() {
    GatewayClient.getDefault().stopScanGateway();
  }

  public void connectGateway(final GatewayModel gatewayModel) {
    final HashMap<String, Object> resultMap = new HashMap<>();
    GatewayClient.getDefault().connectGateway(gatewayModel.mac, new ConnectCallback() {
      @Override
      public void onConnectSuccess(ExtendedBluetoothDevice device) {
        resultMap.put("status", TTGatewayConnectStatus.success);
        successCallbackCommand(GatewayCommand.COMMAND_CONNECT_GATEWAY, resultMap);
      }

      @Override
      public void onDisconnected() {//todo:比ios少一个错误码
        callbackCommand(GatewayCommand.COMMAND_CONNECT_GATEWAY, ResultStateFail, gatewayModel.toMap(), TTGatewayConnectStatus.timeout, "disconnect gateway");
      }
    });
  }

  public void disconnectGateway() {//todo:

  }

  public void getSurroundWifi(final GatewayModel gatewayModel) {
    final HashMap<String, Object> resultMap = new HashMap<>();
    GatewayClient.getDefault().scanWiFiByGateway(gatewayModel.mac, new ScanWiFiByGatewayCallback() {
      @Override
      public void onScanWiFiByGateway(List<WiFi> wiFis) {
        List<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
        for (WiFi wiFi : wiFis) {
          HashMap<String, Object> wifiMap = new HashMap<>();
          wifiMap.put("wifi", wiFi.ssid);
          wifiMap.put("rssi", wiFi.rssi);
          mapList.add(wifiMap);
        }
        resultMap.put("finished", false);
        resultMap.put("wifiList", mapList);
        successCallbackCommand(GatewayCommand.COMMAND_GET_SURROUND_WIFI, resultMap);
      }

      @Override
      public void onScanWiFiByGatewaySuccess() {
        resultMap.put("finished", true);
        successCallbackCommand(GatewayCommand.COMMAND_GET_SURROUND_WIFI, resultMap);
      }

      @Override
      public void onFail(GatewayError error) {
        errorCallbackCommand(GatewayCommand.COMMAND_GET_SURROUND_WIFI, error);
      }
    });
  }

  public void initGateway(final GatewayModel gatewayModel) {
    ConfigureGatewayInfo configureGatewayInfo = new ConfigureGatewayInfo();
    configureGatewayInfo.plugName = gatewayModel.gatewayName;
    configureGatewayInfo.ssid = gatewayModel.wifi;
    configureGatewayInfo.wifiPwd = gatewayModel.wifiPassword;
    configureGatewayInfo.uid = gatewayModel.ttlockUid;
    configureGatewayInfo.userPwd = gatewayModel.ttlockLoginPassword;

//    configureGatewayInfo.uid = 8409;
//    configureGatewayInfo.userPwd = "123456";

    GatewayClient.getDefault().initGateway(configureGatewayInfo, new InitGatewayCallback() {
      @Override
      public void onInitGatewaySuccess(DeviceInfo deviceInfo) {
        HashMap<String, Object> gatewayInfoMap = new HashMap<>();
        gatewayInfoMap.put("modelNum", deviceInfo.modelNum);
        gatewayInfoMap.put("hardwareRevision", deviceInfo.hardwareRevision);
        gatewayInfoMap.put("firmwareRevision", deviceInfo.firmwareRevision);
        successCallbackCommand(GatewayCommand.COMMAND_INIT_GATEWAY, gatewayInfoMap);
      }

      @Override
      public void onFail(GatewayError error) {
        errorCallbackCommand(GatewayCommand.COMMAND_INIT_GATEWAY, error);
      }
    });
  }

  public void doNextCommandAction() {
    if (commandQue.size() == 0) {
      return;
    }
    commandTimeOutCheck();
    String command = commandQue.peek();
    switch (command) {
//      case Constant.COMMAND_SETUP_PUGIN:
//        setupPlug();
//        break;
//      case Constant.COMMAND_START_SCAN_LOCK:
//        commandQue.poll();
//        removeCommandTimeOutRunable();
//        if (initPermission()) {
//          startScan();
//        }
//        break;
//      case Constant.COMMAND_STOP_SCAN_LOCK:
//        stopScan();
//        break;
      case Constant.COMMAND_INIT_LOCK:
        initLock();
        break;
      case Constant.COMMAND_CONTROL_LOCK:
        controlLock();
        break;
      case Constant.COMMAND_RESET_LOCK:
        resetLock();
        break;
      case Constant.COMMAND_GET_LOCK_TIME:
        getLockTime();
        break;
      case Constant.COMMAND_SET_LOCK_TIME:
        setLockTime();
        break;
      case Constant.COMMAND_MODIFY_PASSCODE:
        modifyPasscode();
        break;
      case Constant.COMMAND_CREATE_CUSTOM_PASSCODE:
        setCustomPasscode();
        break;
      case Constant.COMMAND_RESET_PASSCODE:
        resetPasscodes();
        break;
      case Constant.COMMAND_GET_LOCK_OPERATE_RECORD:
        getOperationLog();
        break;
      case Constant.COMMAND_GET_LOCK_SWITCH_STATE:
        getSwitchStatus();
        break;
      case Constant.COMMAND_DELETE_PASSCODE:
        deletePasscode();
        break;
      case Constant.COMMAND_MODIFY_ADMIN_PASSCODE:
        setAdminPasscode();
        break;
      case Constant.COMMAND_GET_ADMIN_PASSCODE:
        getAdminPasscode();
        break;
      case Constant.COMMAND_ADD_CARD:
        addICCard();
        break;
      case Constant.COMMAND_MODIFY_CARD:
        modifyICCard();
        break;
      case Constant.COMMAND_DELETE_CARD:
        deleteICCard();
        break;
      case Constant.COMMAND_ADD_FINGERPRINT:
        addFingerPrint();
        break;
      case Constant.COMMAND_MODIFY_FINGERPRINT:
        modifyFingerPrint();
        break;
      case Constant.COMMAND_DELETE_FINGERPRINT:
        deleteFingerPrint();
        break;
      case Constant.COMMAND_CLEAR_ALL_CARD:
        clearICCard();
        break;
      case Constant.COMMAND_CLEAR_ALL_FINGERPRINT:
        clearFingerPrint();
        break;
      case Constant.COMMAND_GET_LOCK_POWER:
        getBattery();
        break;
      case Constant.COMMAND_ADD_PASSAGE_MODE:
        addPassageMode();
        break;
      case Constant.COMMAND_CLEAR_ALL_PASSAGE_MODE:
        clearPassageMode();
        break;
      case Constant.COMMAND_SET_AUTOMATIC_LOCK_PERIODIC_TIME:
        setAutoLockTime();
        break;
      case Constant.COMMAND_GET_AUTOMATIC_LOCK_PERIODIC_TIME:
        getAutoLockTime();
        break;
      case Constant.COMMAND_SET_LOCK_REMOTE_UNLOCK_SWITCH_STATE:
        setRemoteUnlockSwitch();
        break;
      case Constant.COMMAND_GET_LOCK_REMOTE_UNLOCK_SWITCH_STATE:
        getRemoteUnlockSwitch();
        break;
      case Constant.COMMAND_SET_LOCK_CONFIG:
        setLockConfig();
        break;
      case Constant.COMMAND_GET_LOCK_CONFIG:
        getLockConfig();
        break;
      case Constant.COMMAND_RESET_EKEY:
        resetEKey();
        break;
      case Constant.COMMAND_SUPPORT_FEATURE:
        featureSupport();
        break;
      default:
        errorCallbackCommand(commandQue.poll(), LockError.INVALID_COMMAND);
        clearCommand();
        LogUtil.d("unknown command:" + command);
        break;
    }
  }

  private void commandTimeOutCheck() {
    commandTimeOutCheck(COMMAND_TIME_OUT);
  }

  private void commandTimeOutCheck(long timeOut) {
    if (handler != null) {
      handler.postDelayed(commandTimeOutRunable, timeOut);
    }
  }

  private void removeCommandTimeOutRunable() {
    if (handler != null) {
      handler.removeCallbacks(commandTimeOutRunable);
    }
  }

  public void setupPlug() {
    TTLockClient.getDefault().prepareBTService(activity);
    //todo:
    ttlockModel.state = TTBluetoothState.turnOn.ordinal();
    commandQue.clear();
//    removeCommandTimeOutRunable();
    successCallbackCommand(Constant.COMMAND_SETUP_PUGIN, ttlockModel.toMap());

//    doNextCommandAction();
  }

  public void clearCommand() {
    if (commandQue != null) {
      commandQue.clear();
    }
  }

  public void startScan() {
    TTLockClient.getDefault().startScanLock(new ScanLockCallback() {
      @Override
      public void onScanLockSuccess(ExtendedBluetoothDevice extendedBluetoothDevice) {
        TtlockModel data = new TtlockModel();
        data.electricQuantity = extendedBluetoothDevice.getBatteryCapacity();
        data.lockName = extendedBluetoothDevice.getName();
        data.lockMac = extendedBluetoothDevice.getAddress();
        data.isInited = !extendedBluetoothDevice.isSettingMode();
        data.isAllowUnlock = extendedBluetoothDevice.isTouch();
        data.lockVersion = extendedBluetoothDevice.getLockVersionJson();
        data.rssi = extendedBluetoothDevice.getRssi();
        successCallbackCommand(Constant.COMMAND_START_SCAN_LOCK, data.toMap());
//              data.lockSwitchState = @(scanModel.lockSwitchState);
//              data.oneMeterRssi = @(scanModel.oneMeterRSSI);
      }

      @Override
      public void onFail(LockError lockError) {//todo:
        LogUtil.d("lockError:" + lockError);
      }
    });

//    doNextCommandAction();
  }

  public void stopScan() {
    TTLockClient.getDefault().stopScanLock();

//    commandQue.poll();
//    removeCommandTimeOutRunable();
//    doNextCommandAction();
  }

  public void initLock() {

    ExtendedBluetoothDevice extendedBluetoothDevice = new ExtendedBluetoothDevice();
    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(ttlockModel.lockMac);
    extendedBluetoothDevice.setDevice(device);
    extendedBluetoothDevice.setAddress(ttlockModel.lockMac);

    LockVersion lockVersion = GsonUtil.toObject(ttlockModel.lockVersion, LockVersion.class);
    extendedBluetoothDevice.setProtocolType(lockVersion.getProtocolType());
    extendedBluetoothDevice.setProtocolVersion(lockVersion.getProtocolVersion());
    extendedBluetoothDevice.setScene(lockVersion.getScene());

    extendedBluetoothDevice.setSettingMode(!ttlockModel.isInited);

    TTLockClient.getDefault().initLock(extendedBluetoothDevice, new InitLockCallback() {
      @Override
      public void onInitLockSuccess(String lockData) {
        removeCommandTimeOutRunable();
        ttlockModel.lockData = lockData;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void controlLock() {
    TTLockClient.getDefault().controlLock(ttlockModel.getControlActionValue(), ttlockModel.lockData, ttlockModel.lockMac, new ControlLockCallback(){

      @Override
      public void onControlLockSuccess(ControlLockResult controlLockResult) {
        removeCommandTimeOutRunable();
        ttlockModel.lockTime = controlLockResult.lockTime;
        ttlockModel.controlAction = controlLockResult.controlAction;
        ttlockModel.electricQuantity = controlLockResult.battery;
        ttlockModel.uniqueId = controlLockResult.uniqueid;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void resetLock() {
    TTLockClient.getDefault().resetLock(ttlockModel.lockData, ttlockModel.lockMac, new ResetLockCallback() {
      @Override
      public void onResetLockSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), null);
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getLockTime() {
    TTLockClient.getDefault().getLockTime(ttlockModel.lockData, ttlockModel.lockMac, new GetLockTimeCallback() {

      @Override
      public void onGetLockTimeSuccess(long lockTimestamp) {
        removeCommandTimeOutRunable();
        ttlockModel.lockTime = lockTimestamp;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setLockTime() {
    TTLockClient.getDefault().setLockTime(ttlockModel.lockTime, ttlockModel.lockData, ttlockModel.lockMac, new SetLockTimeCallback() {
      @Override
      public void onSetTimeSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), null);
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getOperationLog() {
    //long period operation
    removeCommandTimeOutRunable();

    commandTimeOutCheck(4 * COMMAND_TIME_OUT);

    TTLockClient.getDefault().getOperationLog(ttlockModel.logType, ttlockModel.lockData, ttlockModel.lockMac, new GetOperationLogCallback() {
      @Override
      public void onGetLogSuccess(String log) {
        removeCommandTimeOutRunable();
        ttlockModel.records = log;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getSwitchStatus() {
    TTLockClient.getDefault().getLockStatus(ttlockModel.lockData, ttlockModel.lockMac, new GetLockStatusCallback() {
      @Override
      public void onGetLockStatusSuccess(int status) {
        removeCommandTimeOutRunable();
        ttlockModel.lockSwitchState = status;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setAdminPasscode() {
    TTLockClient.getDefault().modifyAdminPasscode(ttlockModel.adminPasscode, ttlockModel.lockData, ttlockModel.lockMac, new ModifyAdminPasscodeCallback() {
      @Override
      public void onModifyAdminPasscodeSuccess(String passcode) {
        removeCommandTimeOutRunable();
        ttlockModel.adminPasscode = passcode;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getAdminPasscode() {
    TTLockClient.getDefault().getAdminPasscode(ttlockModel.lockData, ttlockModel.lockMac, new GetAdminPasscodeCallback() {
      @Override
      public void onGetAdminPasscodeSuccess(String passcode) {
        removeCommandTimeOutRunable();
        ttlockModel.adminPasscode = passcode;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setCustomPasscode() {
    TTLockClient.getDefault().createCustomPasscode(ttlockModel.passcode, ttlockModel.startDate, ttlockModel.endDate, ttlockModel.lockData, ttlockModel.lockMac, new CreateCustomPasscodeCallback() {
      @Override
      public void onCreateCustomPasscodeSuccess(String passcode) {
        removeCommandTimeOutRunable();
        ttlockModel.passcode = passcode;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void modifyPasscode() {
    TTLockClient.getDefault().modifyPasscode(ttlockModel.passcodeOrigin, ttlockModel.passcodeNew, ttlockModel.startDate, ttlockModel.endDate, ttlockModel.lockData, ttlockModel.lockMac, new ModifyPasscodeCallback() {
      @Override
      public void onModifyPasscodeSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void deletePasscode() {
    TTLockClient.getDefault().deletePasscode(ttlockModel.passcode, ttlockModel.lockData, ttlockModel.lockMac, new DeletePasscodeCallback() {
      @Override
      public void onDeletePasscodeSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }


  public void resetPasscodes() {
    TTLockClient.getDefault().resetPasscode(ttlockModel.lockData, ttlockModel.lockMac, new ResetPasscodeCallback() {
      @Override
      public void onResetPasscodeSuccess(String lockData) {
        removeCommandTimeOutRunable();
        ttlockModel.lockData = lockData;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void addICCard() {
    ValidityInfo validityInfo = new ValidityInfo();
    validityInfo.setStartDate(ttlockModel.startDate);
    validityInfo.setEndDate(ttlockModel.endDate);

    if (TextUtils.isEmpty(ttlockModel.cycleJsonList)) {
      validityInfo.setModeType(ValidityInfo.TIMED);
    } else {
      validityInfo.setModeType(ValidityInfo.CYCLIC);
      validityInfo.setCyclicConfigs(GsonUtil.toObject(ttlockModel.cycleJsonList, new TypeToken<List<CyclicConfig>>(){}));
      ttlockModel.cycleJsonList = null;//clear data
    }
    TTLockClient.getDefault().addICCard(validityInfo, ttlockModel.lockData, ttlockModel.lockMac, new AddICCardCallback() {
      @Override
      public void onEnterAddMode() {
        removeCommandTimeOutRunable();
        progressCallbackCommand(commandQue.peek(), ttlockModel.toMap());
      }

      @Override
      public void onAddICCardSuccess(long cardNum) {
        ttlockModel.cardNumber = String.valueOf(cardNum);
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void modifyICCard() {
    ValidityInfo validityInfo = new ValidityInfo();
    validityInfo.setStartDate(ttlockModel.startDate);
    validityInfo.setEndDate(ttlockModel.endDate);

    if (TextUtils.isEmpty(ttlockModel.cycleJsonList)) {
      validityInfo.setModeType(ValidityInfo.TIMED);
    } else {
      validityInfo.setModeType(ValidityInfo.CYCLIC);
      validityInfo.setCyclicConfigs(GsonUtil.toObject(ttlockModel.cycleJsonList, new TypeToken<List<CyclicConfig>>(){}));
      ttlockModel.cycleJsonList = null;//clear data
    }
    TTLockClient.getDefault().modifyICCardValidityPeriod(validityInfo, ttlockModel.cardNumber, ttlockModel.lockData, ttlockModel.lockMac, new ModifyICCardPeriodCallback() {
      @Override
      public void onModifyICCardPeriodSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void deleteICCard() {
    TTLockClient.getDefault().deleteICCard(ttlockModel.cardNumber, ttlockModel.lockData, ttlockModel.lockMac, new DeleteICCardCallback() {
      @Override
      public void onDeleteICCardSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void clearICCard() {
    TTLockClient.getDefault().clearAllICCard(ttlockModel.lockData, ttlockModel.lockMac, new ClearAllICCardCallback() {
      @Override
      public void onClearAllICCardSuccess() {
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void addFingerPrint() {
    ValidityInfo validityInfo = new ValidityInfo();
    validityInfo.setStartDate(ttlockModel.startDate);
    validityInfo.setEndDate(ttlockModel.endDate);

    if (TextUtils.isEmpty(ttlockModel.cycleJsonList)) {
      validityInfo.setModeType(ValidityInfo.TIMED);
    } else {
      validityInfo.setModeType(ValidityInfo.CYCLIC);
      validityInfo.setCyclicConfigs(GsonUtil.toObject(ttlockModel.cycleJsonList, new TypeToken<List<CyclicConfig>>(){}));
      ttlockModel.cycleJsonList = null;//clear data
    }
    TTLockClient.getDefault().addFingerprint(validityInfo, ttlockModel.lockData, ttlockModel.lockMac, new AddFingerprintCallback() {
      @Override
      public void onEnterAddMode(int totalCount) {
        removeCommandTimeOutRunable();
        ttlockModel.totalCount = totalCount;
      }

      @Override
      public void onCollectFingerprint(int currentCount) {
        ttlockModel.currentCount = currentCount;
        progressCallbackCommand(commandQue.peek(), ttlockModel.toMap());
      }

      @Override
      public void onAddFingerpintFinished(long fingerprintNum) {
        ttlockModel.fingerprintNumber = String.valueOf(fingerprintNum);
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void modifyFingerPrint() {
    ValidityInfo validityInfo = new ValidityInfo();
    validityInfo.setStartDate(ttlockModel.startDate);
    validityInfo.setEndDate(ttlockModel.endDate);

    if (TextUtils.isEmpty(ttlockModel.cycleJsonList)) {
      validityInfo.setModeType(ValidityInfo.TIMED);
    } else {
      validityInfo.setModeType(ValidityInfo.CYCLIC);
      validityInfo.setCyclicConfigs(GsonUtil.toObject(ttlockModel.cycleJsonList, new TypeToken<List<CyclicConfig>>(){}));
      ttlockModel.cycleJsonList = null;//clear data
    }
    TTLockClient.getDefault().modifyFingerprintValidityPeriod(validityInfo, ttlockModel.fingerprintNumber, ttlockModel.lockData, ttlockModel.lockMac, new ModifyFingerprintPeriodCallback() {
      @Override
      public void onModifyPeriodSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void deleteFingerPrint() {
    TTLockClient.getDefault().deleteFingerprint(ttlockModel.fingerprintNumber, ttlockModel.lockData, ttlockModel.lockMac, new DeleteFingerprintCallback() {
      @Override
      public void onDeleteFingerprintSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void clearFingerPrint() {
    TTLockClient.getDefault().clearAllICCard(ttlockModel.lockData, ttlockModel.lockMac, new ClearAllICCardCallback() {
      @Override
      public void onClearAllICCardSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getBattery() {
    TTLockClient.getDefault().getBatteryLevel(ttlockModel.lockData, ttlockModel.lockMac, new GetBatteryLevelCallback() {
      @Override
      public void onGetBatteryLevelSuccess(int electricQuantity) {
        removeCommandTimeOutRunable();
        ttlockModel.electricQuantity = electricQuantity;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setAutoLockTime() {
    TTLockClient.getDefault().setAutomaticLockingPeriod(ttlockModel.currentTime, ttlockModel.lockData, ttlockModel.lockMac, new SetAutoLockingPeriodCallback() {
      @Override
      public void onSetAutoLockingPeriodSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getAutoLockTime() {
    TTLockClient.getDefault().getAutomaticLockingPeriod(ttlockModel.lockData, new GetAutoLockingPeriodCallback() {
      @Override
      public void onGetAutoLockingPeriodSuccess(int currtentTime, int minTime, int maxTime) {
        ttlockModel.currentTime = currtentTime;
        ttlockModel.minTime = minTime;
        ttlockModel.maxTime = maxTime;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setRemoteUnlockSwitch() {
    TTLockClient.getDefault().setRemoteUnlockSwitchState(ttlockModel.isOn, ttlockModel.lockData, ttlockModel.lockMac, new SetRemoteUnlockSwitchCallback() {
      @Override
      public void onSetRemoteUnlockSwitchSuccess(String lockData) {
        removeCommandTimeOutRunable();
        ttlockModel.lockData = lockData;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getRemoteUnlockSwitch() {
    TTLockClient.getDefault().getRemoteUnlockSwitchState(ttlockModel.lockData, ttlockModel.lockMac, new GetRemoteUnlockStateCallback() {
      @Override
      public void onGetRemoteUnlockSwitchStateSuccess(boolean enabled) {
        removeCommandTimeOutRunable();
        ttlockModel.isOn = enabled;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void addPassageMode() {
    PassageModeConfig passageModeConfig = new PassageModeConfig();
    PassageModeType passageModeType = PassageModeType.class.getEnumConstants()[ttlockModel.passageModeType];
    passageModeConfig.setModeType(passageModeType);
    String repeatJson = GsonUtil.toJson(ttlockModel.weekly);
    if (passageModeType == PassageModeType.Monthly) {
      repeatJson = GsonUtil.toJson(ttlockModel.monthly);
    }
    passageModeConfig.setRepeatWeekOrDays(repeatJson);
    //todo:
//    passageModeConfig.setStartDate(ttlockModel.startDate);
//    passageModeConfig.setEndDate(ttlockModel.endDate);

    TTLockClient.getDefault().setPassageMode(passageModeConfig, ttlockModel.lockData, ttlockModel.lockMac, new SetPassageModeCallback() {
      @Override
      public void onSetPassageModeSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void clearPassageMode() {
    TTLockClient.getDefault().clearPassageMode(ttlockModel.lockData, ttlockModel.lockMac, new ClearPassageModeCallback() {
      @Override
      public void onClearPassageModeSuccess() {
        removeCommandTimeOutRunable();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void setLockConfig() {
    TTLockConfigType ttLockConfigType = TTLockConfigConverter.flutter2Native(ttlockModel.lockConfig);

    if (ttLockConfigType == null) {
      removeCommandTimeOutRunable();
      errorCallbackCommand(commandQue.poll(), LockError.DATA_FORMAT_ERROR);
      clearCommand();
      return;
    }

    LogUtil.d("ttLockConfigType:" + ttLockConfigType);
    TTLockClient.getDefault().setLockConfig(ttLockConfigType, ttlockModel.isOn, ttlockModel.lockData, new SetLockConfigCallback() {
      @Override
      public void onSetLockConfigSuccess(TTLockConfigType ttLockConfigType) {
        removeCommandTimeOutRunable();
        ttlockModel.lockConfig = ttLockConfigType.ordinal();
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void getLockConfig() {
    TTLockConfigType ttLockConfigType = TTLockConfigConverter.flutter2Native(ttlockModel.lockConfig);

    if (ttLockConfigType == null) {
      removeCommandTimeOutRunable();
      errorCallbackCommand(commandQue.poll(), LockError.DATA_FORMAT_ERROR);
      clearCommand();
      return;
    }
    LogUtil.d("ttLockConfigType:" + ttLockConfigType);
    TTLockClient.getDefault().getLockConfig(ttLockConfigType, ttlockModel.lockData, new GetLockConfigCallback() {
      @Override
      public void onGetLockConfigSuccess(TTLockConfigType ttLockConfigType, boolean switchOn) {
        removeCommandTimeOutRunable();
        ttlockModel.lockConfig = ttLockConfigType.ordinal();
        ttlockModel.isOn = switchOn;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void resetEKey() {
    TTLockClient.getDefault().resetEkey(ttlockModel.lockData, ttlockModel.lockMac, new ResetKeyCallback() {
      @Override
      public void onResetKeySuccess(String lockData) {
        removeCommandTimeOutRunable();
        ttlockModel.lockData = lockData;
        successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
        doNextCommandAction();
      }

      @Override
      public void onFail(LockError error) {
        removeCommandTimeOutRunable();
        errorCallbackCommand(commandQue.poll(), error);
        clearCommand();
      }
    });
  }

  public void featureSupport() {
    boolean isSupport = FeatureValueUtil.isSupportFeature(ttlockModel.lockData, ttlockModel.feature);
    ttlockModel.isSupportFeature = isSupport;
    successCallbackCommand(commandQue.poll(), ttlockModel.toMap());
    removeCommandTimeOutRunable();
    doNextCommandAction();
  }

  /**
   * android 6.0
   */
  private boolean initPermission() {
    LogUtil.d("");
    String permissions[] = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    ArrayList<String> toApplyList = new ArrayList<String>();

    for (String perm : permissions) {
      if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity, perm)) {
        toApplyList.add(perm);
      }
    }
    String tmpList[] = new String[toApplyList.size()];
    if (!toApplyList.isEmpty()) {
      ActivityCompat.requestPermissions(activity, toApplyList.toArray(tmpList), PERMISSIONS_REQUEST_CODE);
      return false;
    }
    return true;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case PERMISSIONS_REQUEST_CODE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0) {
          for (int i=0;i<permissions.length;i++) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
              // permission was granted, yay! Do the
              // contacts-related task you need to do.
              if (isGatewayCommand) {
                startScanGateway();
              } else {
                startScan();
              }
            } else {
              // permission denied, boo! Disable the
              // functionality that depends on this permission.
            }
          }
        }
        return;
      }
      // other 'case' lines to check for other
      // permissions this app might request.
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }


  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    this.events = events;
  }

  @Override
  public void onCancel(Object arguments) {
    //todo:
  }

  public void successCallbackCommand(String command, Map data) {
    callbackCommand(command, ResultStateSuccess, data,-1, "");
  }

  public void progressCallbackCommand(String command, Map data) {
    callbackCommand(command, ResultStateProgress, data,-1, "");
  }

  public void errorCallbackCommand(String command, GatewayError gatewayError) {
    callbackCommand(command, ResultStateFail, null, GatewayErrorConverter.native2Flutter(gatewayError), gatewayError.getDescription());
  }

  public void errorCallbackCommand(String command, LockError lockError) {
    callbackCommand(command, ResultStateFail, null, TTLockErrorConverter.native2Flutter(lockError), lockError.getErrorMsg());
  }

  public void callbackCommand(final String command, final int resultState, final Map data, final int errorCode, final String errorMessage) {

    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("command", command);
        resultMap.put("errorMessage", errorMessage);
        resultMap.put("errorCode", errorCode);
        resultMap.put("resultState", resultState);
        resultMap.put("data", data);

        events.success(resultMap);
      }
    });
  }

}
