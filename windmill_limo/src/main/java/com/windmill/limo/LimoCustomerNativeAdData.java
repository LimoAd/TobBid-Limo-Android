package com.windmill.limo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.adbid.media.AdMaterialType;
import com.adbid.media.AdbidAdInfo;
import com.adbid.media.AdbidError;
import com.adbid.media.nativeAd.AdbidNativeAd;
import com.adbid.media.nativeAd.AdbidNativeAdView;
import com.adbid.media.nativeAd.AdbidNativeAppInfo;
import com.adbid.media.nativeAd.AdbidNativeEventListener;
import com.adbid.media.nativeAd.AdbidNativeVideoListener;
import com.czhj.sdk.common.utils.ImageManager;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.base.TBError;
import com.windmill.sdk.base.WMAdapterError;
import com.windmill.sdk.custom.WMCustomNativeAdData;
import com.windmill.sdk.natives.WMNativeAdDataType;
import com.windmill.sdk.natives.WMNativePrivacyInfo;

import java.util.ArrayList;
import java.util.List;


public class LimoCustomerNativeAdData extends WMCustomNativeAdData {

    private final String TAG = LimoCustomerNativeAdData.this.getClass().getSimpleName();
    private @Nullable AdbidNativeAd nativeAd;

    public LimoCustomerNativeAdData(AdbidNativeAd nativeAd) {
        this.nativeAd = nativeAd;
        if (nativeAd == null) {
            return;
        }
        try {
            setTitle(nativeAd.getTitle());
            setDesc(nativeAd.getDescriptionText());
            setIconUrl(nativeAd.getIconImgUrl());
            setCTAText(nativeAd.getCallToAction());
            setIsExpressAd(false);
            setAdLogo(nativeAd.getAdLogoBitmap());

            ArrayList<String> imageUrlList = new ArrayList<>();
            String mainImageUrl = nativeAd.getMainImageUrl();
            if (!TextUtils.isEmpty(mainImageUrl)) {
                imageUrlList.add(mainImageUrl);
            }
            List<String> imageList = nativeAd.getImageUrlList();
            if (imageList != null && !imageList.isEmpty()) {
                imageUrlList.addAll(imageList);
                setImageUrlList(imageUrlList);
            }

            AdMaterialType materialType = nativeAd.getAdMaterialType();
            if (materialType == AdMaterialType.VIDEO) {
                setAdPatternType(WMNativeAdDataType.NATIVE_VIDEO_AD);
            } else if (materialType == AdMaterialType.IMAGE) {
                setAdPatternType(WMNativeAdDataType.NATIVE_BIG_IMAGE_AD);
            } else if (materialType == AdMaterialType.MULTIPLE_IMAGE) {
                setAdPatternType(WMNativeAdDataType.NATIVE_GROUP_IMAGE_AD);
            }

            AdbidAdInfo adInfo = nativeAd.getAdbidAdInfo();
            if (adInfo != null) {
                setEcpm(String.valueOf(adInfo.getPrice()));
            }

            if (nativeAd.isDownload()) {
                setInteractionType(WMConstants.INTERACTION_TYPE_DOWNLOAD);
            }

            AdbidNativeAppInfo nativeAppInfo = nativeAd.getNativeAppInfo();
            if (nativeAppInfo != null) {
                setAppInfo(new WMNativePrivacyInfo(nativeAppInfo.getAppName(),
                        nativeAppInfo.getAppVersion(), 0L, "", nativeAppInfo.getPublisher(),
                        nativeAppInfo.getAppPrivacyUrl(), "", nativeAppInfo.getAppPermissonUrl(),
                        nativeAppInfo.getFunctionUrl()));
            }

            nativeAd.setEventListener(new AdbidNativeEventListener() {
                @Override public void onImpression(AdbidNativeAdView view, AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onImpression");
                        callNativeAdShow();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.d(TAG, "onImpression error: " + e.getMessage());
                    }
                }

                @Override public void onNativeAdClick(AdbidNativeAdView view, AdbidAdInfo adInfo) {
                    try {
                        Log.d(TAG, "onNativeAdClick");
                        callNativeAdClick();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.d(TAG, "onNativeAdClick error: " + e.getMessage());
                    }
                }

                @Override public void onAdClose(AdbidNativeAdView view) {
                    try {
                        Log.d(TAG, "onAdClose");
                        callNativeAdDislikeSelect(0, "limo", true);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Log.d(TAG, "onAdClose error: " + e.getMessage());
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(TAG, "init native ad data error: " + e.getMessage());
        }
    }

    @Override public void render() {
    }

    @Override public void bindViewForInteraction(Context context, View view, List<View> clickableViews,
                                                 List<View> creativeViewList, View dislikeView) {
        if (nativeAd == null || view == null) {
            return;
        }
        try {
            AdbidNativeAdView adView = new AdbidNativeAdView(context);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params == null) {
                params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if (view.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) view.getParent();
                int index = parent.indexOfChild(view);
                parent.removeView(view);
                adView.addView(view, params);
                parent.addView(adView, index, params);
            } else {
                adView.addView(view, params);
            }
            ArrayList<View> allClicks = new ArrayList<>();
            if (clickableViews != null) {
                allClicks.addAll(clickableViews);
            }
            if (creativeViewList != null) {
                allClicks.addAll(creativeViewList);
            }
            nativeAd.registerViews(adView, allClicks, dislikeView);
        } catch (Throwable e) {
            Log.d(TAG, "bindViewForInteraction error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public void bindMediaView(Context context, ViewGroup mediaLayout) {
        if (nativeAd == null || mediaLayout == null) {
            return;
        }
        try {
            View mediaView = nativeAd.getMediaView();
            if (mediaView != null && nativeAd.getAdMaterialType() == AdMaterialType.VIDEO) {
                if (mediaView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) mediaView.getParent()).removeView(mediaView);
                }
                mediaLayout.removeAllViews();
                mediaLayout.addView(mediaView);
            }
            nativeAd.setVideoListener(new AdbidNativeVideoListener() {
                @Override public void onVideoStart() {
                    try {
                        Log.d(TAG, "onVideoStart");
                        callNativeAdVideoStart();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onVideoPause() {
                    try {
                        Log.d(TAG, "onVideoPause");
                        callNativeAdVideoPause();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onVideoResume() {
                    try {
                        Log.d(TAG, "onVideoResume");
                        callNativeAdVideoResume();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onVideoComplete() {
                    try {
                        Log.d(TAG, "onVideoComplete");
                        callNativeAdVideoCompleted();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onVideoError(AdbidError error) {
                    try {
                        int code = error != null ? error.getCode() : -1;
                        String msg = error != null ? error.getMessage() : "unknown";
                        Log.d(TAG, "onVideoError: " + code + ";" + msg);
                        TBError tbError = TBError.ERROR_ADN_SHOW.copy();
                        tbError.setMessage(tbError.getMessage() + "; onVideoError: " + code + "; " + msg);
                        callNativeAdVideoError(new WMAdapterError(tbError.getErrorCode(),
                                tbError.getMessage()));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onVideoProgressUpdate(long current, long total) {
                    try {
                        Log.d(TAG, "onVideoProgress: " + current + "; " + total);
                        callNativeAdVideoProgressUpdate(total, current);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Throwable e) {
            Log.d(TAG, "bindMediaView error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public void bindImageViews(Context context, List<ImageView> imageViews, int defaultImageRes) {
        Log.d(TAG, "bindImageViews: " + (imageViews == null ? "null" : "size: " + imageViews.size()));
        if (nativeAd == null || imageViews == null || imageViews.isEmpty()) {
            return;
        }
        try {
            List<String> urls = nativeAd.getImageUrlList();
            if (urls != null && !urls.isEmpty()) {
                int min = Math.min(imageViews.size(), urls.size());
                for (int i = 0; i < min; i++) {
                    String url = urls.get(i);
                    ImageView imageView = imageViews.get(i);
                    if (url == null || url.isEmpty()) {
                        continue;
                    }
                    ImageManager.with(context).load(url).placeholder(defaultImageRes)
                            .error(defaultImageRes).into(imageView);
                }
            }
        } catch (Throwable e) {
            Log.d(TAG, "bindImageViews error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public void startVideo() {
        try {
            if (nativeAd != null) {
                nativeAd.startVideo();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void pauseVideo() {
        try {
            if (nativeAd != null) {
                nativeAd.pauseVideo();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public void setVideoMute(boolean muted) {
        try {
            if (nativeAd != null) {
                nativeAd.setMuted(muted);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override public long getVideoDuration() {
        try {
            if (nativeAd != null) {
                return nativeAd.getVideoDuration();
            }
            return super.getVideoDuration();
        } catch (Throwable e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override public Object getOriginNativeAdData() {
        return nativeAd;
    }

    @Override public void destroy() {
        try {
            super.destroy();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        try {
            if (nativeAd != null) {
                nativeAd.destroy();
                nativeAd = null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
