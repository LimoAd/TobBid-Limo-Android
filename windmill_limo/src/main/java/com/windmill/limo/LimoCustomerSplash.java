package com.windmill.limo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;

import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidListener;
import com.adbid.media.ad.AdbidAppOpen;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.common.util.NumberUtils;
import com.windmill.sdk.custom.WMCustomSplashAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.models.WMBidResult;

import java.util.Map;

public class LimoCustomerSplash extends WMCustomSplashAdapter implements AdbidListener {

    private final String TAG = LimoCustomerSplash.this.getClass().getSimpleName();
    private AdbidAppOpen appOpenAd;
    private String placementId;
    private double cachedEcpm;


    /**
     * @param serverExtra Placement:CustomInfo:{"codeId":"801121648","isSplashEye":true}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            placementId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//801121648
            Log.d(TAG, "loadAd: " + placementId);

            if (TextUtils.isEmpty(placementId)) {
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(141);
                error.setMessage(error.getMessage() + ": 传入的渠道代码位等参数为空");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }

            if (context == null) {
                callLoadFail(new WMAdapterError(TBError.ERROR_ADN_LOAD.getErrorCode(),
                        "loadAd with activity is null"));
                return;
            }

            int fetchAdTimeout = 5;
            try {
                Object fetchAdTimeoutObj = serverExtra.get("countDown");
                if (fetchAdTimeoutObj != null) {
                    fetchAdTimeout = NumberUtils.parseInt(fetchAdTimeoutObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            appOpenAd = new AdbidAppOpen(placementId, fetchAdTimeout * 1000);
            appOpenAd.setAdListener(this);
            appOpenAd.loadAd();
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

    @Override public void showAd(ViewGroup viewGroup, Map<String, Object> serverExtra) {
        try {
            Log.d(TAG, "showAd: " + placementId);
            if (appOpenAd != null && isReady()) {
                appOpenAd.showAd(viewGroup);
            } else {
                TBError error = TBError.ERROR_ADN_NOT_READY.copy();
                callSplashAdShowError(new WMAdapterError(error.getErrorCode(), error.getMessage()));
            }
        } catch (Throwable e) {
            Log.d(TAG, "showAd: " + e.getMessage());
            e.printStackTrace();
            TBError error = TBError.ERROR_ADN_SHOW.copy();
            error.setInternalCode(163);
            error.setMessage(error.getMessage() + "; 渠道展示崩溃异常");
            error.setInternalMsg(e.getMessage());
            callSplashAdShowError(new WMAdapterError(error.getErrorCode(),
                    String.valueOf(error.getInternalCode()), error.getMessage()));
        }
    }


    @Override public boolean isReady() {
        return appOpenAd != null && appOpenAd.isReady();
    }

    @Override public void notifyBiddingResult(WMBidResult bidResult) {
        LimoCustomerProxy.handleBiddingResult(bidResult, appOpenAd);
    }

    @Override public void destroyAd() {
        super.destroyAd();
        if (appOpenAd != null) {
            appOpenAd.destroy();
            appOpenAd = null;
        }
    }

    @Override public void onAdLoad(AdbidAdInfo adbidAdInfo) {
        try {
            if (appOpenAd == null || adbidAdInfo == null) {
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(143);
                error.setMessage(error.getMessage() + ": 加载回调的参数广告对象/列表为空");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }
            cachedEcpm = adbidAdInfo.getPrice();
            Log.d(TAG, "onSplashRenderSuccess");
            if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                callLoadBiddingSuccess(new BidPrice(String.valueOf(cachedEcpm)));
            }
            callLoadSuccess();
        } catch (Throwable e) {
            Log.d(TAG, "onAdLoad error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public void onAdLoadFail(String adUnitId, AdbidError adbidError) {
        try {
            int code = adbidError != null ? adbidError.getCode() : -1;
            String msg = adbidError != null ? adbidError.getMessage() : "unknown";
            Log.d(TAG, "onAdLoadFail: " + code + ";" + msg);
            callLoadFail(new WMAdapterError(code, String.valueOf(code), msg));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void onAdDisplayed(AdbidAdInfo adbidAdInfo) {
        try {
            Log.d(TAG, "onAdDisplayed");
            callSplashAdShow();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void onAdDisplayedFailed(AdbidAdInfo adbidAdInfo, AdbidError adbidError) {
        try {
            int code = adbidError != null ? adbidError.getCode() : -1;
            String msg = adbidError != null ? adbidError.getMessage() : "unknown";
            Log.d(TAG, "onAdDisplayedFailed: " + code + ";" + msg);
            callSplashAdShowError(new WMAdapterError(code, String.valueOf(code), msg));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void onAdHidden(AdbidAdInfo adbidAdInfo) {
        try {
            Log.d(TAG, "onAdHidden");
            callSplashAdClosed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void onAdClicked(AdbidAdInfo adbidAdInfo) {
        try {
            Log.d(TAG, "onAdClicked");
            callSplashAdClick();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
