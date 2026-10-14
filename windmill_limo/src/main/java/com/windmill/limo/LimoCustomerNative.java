package com.windmill.limo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.ad.AdbidNativeLoader;
import com.adbid.media.nativeAd.AdbidNativeAd;
import com.adbid.media.nativeOverseas.NativeAdbidLoadListener;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomNativeAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.models.WMBidResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LimoCustomerNative extends WMCustomNativeAdapter {

    private final String TAG = LimoCustomerNative.this.getClass().getSimpleName();
    private @Nullable AdbidNativeLoader nativeLoader;
    private @Nullable AdbidNativeAd nativeAd;
    private String placementId;

    /**
     * @param serverExtra Placement:CustomInfo:{"codeId":"4952664543881484"}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            placementId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);
            Log.d(TAG, "loadAd:" + placementId + "; nativeAdType:" + getNativeAdType());

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
                        "loadAd with context is null"));
                return;
            }

            //只支持自渲染信息流
            if (getNativeAdType() != WMConstants.AD_NATIVE_TYPE_CUSTOM) {
                Log.d(TAG, "loadAd: nativeAdType is not support, only CUSTOM(self-render)");
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(144);
                error.setMessage(error.getMessage() + ": 下发广告位子类型不支持");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }

            nativeLoader = new AdbidNativeLoader(context, placementId, new NativeAdbidLoadListener() {
                @Override public void onNativeAdLoaded(AdbidNativeAd ad) {
                    try {
                        Log.d(TAG, "onNativeAdLoaded");
                        if (ad == null) {
                            TBError error = TBError.ERROR_ADN_LOAD.copy();
                            error.setInternalCode(143);
                            error.setMessage(error.getMessage() + ": 加载回调的参数广告对象/列表为空");
                            callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                                    error.getMessage()));
                            return;
                        }
                        nativeAd = ad;
                        double price = 0;
                        AdbidAdInfo adbidAdInfo = ad.getAdbidAdInfo();
                        if (adbidAdInfo != null) {
                            price = adbidAdInfo.getPrice();
                        }
                        Log.d(TAG, "onNativeAdLoaded price: " + price);

                        List<LimoCustomerNativeAdData> adDataList = new ArrayList<>();
                        adDataList.add(new LimoCustomerNativeAdData(ad));

                        if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                            callLoadBiddingSuccess(new BidPrice(String.valueOf(price)));
                        }
                        callLoadSuccess(adDataList);
                    } catch (Throwable e) {
                        Log.d(TAG, "onNativeAdLoaded error: " + e.getMessage());
                        e.printStackTrace();
                        int code = TBError.ERROR_ADN_LOAD.getErrorCode();
                        callLoadFail(new WMAdapterError(code, String.valueOf(code),
                                e.getMessage() != null ? e.getMessage() : "onNativeAdLoaded error"));
                    }
                }

                @Override public void onNativeAdLoadFail(AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onNativeAdLoadFail: " + code + ";" + msg);
                        callLoadFail(new WMAdapterError(code, String.valueOf(code), msg));
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.d(TAG, "onNativeAdLoadFail error: " + e.getMessage());
                    }
                }
            });
            nativeLoader.loadAd();
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

    @Override public boolean isReady() {
        return nativeAd != null && nativeAd.isReady();
    }

    @Override public boolean isExpressRender() {
        return false;
    }

    @Override public void notifyBiddingResult(WMBidResult bidResult) {
        LimoCustomerProxy.handleBiddingResult(bidResult, nativeLoader);
    }

    @Override public void destroyAd() {
        super.destroyAd();
        if (nativeAd != null) {
            nativeAd.destroy();
            nativeAd = null;
        }
        nativeLoader = null;
    }
}
