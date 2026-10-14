package com.windmill.limo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidBannerListener;
import com.adbid.media.AdbidError;
import com.adbid.media.ad.AdbidBannerView;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomBannerAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.models.WMBidResult;

import java.util.Map;


public class LimoCustomerBanner extends WMCustomBannerAdapter {

    private final String TAG = LimoCustomerBanner.this.getClass().getSimpleName();
    private AdbidBannerView bannerView;
    private String placementId;
    private double cachedEcpm;

    @Override public Object getChannelObject() {
        return bannerView;
    }

    /**
     * @param serverExtra Placement:CustomInfo:{"codeId":"948728682","adSize":"600x300"}
     */
    @Override public void loadAd(final Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            placementId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);
            if (TextUtils.isEmpty(placementId)) {
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(141);
                error.setMessage(error.getMessage() + ": 传入的渠道代码位等参数为空");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }

            int width = 0, height = 0;
            try {
                if (localExtra != null) {
                    Object w = localExtra.get(WMConstants.AD_WIDTH);
                    if (w != null && !"0".equals(String.valueOf(w))) {
                        width = Integer.parseInt(String.valueOf(w));
                    }
                    Object h = localExtra.get(WMConstants.AD_HEIGHT);
                    if (h != null && !"0".equals(String.valueOf(h))) {
                        height = Integer.parseInt(String.valueOf(h));
                    }
                }
                if (width == 0 || height == 0) {
                    String adSize = (String) serverExtra.get(WMConstants.AD_SIZE);//600x300
                    if (adSize != null) {
                        int x = adSize.indexOf("x");
                        width = Integer.parseInt(adSize.substring(0, x));
                        height = Integer.parseInt(adSize.substring(x + 1));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, "loadAd:" + placementId + " width:" + width + " height:" + height);

            if (context == null) {
                callLoadFail(new WMAdapterError(TBError.ERROR_ADN_LOAD.getErrorCode(),
                        "loadAd with context is null"));
                return;
            }

            bannerView = new AdbidBannerView(context);
            bannerView.setUnitId(placementId);
            if (width > 0 && height > 0) {
                bannerView.setAdSize(width, height);
            }
            bannerView.setBannerAdListener(new AdbidBannerListener() {
                @Override public void onBannerLoad(AdbidAdInfo adInfo) {
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
                        if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                            callLoadBiddingSuccess(new BidPrice(String.valueOf(cachedEcpm)));
                        }
                        callLoadSuccess();
                    } catch (Throwable e) {
                        Log.d(TAG, "onBannerLoad error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override public void onBannerFail(String adUnitId, AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onBannerFail: " + code + ";" + msg);
                        callLoadFail(new WMAdapterError(code, String.valueOf(code), msg));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onBannerShow(AdbidAdInfo adInfo) {
                    try {
                        callBannerAdShow();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onBannerClose(AdbidAdInfo adInfo) {
                    try {
                        callBannerAdClosed();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onBannerClicked(AdbidAdInfo adInfo) {
                    try {
                        callBannerAdClick();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            //加载广告
            bannerView.loadAd();

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
        return bannerView != null;
    }

    @Override public void notifyBiddingResult(WMBidResult bidResult) {
        LimoCustomerProxy.handleBiddingResult(bidResult, bannerView);
    }

    @Override public View getBannerView() {
        return bannerView;
    }

    @Override public void destroyAd() {
        super.destroyAd();
        if (bannerView != null) {
            bannerView.destroy();
            bannerView = null;
        }
    }
}
