package io.invertase.firebase.admob;

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Activity;
import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import javax.annotation.Nullable;

import io.invertase.firebase.common.ReactNativeFirebaseModule;
import io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent;

import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.buildAdRequest;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.getCodeAndMessageFromAdErrorCode;
import static io.invertase.firebase.admob.ReactNativeFirebaseAdMobCommon.sendAdEvent;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLICKED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_CLOSED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_ERROR;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_LEFT_APPLICATION;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_LOADED;
import static io.invertase.firebase.database.ReactNativeFirebaseAdMobEvent.AD_OPENED;

import androidx.annotation.NonNull;

public class ReactNativeFirebaseAdMobInterstitialModule extends ReactNativeFirebaseModule {
  private static final String SERVICE = "AdMobInterstitial";
  private static SparseArray<InterstitialAd> interstitialAdArray = new SparseArray<>();
  private InterstitialAd mInterstitialAd;

  public ReactNativeFirebaseAdMobInterstitialModule(ReactApplicationContext reactContext) {
    super(reactContext, SERVICE);
  }

  private void sendInterstitialEvent(String type, int requestId, String adUnitId, @Nullable WritableMap error) {
    sendAdEvent(
      ReactNativeFirebaseAdMobEvent.EVENT_INTERSTITIAL,
      requestId,
      type,
      adUnitId,
      error
    );
  }

  @ReactMethod
  public void interstitialLoad(int requestId, String adUnitId, ReadableMap adRequestOptions) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      WritableMap error = Arguments.createMap();
      error.putString("code", "null-activity");
      error.putString("message", "Interstitial ad attempted to load but the current Activity was null.");
      sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
      return;
    }

    MobileAds.initialize(currentActivity, new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {}
    });

    currentActivity.runOnUiThread(() -> {

      AdRequest adRequest = new AdRequest.Builder().build();
      InterstitialAd.load(currentActivity, adUnitId, adRequest, new InterstitialAdLoadCallback() {
        @Override
        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
          super.onAdLoaded(interstitialAd);

          interstitialAdArray.put(requestId, interstitialAd);
          mInterstitialAd = interstitialAd;
          sendInterstitialEvent(AD_LOADED, requestId, adUnitId, null);

          mInterstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
            @Override
            public void onPaidEvent(@NonNull AdValue adValue) {

            }
          });

          mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
              super.onAdClicked();
              sendInterstitialEvent(AD_CLICKED, requestId, adUnitId, null);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
              super.onAdDismissedFullScreenContent();
              sendInterstitialEvent(AD_CLOSED, requestId, adUnitId, null);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
              super.onAdFailedToShowFullScreenContent(adError);
              sendInterstitialEvent(AD_LEFT_APPLICATION, requestId, adUnitId, null);
            }

            @Override
            public void onAdImpression() {
              super.onAdImpression();
              sendInterstitialEvent(AD_OPENED, requestId, adUnitId, null);
            }

            @Override
            public void onAdShowedFullScreenContent() {
              super.onAdShowedFullScreenContent();
              sendInterstitialEvent(AD_OPENED, requestId, adUnitId, null);
            }
          });
        }

        @Override
        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
          super.onAdFailedToLoad(loadAdError);

          WritableMap error = Arguments.createMap();
          String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(loadAdError.getCode());
          error.putString("code", codeAndMessage[0]);
          error.putString("message", codeAndMessage[1]);
          sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
        }
      });

//      InterstitialAd interstitialAd = new InterstitialAd(currentActivity);
//      interstitialAd.setAdUnitId(adUnitId);
//
//      // Apply AdRequest builder
//      interstitialAd.loadAd(buildAdRequest(adRequestOptions));
//
//      interstitialAd.setAdListener(new AdListener() {
//        @Override
//        public void onAdLoaded() {//done
//          sendInterstitialEvent(AD_LOADED, requestId, adUnitId, null);
//        }
//
//        @Override
//        public void onAdFailedToLoad(int errorCode) {//done
//          WritableMap error = Arguments.createMap();
//          String[] codeAndMessage = getCodeAndMessageFromAdErrorCode(errorCode);
//          error.putString("code", codeAndMessage[0]);
//          error.putString("message", codeAndMessage[1]);
//          sendInterstitialEvent(AD_ERROR, requestId, adUnitId, error);
//        }
//
//        @Override
//        public void onAdOpened() {//done
//          sendInterstitialEvent(AD_OPENED, requestId, adUnitId, null);
//        }
//
//        //done
//        @Override
//        public void onAdClicked() {//done
//          sendInterstitialEvent(AD_CLICKED, requestId, adUnitId, null);
//        }
//
//        @Override
//        public void onAdLeftApplication() {//done
//          sendInterstitialEvent(AD_LEFT_APPLICATION, requestId, adUnitId, null);
//        }
//
//        @Override
//        public void onAdClosed() {//done
//          sendInterstitialEvent(AD_CLOSED, requestId, adUnitId, null);
//        }
//      });

    });
  }

  @ReactMethod
  public void interstitialShow(int requestId, ReadableMap showOptions, Promise promise) {
    if (getCurrentActivity() == null) {
      rejectPromiseWithCodeAndMessage(promise, "null-activity", "Interstitial ad attempted to show but the current Activity was null.");
      return;
    }
    getCurrentActivity().runOnUiThread(() -> {
      InterstitialAd interstitialAd = interstitialAdArray.get(requestId);
      if (interstitialAd == null) {
        rejectPromiseWithCodeAndMessage(promise, "null-interstitialAd", "Interstitial ad attempted to show but its object was null.");
        return;
      }

      if (showOptions.hasKey("immersiveModeEnabled")) {
        interstitialAd.setImmersiveMode(showOptions.getBoolean("immersiveModeEnabled"));
      } else {
        interstitialAd.setImmersiveMode(false);
      }

      if (interstitialAd != null) {
        interstitialAd.show(getCurrentActivity());
        promise.resolve(null);
      } else {
        rejectPromiseWithCodeAndMessage(promise, "not-ready", "Interstitial ad attempted to show but was not ready.");
      }
    });
  }
}
