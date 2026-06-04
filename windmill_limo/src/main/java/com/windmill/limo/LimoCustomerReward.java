package com.windmill.limo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adbid.media.AdBidLossInfo;
import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidRewardListener;
import com.adbid.media.ad.AdbidRewarded;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomRewardAdapter;
import com.windmill.sdk.models.BidPrice;

import java.util.HashMap;
import java.util.Map;


public class LimoCustomerReward extends WMCustomRewardAdapter {

    private final String TAG = LimoCustomerReward.this.getClass().getSimpleName();
    private @Nullable AdbidRewarded rewardedAd;

    /**
     * @param context
     * @param localExtra
     * @param serverExtra Placement:CustomInfo:{"codeId":"901121365"}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            String codeId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//901121365

            Log.d(TAG, "loadAd:" + codeId);

            //创建激励广告
            rewardedAd = new AdbidRewarded(codeId);

            rewardedAd.setLocalExtra(localExtra);

            rewardedAd.setAdListener(new AdbidRewardListener() {
                @Override public void onUserReward(@NonNull AdbidAdInfo adInfo) {
                    callVideoAdReward(true);
                }

                @Override public void onAdLoad(@NonNull AdbidAdInfo adInfo) {
                    //获取广告价格，单位分
                    Log.d(TAG, "onAdLoad");
                    int price = (int) adInfo.getPrice();
                    if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                        callLoadBiddingSuccess(new BidPrice(String.valueOf(price)));
                    }
                    callLoadSuccess();
                }

                @Override
                public void onAdLoadFail(@Nullable String adUnitId, @NonNull AdbidError error) {
                    Log.d(TAG, "onError " + error.getCode() + ":" + error.getMessage());
                    callLoadFail(new WMAdapterError(error.getCode(), error.getMessage()));
                }

                @Override public void onAdDisplayed(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdDisplayed");
                    callVideoAdShow();
                }

                @Override
                public void onAdDisplayedFailed(@Nullable AdbidAdInfo adInfo, AdbidError error) {
                    Log.d(TAG, "onAdDisplayedFailed");
                    callVideoAdPlayError(new WMAdapterError(error.getCode(), error.getMessage()));
                }

                @Override public void onAdHidden(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdHidden");
                    callVideoAdClosed();
                }

                @Override public void onAdClicked(@NonNull AdbidAdInfo adInfo) {
                    Log.d(TAG, "onAdClicked");
                    callVideoAdClick();
                }
            });

            //加载广告
            rewardedAd.loadAd();
        } catch (Throwable e) {
            callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                    e.getMessage()));
        }
    }

    @Override public void showAd(Activity activity, HashMap<String, String> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            if (rewardedAd != null) {
                rewardedAd.showAd();
            } else {
                callVideoAdPlayError(
                        new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                                "成功加载广告后再进行广告展示!"));
            }
        } catch (Throwable e) {
            callVideoAdPlayError(
                    new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                            e.getMessage()));
        }
    }

    @Override
    public void notifyBiddingResult(boolean isWin, String price, Map<String, Object> referBidInfo) {
        Log.d(TAG, "notifyBiddingResult " + isWin + ":" + price);
        if (rewardedAd != null) {
            if (isWin) {
                rewardedAd.winNotice(Double.parseDouble(price));
            } else {
                rewardedAd.lossNotice(new AdBidLossInfo(Double.parseDouble(price)));
            }
        }
    }

    @Override public boolean isReady() {
        return rewardedAd != null && rewardedAd.isReady();
    }

    @Override public void destroyAd() {
        if (rewardedAd != null) {
            rewardedAd.destroy();
            rewardedAd = null;
        }
    }

}
