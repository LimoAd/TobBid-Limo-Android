package com.windmill.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.windmill.android.demo.log.CallBackInfo;
import com.windmill.android.demo.log.CallBackItem;
import com.windmill.android.demo.log.ExpandAdapter;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.models.AdInfo;
import com.windmill.sdk.reward.WMRewardAd;
import com.windmill.sdk.reward.WMRewardAdListener;
import com.windmill.sdk.reward.WMRewardAdRequest;
import com.windmill.sdk.reward.WMRewardInfo;

import java.util.ArrayList;
import java.util.List;

public class RewardVideoActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private WMRewardAd windRewardedVideoAd;
    private String placementId;

    private ListView listView;
    private ExpandAdapter adapter;
    private List<CallBackItem> callBackDataList = new ArrayList<>();

    private void initCallBack() {
        resetCallBackData();
        listView = findViewById(R.id.callback_lv);
        adapter = new ExpandAdapter(this, callBackDataList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("lance", "------onItemClick------" + position);
                CallBackItem callItem = callBackDataList.get(position);
                if (callItem != null) {
                    if (callItem.is_expand()) {
                        callItem.set_expand(false);
                    } else {
                        callItem.set_expand(true);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_video);

        spinner = findViewById(R.id.id_spinner);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.reward_adapter));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(this);

        WebView.setWebContentsDebuggingEnabled(true);

        String[] stringArray = getResources().getStringArray(R.array.reward_id_value);
        placementId = stringArray[0];

        initCallBack();


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (windRewardedVideoAd != null) {
            windRewardedVideoAd.destroy();
            windRewardedVideoAd = null;
        }
    }

    public void ButtonClick(View view) {
        switch (view.getId()) {
            case R.id.bt_load_ad:
                resetCallBackData();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                loadAd();
                break;
            case R.id.bt_show_ad:
                if (windRewardedVideoAd != null && windRewardedVideoAd.isReady()) {
                    windRewardedVideoAd.showAd(this, null);
                } else {
                    Log.d("lance", "------Ad is not Ready------");
                }
                break;
        }
    }

    private void loadAd() {
        windRewardedVideoAd = new WMRewardAd(this, new WMRewardAdRequest(placementId, String.valueOf(0), null));

        windRewardedVideoAd.setRewardAdListener(new WMRewardAdListener() {
            @Override
            public void onVideoAdLoadSuccess(final String placementId) {
                Log.d("lance", "------onVideoAdLoadSuccess------" + placementId);
                logCallBack("onVideoAdLoadSuccess", "");
            }

            @Override
            public void onVideoAdPlayEnd(AdInfo adInfo) {
                Log.d("lance", "------onVideoAdPlayEnd------" + adInfo.getPlacementId());
                logCallBack("onVideoAdPlayEnd", "");
            }

            @Override
            public void onVideoAdPlayStart(AdInfo adInfo) {
                Log.d("lance", "------onVideoAdPlayStart------" + adInfo.getPlacementId());
                logCallBack("onVideoAdPlayStart", "");
            }

            @Override
            public void onVideoAdClicked(AdInfo adInfo) {
                Log.d("lance", "------onVideoAdClicked------" + adInfo.getPlacementId());
                logCallBack("onVideoAdClicked", "");
            }

            @Override
            public void onVideoAdClosed(AdInfo adInfo) {
                Log.d("lance", "------onVideoAdClosed------" + adInfo.getPlacementId());
                logCallBack("onVideoAdClosed", "");
            }

            @Override
            public void onVideoRewarded(AdInfo adInfo, final WMRewardInfo rewardInfo) {
                Log.d("lance", "------onVideoRewarded------" + rewardInfo.toString() + ":" + adInfo.getPlacementId());
                logCallBack("onVideoRewarded", rewardInfo.toString());
            }

            @Override
            public void onVideoAdLoadError(final WindMillError error, final String placementId) {
                Log.d("lance", "onVideoAdLoadError------onVideoAdLoadError------:" + windRewardedVideoAd.getLoadFailMessages() + ":" + placementId + ":" + error);
                logCallBack("onVideoAdLoadError", windRewardedVideoAd.getLoadFailMessages() + ":" + placementId);
            }

            @Override
            public void onVideoAdPlayError(final WindMillError error, final String placementId) {
                Log.d("lance", "------onVideoAdPlayError------" + windRewardedVideoAd.getLoadFailMessages() + ":" + placementId + ":" + error);
                logCallBack("onVideoAdPlayError", windRewardedVideoAd.getLoadFailMessages() + ":" + placementId);
            }
        });

        windRewardedVideoAd.loadAd();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String[] stringArray = getResources().getStringArray(R.array.reward_id_value);
        placementId = stringArray[position];
        Log.d("lance", "------onItemSelected------" + position + ":" + placementId);

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d("lance", "------onNothingSelected------");
    }

    private void resetCallBackData() {
        callBackDataList.clear();
        for (int i = 0; i < CallBackInfo.REWARD_CALLBACK.length; i++) {
            callBackDataList.add(new CallBackItem(CallBackInfo.REWARD_CALLBACK[i], "", false, false));
        }
    }

    private void logCallBack(String call, String child) {
        for (int i = 0; i < callBackDataList.size(); i++) {
            CallBackItem callItem = callBackDataList.get(i);
            if (callItem.getText().equals(call)) {
                callItem.set_callback(true);
                if (!TextUtils.isEmpty(child)) {
                    callItem.setChild_text(child);
                }
                break;
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

}