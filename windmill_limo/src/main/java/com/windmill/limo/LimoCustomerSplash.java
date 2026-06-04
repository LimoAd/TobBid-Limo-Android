package com.windmill.limo;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.adbid.media.AdBidLossInfo;
import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.AdbidListener;
import com.adbid.media.ad.AdbidAppOpen;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomSplashAdapter;
import com.windmill.sdk.models.BidPrice;
import com.windmill.sdk.splash.IWMSplashEyeAd;

import java.util.Map;

public class LimoCustomerSplash extends WMCustomSplashAdapter implements AdbidListener {

    private final String TAG = LimoCustomerSplash.this.getClass().getSimpleName();
    private @Nullable AdbidAppOpen mSplashAD;


    /**
     * @param context
     * @param viewGroup
     * @param localExtra
     * @param serverExtra Placement:CustomInfo:{"codeId":"801121648","isSplashEye":true}
     */
    @Override public void loadAd(Context context, ViewGroup viewGroup,
                                 Map<String, Object> localExtra, Map<String, Object> serverExtra) {
        try {

            if (context == null) {
                callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                        "activity is null"));
            } else {
                String codeId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//801121648

                mSplashAD = new AdbidAppOpen(codeId, 5000);
                mSplashAD.setAdListener(this);
                mSplashAD.loadAd();
            }
        } catch (Exception e) {
            callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                    e.getMessage()));
        }
    }

    @Override
    public void showAd(Activity activity, ViewGroup viewGroup, Map<String, Object> serverExtra) {
        try {
            if (mSplashAD!=null&&isReady()) {
                mSplashAD.showAd(viewGroup);
            } else {
                callSplashAdShowError(
                        new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                                "mSplashAD is null"));
            }
        } catch (Exception e) {
            callSplashAdShowError(
                    new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_PLAY.getErrorCode(),
                            e.getMessage()));
        }
    }


    @Override public boolean isReady() {
        return mSplashAD != null && mSplashAD.isReady();
    }


    @Override
    public void notifyBiddingResult(boolean isWin, String price, Map<String, Object> referBidInfo) {
        Log.d(TAG, "notifyBiddingResult " + isWin + ":" + price);
        if (mSplashAD != null) {
            if (isWin) {
                mSplashAD.winNotice(Double.parseDouble(price));
            } else {
                mSplashAD.lossNotice(new AdBidLossInfo(Double.parseDouble(price)));
            }
        }
    }

    @Override public void destroyAd() {
        if (mSplashAD != null) {
            mSplashAD.destroy();
            mSplashAD = null;
        }
    }

    @Override public IWMSplashEyeAd getSplashEyeAd() {
        return null;
    }


    @Override public void onAdLoad(AdbidAdInfo adbidAdInfo) {
        Log.d(TAG, "onSplashRenderSuccess");
        if (mSplashAD == null) {
            callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                    "limo splash is null"));
            return;
        }
        if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
            callLoadBiddingSuccess(new BidPrice(String.valueOf((int) adbidAdInfo.getPrice())));
        }
        callLoadSuccess();
    }

    @Override public void onAdLoadFail(String s, AdbidError adbidError) {
        Log.d(TAG, "onAdLoadFail");
        callLoadFail(new WMAdapterError(adbidError.getCode(), adbidError.getMessage()));
    }

    @Override public void onAdDisplayed(AdbidAdInfo adbidAdInfo) {
        Log.d(TAG, "onAdDisplayed");
        callSplashAdShow();
    }

    @Override public void onAdDisplayedFailed(AdbidAdInfo adbidAdInfo, AdbidError adbidError) {
        Log.d(TAG, "onAdDisplayedFailed");
        callSplashAdShowError(new WMAdapterError(adbidError.getCode(), adbidError.getMessage()));
    }

    @Override public void onAdHidden(AdbidAdInfo adbidAdInfo) {
        Log.d(TAG, "onAdHidden");
        callSplashAdClosed();
    }

    @Override public void onAdClicked(AdbidAdInfo adbidAdInfo) {
        Log.d(TAG, "onAdClicked");
        callSplashAdClick();
    }
}
