package com.windmill.limo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adbid.media.AdBidLossInfo;
import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidListener;
import com.adbid.media.ad.AdbidInterstitial;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomInterstitialAdapter;
import com.windmill.sdk.models.BidPrice;

import java.util.HashMap;
import java.util.Map;


public class LimoCustomerInterstitial extends WMCustomInterstitialAdapter {

    private final String TAG = LimoCustomerInterstitial.this.getClass().getSimpleName();
    private @Nullable AdbidInterstitial interstitialAd;

    /**
     * @param context
     * @param localExtra
     * @param serverExtra Placement:CustomInfo:{"codeId":"901121375"}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            String codeId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//901121375

            Log.d(TAG, "loadAd:" + codeId);

            interstitialAd = new AdbidInterstitial(codeId);
            interstitialAd.setAdListener(new AdbidListener() {
                @Override public void onAdLoad(@NonNull AdbidAdInfo adInfo) {
                    //获取广告价格，单位分
                    int price = (int) adInfo.getPrice();
                    Log.d(TAG, "onAdLoad");
                    if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                        callLoadBiddingSuccess(new BidPrice(String.valueOf(price)));
                    }
                    callLoadSuccess();
                }

                @Override
                public void onAdLoadFail(@Nullable String adUnitId, @NonNull AdbidError error) {
                    Log.d(TAG, "onAdLoadFail:" + error.getCode() + ":" + error.getMessage());
                    callLoadFail(new WMAdapterError(error.getCode(), error.getMessage()));
                }

                @Override public void onAdDisplayed(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdShow");
                    callVideoAdShow();
                }

                @Override
                public void onAdDisplayedFailed(@Nullable AdbidAdInfo adInfo, AdbidError error) {
                    Log.d(TAG, "onAdDisplayedFailed");
                    callVideoAdPlayError(new WMAdapterError(error.getCode(), error.getMessage()));
                }

                @Override public void onAdHidden(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdClose");
                    callVideoAdClosed();
                }

                @Override public void onAdClicked(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdVideoBarClick");
                    callVideoAdClick();
                }
            });

            //加载广告
            interstitialAd.loadAd();
        } catch (Throwable e) {
            callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                    "catch limo loadAd error " + e.getMessage()));
        }
    }

    @Override public void showAd(Activity activity, HashMap<String, String> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            if (interstitialAd != null && isReady()) {
                interstitialAd.showAd();
            } else {
                callVideoAdPlayError(
                        new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                                "成功加载广告后再进行广告展示！"));
            }
        } catch (Throwable throwable) {
            callVideoAdPlayError(
                    new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                            "catch limo presentVideoAd error " + throwable.getMessage()));
        }
    }

    @Override public boolean isReady() {
        return interstitialAd != null && interstitialAd.isReady();
    }

    @Override
    public void notifyBiddingResult(boolean isWin, String price, Map<String, Object> referBidInfo) {
        Log.d(TAG, "notifyBiddingResult " + isWin + ":" + price);
        if (interstitialAd != null) {
            if (isWin) {
                interstitialAd.winNotice(Double.parseDouble(price));
            } else {
                interstitialAd.lossNotice(new AdBidLossInfo(Double.parseDouble(price)));
            }
        }
    }

    @Override public void destroyAd() {
        if (interstitialAd != null) {
            interstitialAd.destroy();
            interstitialAd = null;
        }
    }
}
