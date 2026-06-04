package com.windmill.limo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.adbid.media.AdbidError;
import com.adbid.sdk.AdbidCustomController;
import com.adbid.sdk.AdbidInitConfig;
import com.adbid.sdk.AdbidLocation;
import com.adbid.sdk.AdbidSdk;
import com.adbid.sdk.AdbidSdkInitListener;
import com.windmill.sdk.WMAdConfig;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WMCustomController;
import com.windmill.sdk.WindMillAd;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.custom.WMCustomAdapterProxy;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class LimoCustomerProxy extends WMCustomAdapterProxy {

    private final String TAG = LimoCustomerProxy.this.getClass().getSimpleName();

    /**
     * @param serverExtra APP:CustomInfo:{"appId":"5001121"}
     */
    @Override public void initializeADN(final Context context, Map<String, Object> serverExtra) {
        try {
            String appCustomInfo = (String) serverExtra.get(WMConstants.CUSTOM_INFO);

            @SuppressWarnings("DataFlowIssue") JSONObject object = new JSONObject(appCustomInfo);

            String appId = object.optString("appId");

            Log.d(TAG, "initializeADN:" + appId);

            //开启debug日志，发布前关闭
            AdbidSdk.getInstance(context).setDebugMode(false);
            AdbidCustomController adbidCustomController = null;
            WMAdConfig wmAdConfig;
            if ((wmAdConfig = WindMillAd.sharedAds().getAdConfig()) != null &&
                    wmAdConfig.getCustomController() != null) {
                WMCustomController wmCustomController = wmAdConfig.getCustomController();
                adbidCustomController = new AdbidCustomController() {
                    @Override public boolean isCanUsePhoneState() {
                        return wmCustomController.isCanUsePhoneState();
                    }

                    @Override public boolean isSupportPersonalized() {
                        return false;
                    }

                    @Override public boolean isCanUseLocation() {
                        return wmCustomController.isCanUseLocation();
                    }

                    @Override public boolean isCanUseWifiState() {
                        return wmCustomController.isCanUseWifiState();
                    }

                    @Override public boolean isCanUseOaid() {
                        return TextUtils.isEmpty(wmCustomController.getDevOaid());
                    }

                    @Override public String getDevOaid() {
                        return wmCustomController.getDevOaid();
                    }

                    @Override public boolean isCanUseAppList() {
                        return wmCustomController.isCanUseAppList();
                    }

                    @Override public List<PackageInfo> getAppList() {
                        return wmCustomController.getInstalledPackages();
                    }

                    @Override public boolean isCanUseAndroidId() {
                        return wmCustomController.isCanUseAndroidId();
                    }

                    @Override public String getAndroidId() {
                        return wmCustomController.getAndroidId();
                    }

                    @Override public boolean isCanUseMacAddress() {
                        return false;
                    }

                    @Override public String getMacAddress() {
                        return wmCustomController.getMacAddress();
                    }

                    @Override public boolean isCanUseWriteExternal() {
                        return wmCustomController.isCanUseWriteExternal();
                    }

                    @Override public boolean isCanUseShakeAd() {
                        return false;
                    }

                    @Override public boolean isCanUseRecordAudio() {
                        return false;
                    }

                    @Override public String getDevImei() {
                        return wmCustomController.getDevImei();
                    }

                    @Override public String[] getDevImeiList() {
                        String imei = wmCustomController.getDevImei();
                        return new String[]{imei};
                    }

                    @Override public AdbidLocation getLocation() {
                        Location location = wmCustomController.getLocation();
                        if (location != null) {
                            return new AdbidLocation.Builder(location.getLatitude(),
                                    location.getLongitude()).build();
                        }
                        return null;
                    }

                    @Override public boolean isCanUseIP() {
                        return false;
                    }

                    @Override public String getIP() {
                        return "";
                    }
                };
            }
            //初始化配置
            AdbidInitConfig config = AdbidInitConfig
                    .builder(appId)
                    .addCustomController(adbidCustomController).build();

            //初始化
            AdbidSdk.getInstance(context).initialize(config, new AdbidSdkInitListener() {
                @Override public void onSdkInitCallback(boolean isSuccess, AdbidError adbidError) {
                    if (isSuccess) {
                        Log.d(TAG, "success");
                        callInitSuccess();
                    } else {
                        Log.d(TAG, "fail:" + adbidError.getCode() + ":" + adbidError.getMessage());
                        callInitFail(adbidError.getCode(), adbidError.getMessage());
                    }
                }
            });


        } catch (Throwable e) {
            e.printStackTrace();
            callInitFail(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(), e.getMessage());
        }
    }


    @Override public String getNetworkSdkVersion() {
        return AdbidSdk.VERSION;
    }

    @Override public int baseOnToBidCustomAdapterVersion() {
        return WMConstants.TO_BID_CUSTOM_ADAPTER_VERSION_2;
    }

    @Override public void notifyPrivacyStatusChange() {
        Log.d(TAG, "notifyPrivacyStatusChange");
    }
}
