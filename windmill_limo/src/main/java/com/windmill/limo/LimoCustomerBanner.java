package com.windmill.limo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adbid.media.AdBidLossInfo;
import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidBannerListener;
import com.adbid.media.AdbidError;
import com.adbid.media.ad.AdbidBannerView;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomBannerAdapter;
import com.windmill.sdk.models.BidPrice;

import org.json.JSONObject;

import java.util.Map;


public class LimoCustomerBanner extends WMCustomBannerAdapter {

    private final String TAG = LimoCustomerBanner.this.getClass().getSimpleName();
    private @Nullable AdbidBannerView bannerView;

    /**
     * @param context
     * @param localExtra
     * @param serverExtra Placement:CustomInfo:{"codeId":"948728682","adSize":"600x300"}
     */
    @Override public void loadAd(final Context context, Map<String, Object> localExtra,
                                 Map<String, Object> serverExtra) {
        try {
            String codeId = (String) serverExtra.get(WMConstants.PLACEMENT_ID);//901121246
            String placementCustomInfo = (String) serverExtra.get(WMConstants.CUSTOM_INFO);
            int width = 0, height = 0;
            if (placementCustomInfo != null && !TextUtils.isEmpty(placementCustomInfo)) {
                try {
                    JSONObject object = new JSONObject(placementCustomInfo);
                    String adSize = object.optString("adSize");//600x300
                    int x = adSize.indexOf("x");
                    width = (int) Float.parseFloat(adSize.substring(0, x));
                    height = (int) Float.parseFloat(adSize.substring(x + 1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "loadAd:" + codeId + ":" + width + ":" + height);

            if (context == null) {
                callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                        "loadAd with activity is null or adSize is error"));
                return;
            }

            bannerView = new AdbidBannerView(context);
            bannerView.setUnitId(codeId);
            bannerView.setAdSize(width, height);
            bannerView.setBannerAdListener(new AdbidBannerListener() {
                @Override public void onBannerLoad(@NonNull AdbidAdInfo adInfo) {
                    int price = (int) adInfo.getPrice();
                    if (getBiddingType() == WMConstants.AD_TYPE_CLIENT_BIDING) {
                        callLoadBiddingSuccess(new BidPrice(String.valueOf(price)));
                    }
                    callLoadSuccess();
                }

                @Override
                public void onBannerFail(@NonNull String adUnitId, @NonNull AdbidError error) {
                    callLoadFail(new WMAdapterError(error.getCode(), error.getMessage()));
                }

                @Override public void onBannerShow(@NonNull AdbidAdInfo adInfo) {
                    callBannerAdShow();

                }

                @Override public void onBannerClose(@NonNull AdbidAdInfo adInfo) {
                    callBannerAdClosed();
                }

                @Override public void onBannerClicked(@NonNull AdbidAdInfo adInfo) {
                    callBannerAdClick();
                }
            });

            //加载广告
            bannerView.loadAd();

        } catch (Throwable e) {
            callLoadFail(new WMAdapterError(WindMillError.ERROR_AD_ADAPTER_LOAD.getErrorCode(),
                    e.getMessage()));
        }
    }

    @Override
    public void notifyBiddingResult(boolean isWin, String price, Map<String, Object> referBidInfo) {
        Log.d(TAG, "notifyBiddingResult " + isWin + ":" + price);
        if (bannerView != null) {
            if (isWin) {
                bannerView.winNotice(Double.parseDouble(price));
            } else {
                bannerView.lossNotice(new AdBidLossInfo(Double.parseDouble(price)));
            }
        }
    }

    @Override public boolean isReady() {
        return bannerView != null;
    }

    @Override public View getBannerView() {
        return bannerView;
    }

    @Override public void destroyAd() {
        if (bannerView != null) {
            bannerView.destroy();
            bannerView = null;
        }
    }
}
