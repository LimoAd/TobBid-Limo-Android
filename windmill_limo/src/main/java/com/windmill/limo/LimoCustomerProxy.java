package com.windmill.limo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.adbid.media.AdBidLossInfo;
import com.adbid.media.AdBidPlatform;
import com.adbid.media.AdbidError;
import com.adbid.media.ad.AdbidAppOpen;
import com.adbid.media.ad.AdbidBannerView;
import com.adbid.media.ad.AdbidInterstitial;
import com.adbid.media.ad.AdbidRewarded;
import com.adbid.sdk.AdbidCustomController;
import com.adbid.sdk.AdbidInitConfig;
import com.adbid.sdk.AdbidLocation;
import com.adbid.sdk.AdbidSdk;
import com.adbid.sdk.AdbidSdkConfiguration;
import com.adbid.sdk.AdbidSdkInitListener;
import com.windmill.sdk.WMAdConfig;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WMCustomController;
import com.windmill.sdk.WindMillAd;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.common.util.NumberUtils;
import com.windmill.sdk.custom.WMCustomAdapterProxy;
import com.windmill.sdk.models.WMBidResult;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimoCustomerProxy extends WMCustomAdapterProxy {

    private final String TAG = LimoCustomerProxy.this.getClass().getSimpleName();
    private WMCustomController controller;

    /**
     * @param serverExtra APP:CustomInfo:{"appId":"5001121"}
     */
    @Override public void initializeADN(Context context, Map<String, Object> serverExtra) {
        try {
            String appCustomInfo = (String) serverExtra.get(WMConstants.CUSTOM_INFO);

            @SuppressWarnings("DataFlowIssue") JSONObject object = new JSONObject(appCustomInfo);

            String appId = object.optString("appId");
            Log.d(TAG, "initializeADN appId:" + appId);

            if (AdbidSdkConfiguration.Instance.isInit()) {
                Log.d(TAG, "initializeADN: already initialized");
                callInitSuccess();
                return;
            }

            if (TextUtils.isEmpty(appId)) {
                TBError error = TBError.ERROR_ADN_INIT.copy();
                error.setMessage(error.getMessage() + "; 渠道AppId为空");
                callInitFail(error.getErrorCode(), error.getMessage());
                return;
            }

            //开启debug日志，发布前关闭
            AdbidSdk.getInstance(context).setDebugMode(WindMillAd.sharedAds().isDebugEnable());

            WMAdConfig wmAdConfig = WindMillAd.sharedAds().getAdConfig();
            if (wmAdConfig != null && wmAdConfig.getCustomController() != null) {
                controller = wmAdConfig.getCustomController();
            }

            //初始化配置
            AdbidInitConfig.Builder builder = AdbidInitConfig.builder(appId)
                    .setUserId(WindMillAd.sharedAds().getUserId())
                    .addCustomController(new AdbidCustomController() {
                        @Override public boolean isCanUsePhoneState() {
                            return false;
                        }

                        @Override public boolean isSupportPersonalized() {
                            return WindMillAd.sharedAds().isPersonalizedAdvertisingOn();
                        }

                        @Override public boolean isCanUseLocation() {
                            if (!WindMillAd.sharedAds().isPersonalizedAdvertisingOn()) {
                                return false;
                            }
                            return controller != null && controller.isCanUseLocation();
                        }

                        @Override public boolean isCanUseWifiState() {
                            return false;
                        }

                        @Override public boolean isCanUseOaid() {
                            return controller == null || controller.isCanUseOaid();
                        }

                        @Override public String getDevOaid() {
                            return controller != null ? controller.getDevOaid() : "";
                        }

                        @Override public boolean isCanUseAppList() {
                            if (!WindMillAd.sharedAds().isPersonalizedAdvertisingOn()) {
                                return false;
                            }
                            return controller != null && controller.isCanUseAppList();
                        }

                        @Override public List<PackageInfo> getAppList() {
                            if (!WindMillAd.sharedAds().isPersonalizedAdvertisingOn()) {
                                return new ArrayList<>();
                            }
                            return getPackages();
                        }

                        @Override public boolean isCanUseAndroidId() {
                            return controller != null && controller.isCanUseAndroidId();
                        }

                        @Override public String getAndroidId() {
                            return controller != null ? controller.getAndroidId() : "";
                        }

                        @Override public boolean isCanUseMacAddress() {
                            return false;
                        }

                        @Override public String getMacAddress() {
                            return "";
                        }

                        @Override public boolean isCanUseWriteExternal() {
                            return controller != null && controller.isCanUseWriteExternal();
                        }

                        @Override public boolean isCanUseShakeAd() {
                            return controller != null && controller.isCanUseSensor();
                        }

                        @Override public boolean isCanUseRecordAudio() {
                            return controller != null && controller.isCanUsePermissionRecordAudio();
                        }

                        @Override public String getDevImei() {
                            return "";
                        }

                        @Override public String[] getDevImeiList() {
                            return new String[0];
                        }

                        @Override public AdbidLocation getLocation() {
                            if (!WindMillAd.sharedAds().isPersonalizedAdvertisingOn()) {
                                return null;
                            }
                            Location location = controller != null ? controller.getLocation() : null;
                            if (location != null) {
                                return new AdbidLocation.Builder(location.getLatitude(),
                                        location.getLongitude())
                                        .accuracy(location.getAccuracy()).build();
                            }
                            return null;
                        }

                        @Override public boolean isCanUseIP() {
                            return false;
                        }

                        @Override public String getIP() {
                            return "";
                        }
                    });

            callInitBefore(builder);

            AdbidInitConfig config = builder.build();
            //初始化
            AdbidSdk.getInstance(context).initialize(config, new AdbidSdkInitListener() {
                @Override public void onSdkInitCallback(boolean isSuccess, AdbidError adbidError) {
                    try {
                        if (isSuccess) {
                            Log.d(TAG, "onSdkInitCallback: success");
                            callInitSuccess();
                        } else {
                            int code = adbidError != null ? adbidError.getCode() : -1;
                            String msg = adbidError != null ? adbidError.getMessage() : "unknown";
                            Log.d(TAG, "onSdkInitCallback: fail: " + code + ";" + msg);
                            TBError error = TBError.ERROR_ADN_INIT.copy();
                            error.setMessage(error.getMessage() + "; 渠道初始化报错: " + code + "; " + msg);
                            callInitFail(error.getErrorCode(), error.getMessage());
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.d(TAG, "onSdkInitCallback error: " + e.getMessage());
                    }
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();
            TBError error = TBError.ERROR_ADN_INIT.copy();
            error.setMessage(error.getMessage() + "; 渠道初始化崩溃异常");
            error.setInternalMsg(e.getMessage());
            callInitFail(error.getErrorCode(), error.getMessage());
        }
    }

    private List<PackageInfo> getPackages() {
        try {
            if (controller != null) {
                List<PackageInfo> installedPackages = controller.getInstalledPackages();
                if (installedPackages != null && !installedPackages.isEmpty()) {
                    List<PackageInfo> packages = new ArrayList<>();
                    for (PackageInfo packageInfo : installedPackages) {
                        if (packageInfo != null) {
                            packages.add(packageInfo);
                        }
                    }
                    return packages;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
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

    /**
     * 处理竞价结果：胜出通知 winNotice，失败通知 lossNotice
     */
    public static void handleBiddingResult(WMBidResult bidResult, Object adObj) {
        if (bidResult == null || adObj == null) {
            return;
        }
        try {
            double winEcpm = bidResult.winEcpm != null ? NumberUtils.parseDouble(bidResult.winEcpm) : 0.0;
            if (bidResult.isWin) {
                Log.d("LimoCustomerProxy", "notifyBiddingResult isWin: true ecpm: " + winEcpm);
                notifyWin(adObj, winEcpm);
            } else {
                Log.d("LimoCustomerProxy", "notifyBiddingResult isWin: false, AdBidPlatform.Other, winEcpm: " + winEcpm);
                notifyLoss(adObj, new AdBidLossInfo(AdBidPlatform.Other, winEcpm));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.d("LimoCustomerProxy", "handleBiddingResult: " + e.getMessage());
        }
    }

    private static void notifyWin(Object adObj, double ecpm) {
        if (adObj instanceof AdbidAppOpen) {
            ((AdbidAppOpen) adObj).winNotice(ecpm);
        } else if (adObj instanceof AdbidRewarded) {
            ((AdbidRewarded) adObj).winNotice(ecpm);
        } else if (adObj instanceof AdbidInterstitial) {
            ((AdbidInterstitial) adObj).winNotice(ecpm);
        } else if (adObj instanceof AdbidBannerView) {
            ((AdbidBannerView) adObj).winNotice(ecpm);
        }
    }

    private static void notifyLoss(Object adObj, AdBidLossInfo lossInfo) {
        if (adObj instanceof AdbidAppOpen) {
            ((AdbidAppOpen) adObj).lossNotice(lossInfo);
        } else if (adObj instanceof AdbidRewarded) {
            ((AdbidRewarded) adObj).lossNotice(lossInfo);
        } else if (adObj instanceof AdbidInterstitial) {
            ((AdbidInterstitial) adObj).lossNotice(lossInfo);
        } else if (adObj instanceof AdbidBannerView) {
            ((AdbidBannerView) adObj).lossNotice(lossInfo);
        }
    }
}
