package com.windmill.limo;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidListener;
import com.adbid.media.ad.AdbidInterstitial;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomInterstitialAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.models.WMBidResult;

import java.util.HashMap;
import java.util.Map;


public class LimoCustomerInterstitial extends WMCustomInterstitialAdapter {

    private final String TAG = LimoCustomerInterstitial.this.getClass().getSimpleName();
    private AdbidInterstitial interstitialAd;
    private String placementId;
    private double cachedEcpm;

    @Override public Object getChannelObject() {
        return interstitialAd;
    }

    /**
     * @param serverExtra Placement:CustomInfo:{"codeId":"901121375"}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            placementId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);
            Log.d(TAG, "loadAd:" + placementId);

            if (TextUtils.isEmpty(placementId)) {
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(141);
                error.setMessage(error.getMessage() + ": 传入的渠道代码位等参数为空");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }

            interstitialAd = new AdbidInterstitial(placementId);
            interstitialAd.setAdListener(new AdbidListener() {
                @Override public void onAdLoad(AdbidAdInfo adInfo) {
                    try {
                        if (adInfo == null) {
                            TBError error = TBError.ERROR_ADN_LOAD.copy();
                            error.setInternalCode(143);
                            error.setMessage(error.getMessage() + ": 加载回调的参数广告对象/列表为空");
                            callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                                    error.getMessage()));
                            return;
                        }
                        //获取广告价格，单位分
                        cachedEcpm = adInfo.getPrice();
                        Log.d(TAG, "onAdLoad");
                        if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                            callLoadBiddingSuccess(new BidPrice(String.valueOf(cachedEcpm)));
                        }
                        callLoadSuccess();
                    } catch (Throwable e) {
                        Log.d(TAG, "onAdLoad error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override public void onAdLoadFail(String adUnitId, AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onAdLoadFail:" + code + ";" + msg);
                        callLoadFail(new WMAdapterError(code, String.valueOf(code), msg));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdDisplayed(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdShow");
                        callVideoAdShow();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdDisplayedFailed(AdbidAdInfo adInfo, AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onAdDisplayedFailed:" + code + ";" + msg);
                        TBError error1 = TBError.ERROR_ADN_SHOW.copy();
                        error1.setMessage(error1.getMessage() + "; 渠道展示错误: " + code + "; " + msg);
                        callVideoAdPlayError(new WMAdapterError(error1.getErrorCode(), String.valueOf(code),
                                error1.getMessage()));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdHidden(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdClose");
                        callVideoAdClosed();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdClicked(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdVideoBarClick");
                        callVideoAdClick();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            //加载广告
            interstitialAd.loadAd();
        } catch (Throwable e) {
            Log.d(TAG, "loadAd: " + e.getMessage());
            e.printStackTrace();
            TBError error = TBError.ERROR_ADN_LOAD.copy();
            error.setInternalCode(142);
            error.setMessage(error.getMessage() + ": 调用三方 load 方法异常崩溃: " + e.getMessage());
            callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                    error.getMessage()));
        }
    }

    @Override public void showAd(Activity activity, HashMap<String, String> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            Log.d(TAG, "showAd: " + placementId);
            if (interstitialAd != null && isReady()) {
                interstitialAd.showAd();
            } else {
                TBError error = TBError.ERROR_ADN_NOT_READY.copy();
                callVideoAdPlayError(new WMAdapterError(error.getErrorCode(), error.getMessage()));
            }
        } catch (Throwable e) {
            Log.d(TAG, "showAd: " + e.getMessage());
            e.printStackTrace();
            TBError error = TBError.ERROR_ADN_SHOW.copy();
            error.setInternalCode(163);
            error.setMessage(error.getMessage() + "; 渠道展示崩溃异常");
            error.setInternalMsg(e.getMessage());
            callVideoAdPlayError(new WMAdapterError(error.getErrorCode(),
                    String.valueOf(error.getInternalCode()), error.getMessage()));
        }
    }

    @Override public boolean isReady() {
        return interstitialAd != null && interstitialAd.isReady();
    }

    @Override public void notifyBiddingResult(WMBidResult bidResult) {
        LimoCustomerProxy.handleBiddingResult(bidResult, interstitialAd);
    }

    @Override public void destroyAd() {
        super.destroyAd();
        if (interstitialAd != null) {
            interstitialAd.destroy();
            interstitialAd = null;
        }
    }
}
