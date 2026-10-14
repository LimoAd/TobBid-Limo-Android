package com.windmill.android.demo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.windmill.android.demo.log.CallBackInfo;
import com.windmill.android.demo.log.CallBackItem;
import com.windmill.android.demo.log.ExpandAdapter;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.models.AdInfo;
import com.windmill.sdk.splash.WMSplashAd;
import com.windmill.sdk.splash.WMSplashAdListener;
import com.windmill.sdk.splash.WMSplashAdRequest;
import java.util.ArrayList;
import java.util.List;

public class SplashAdActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private String placementId;
    private ListView listView;
    private ExpandAdapter adapter;
    private ViewGroup splashLY;
    private WMSplashAd splashAd;

    private List<CallBackItem> callBackDataList = new ArrayList<>();

    private void initViewGroup(Activity activity) {

//        if (this.splashLY != null) {
//            if (this.splashLY.getParent() != null) {
//                ((ViewGroup) this.splashLY.getParent()).removeView(this.splashLY);
//            }
//
//            this.splashLY = null;
//        }

        splashLY = new RelativeLayout(activity);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();
        viewGroup.addView(splashLY, layoutParams);
    }

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
        setContentView(R.layout.activity_splash_ad);
        spinner = findViewById(R.id.id_spinner);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.splash_adapter));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(this);

        CheckBox fullScreen = findViewById(R.id.cb_fullscreen);
        CheckBox selfLogo = findViewById(R.id.cb_self_logo);
        fullScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedPreferences = SplashAdActivity.this.getSharedPreferences("setting", 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(Constants.CONF_FULL_SCREEN, isChecked);
                editor.apply();
            }
        });

        selfLogo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedPreferences = SplashAdActivity.this.getSharedPreferences("setting", 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(Constants.CONF_SELF_LOGO, isChecked);
                editor.apply();
            }
        });

        String[] stringArray = getResources().getStringArray(R.array.splash_id_value);
        placementId = stringArray[0];
        initCallBack();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String[] stringArray = getResources().getStringArray(R.array.splash_id_value);
        placementId = stringArray[position];
        Log.d("lance", "------onItemSelected------" + position + ":" + placementId);
        SharedPreferences sharedPreferences = SplashAdActivity.this.getSharedPreferences("setting", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.CONF_PLACEMENT_ID, placementId);
        editor.apply();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d("lance", "------onNothingSelected------");
    }

    public void ButtonClick(View view) {
        switch (view.getId()) {
            case R.id.bt_load:
                resetCallBackData();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                loadSplashAd();
                break;
            case R.id.bt_show:
                showSplashAd();
                break;
        }
    }

    private void loadSplashAd() {

        initViewGroup(this);

        WMSplashAdRequest adRequest = new WMSplashAdRequest(placementId, String.valueOf(0), null);
        splashAd = new WMSplashAd(this, adRequest);
        splashAd.setSplashAdListener(new WMSplashAdListener() {
            @Override
            public void onSplashAdSuccessPresent(AdInfo adInfo) {
                logCallBack("onSplashAdSuccessPresent", "");
            }

            @Override public void onSplashAdFailToPresent(WindMillError error, String placementId) {

            }

            @Override
            public void onSplashAdSuccessLoad(String placementId) {
                logCallBack("onSplashAdSuccessLoad", "");
            }

            @Override
            public void onSplashAdFailToLoad(WindMillError error, String placementId) {
                logCallBack("onSplashAdFailToLoad", error.toString());
                if (splashLY != null) {
                    splashLY.removeAllViews();
                    splashLY.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSplashAdClicked(AdInfo adInfo) {
                logCallBack("onSplashAdClicked", "");
            }

            @Override
            public void onSplashClosed(AdInfo adInfo) {
                logCallBack("onSplashClosed", "");
                if (splashLY != null) {
                    splashLY.removeAllViews();
                    splashLY.setVisibility(View.GONE);
                }
            }
        });

        splashAd.loadAdOnly();
    }

    private void showSplashAd() {
        if (splashAd != null && splashAd.isReady()) {
//            splashAd.showAd(null);
            splashAd.showAd(splashLY);
        } else {
            Toast.makeText(SplashAdActivity.this, "Ad is not Ready", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetCallBackData() {
        callBackDataList.clear();
        for (int i = 0; i < CallBackInfo.SPLASH_CALLBACK.length; i++) {
            callBackDataList.add(new CallBackItem(CallBackInfo.SPLASH_CALLBACK[i], "", false, false));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashAd != null) {
            splashAd.destroy();
            splashAd = null;
        }
    }
}