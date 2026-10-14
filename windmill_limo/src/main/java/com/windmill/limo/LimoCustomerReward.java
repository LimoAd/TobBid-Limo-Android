package com.windmill.limo;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidRewardListener;
import com.adbid.media.ad.AdbidRewarded;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomRewardAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.models.WMBidResult;

import java.util.HashMap;
import java.util.Map;


public class LimoCustomerReward extends WMCustomRewardAdapter {

    private final String TAG = LimoCustomerReward.this.getClass().getSimpleName();
    private AdbidRewarded rewardedAd;
    private String placementId;

    @Override public Object getChannelObject() {
        return rewardedAd;
    }

    /**
     * @param serverExtra Placement:CustomInfo:{"codeId":"901121365"}
     */
    @Override public void loadAd(Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            placementId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//901121365
            Log.d(TAG, "loadAd:" + placementId);

            if (TextUtils.isEmpty(placementId)) {
                TBError error = TBError.ERROR_ADN_LOAD.copy();
                error.setInternalCode(141);
                error.setMessage(error.getMessage() + ": 传入的渠道代码位等参数为空");
                callLoadFail(new WMAdapterError(error.getErrorCode(), error.getExtraErrorCode(),
                        error.getMessage()));
                return;
            }

            //创建激励广告
            rewardedAd = new AdbidRewarded(placementId);
            if (localExtra != null) {
                rewardedAd.setLocalExtra(localExtra);
            }

            rewardedAd.setAdListener(new AdbidRewardListener() {
                @Override public void onUserReward(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onUserReward");
                        callVideoAdReward(true);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

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
                        double cachedEcpm = adInfo.getPrice();
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
                        Log.d(TAG, "onAdLoadFail: " + code + ";" + msg);
                        callLoadFail(new WMAdapterError(code, String.valueOf(code), msg));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdDisplayed(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdDisplayed");
                        callVideoAdShow();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdDisplayedFailed(AdbidAdInfo adInfo, AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onAdDisplayedFailed: " + code + ";" + msg);
                        TBError tbError = TBError.ERROR_ADN_SHOW.copy();
                        tbError.setMessage(tbError.getMessage() + "; 渠道展示错误: " + code + "; " + msg);
                        callVideoAdPlayError(new WMAdapterError(tbError.getErrorCode(), String.valueOf(code),
                                tbError.getMessage()));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdHidden(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdHidden");
                        callVideoAdClosed();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onAdClicked(AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onAdClicked");
                        callVideoAdClick();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });

            //加载广告
            rewardedAd.loadAd();
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
            if (isReady()) {
                rewardedAd.showAd();
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
        return rewardedAd != null && rewardedAd.isReady();
    }

    @Override public void notifyBiddingResult(WMBidResult bidResult) {
        LimoCustomerProxy.handleBiddingResult(bidResult, rewardedAd);
    }

    @Override public void destroyAd() {
        super.destroyAd();
        if (rewardedAd != null) {
            rewardedAd.destroy();
            rewardedAd = null;
        }
    }
}
