package com.nuhkoca.trippo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nuhkoca.trippo.BuildConfig;
import com.nuhkoca.trippo.R;
import com.nuhkoca.trippo.TrippoApp;
import com.nuhkoca.trippo.helper.Constants;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static android.content.Context.MODE_PRIVATE;

public class SharedPreferenceUtil {

    private static SharedPreferences mSharedPref;
    private static String mFirebaseToken;
    private static int mIsNotifyTheDevice;

    private static SharedPreferenceUtil INSTANCE;

    public static synchronized SharedPreferenceUtil getInstance() {
        mSharedPref = TrippoApp.getInstance().getSharedPreferences(Constants.TRIPPO_SHARED_PREF, MODE_PRIVATE);

        if (INSTANCE == null) {
            INSTANCE = new SharedPreferenceUtil();
        }

        return INSTANCE;
    }

    public static synchronized SharedPreferenceUtil getInstance(String firebaseToken, int isNotifyTheDevice) {
        mSharedPref = TrippoApp.getInstance().getSharedPreferences(Constants.TRIPPO_SHARED_PREF, MODE_PRIVATE);

        mFirebaseToken = firebaseToken;
        mIsNotifyTheDevice = isNotifyTheDevice;

        if (INSTANCE == null) {
            INSTANCE = new SharedPreferenceUtil();
        }

        return INSTANCE;
    }

    public boolean isFirstRun() {
        boolean isFirst = mSharedPref.getBoolean(Constants.FIRST_RUN_KEY, true); //false
        boolean isOnboardingPassed = mSharedPref.getBoolean(Constants.ONBOARD_PASSED_KEY, false);

        if (isFirst && BuildConfig.VERSION_CODE == 1 && !isOnboardingPassed) {
            mSharedPref.edit().putBoolean(Constants.FIRST_RUN_KEY, true).apply();
            return true;
        }

        return false;
    }

    public void checkAndSaveToken() {
        FirebaseFirestore db = TrippoApp.provideFirestore();

        if (!TextUtils.isEmpty(getDocumentId())) {
            db.collection(Constants.FIRESTORE_COLLECTION_NAME)
                    .document(getDocumentId())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            DocumentSnapshot documentSnapshot = task.getResult();

                            if (documentSnapshot.exists()) {
                                Timber.d("document exists");
                            } else {
                                storeToFirestore();
                                Timber.d("new data added");
                            }
                        }
                    });
        } else {
            storeToFirestore();
        }
    }

    public void storeToFirestore() {
        FirebaseFirestore db = TrippoApp.provideFirestore();

        Map<String, Object> userToken = new HashMap<>();
        userToken.put(Constants.FIRESTORE_KEY, mFirebaseToken);
        userToken.put(Constants.FIRESTORE_PUSH_KEY, mIsNotifyTheDevice);
        userToken.put(Constants.FIRESTORE_DEVICE_MODEL_KEY, DeviceUtils.model());
        userToken.put(Constants.FIRESTORE_DEVICE_PRODUCT_KEY, DeviceUtils.product());
        userToken.put(Constants.FIRESTORE_DEVICE_API_KEY, DeviceUtils.api());
        userToken.put(Constants.FIRESTORE_DEVICE_DEVICE_KEY, DeviceUtils.device());
        userToken.put(Constants.FIRESTORE_DEVICE_BRAND_KEY, DeviceUtils.brand());

        db.collection(Constants.FIRESTORE_COLLECTION_NAME)
                .add(userToken)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Timber.d("token saved successfully.");
                        Timber.d("Token: %s", mFirebaseToken);

                        addDocumentIdToSharedPreference(documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.d(e);
                    }
                });
    }

    private void addDocumentIdToSharedPreference(String documentId) {
        SharedPreferences.Editor editor = mSharedPref.edit();

        editor.putString(Constants.FIRESTORE_DOC_ID_KEY, documentId);

        Timber.d("Document Id %s", documentId);

        editor.apply();
    }

    private String getDocumentId() {
        return mSharedPref.getString(Constants.FIRESTORE_DOC_ID_KEY, "");
    }

    public void addTokenToSharedPreference(String token) {
        SharedPreferences.Editor editor = mSharedPref.edit();

        editor.putString(Constants.FIRESTORE_TOKEN_KEY, token);

        Timber.d("Token %s", token);

        editor.apply();
    }

    public String getTokenFromSharedPreference() {
        return mSharedPref.getString(Constants.FIRESTORE_TOKEN_KEY, "");
    }

    public String getIsNotifyMeFromSharedPreference() {
        return mSharedPref.getString(Constants.FIRESTORE_IS_NOTIFY_ME_KEY, "0");
    }

    public void updateNotification(int isNotifyTheDevice) {
        FirebaseFirestore db = TrippoApp.provideFirestore();

        db.collection(Constants.FIRESTORE_COLLECTION_NAME)
                .document(getDocumentId())
                .update(Constants.FIRESTORE_PUSH_KEY, isNotifyTheDevice)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.d("notification update success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.d(e);
                    }
                });
    }

    public void updateToken(String token) {
        FirebaseFirestore db = TrippoApp.provideFirestore();

        db.collection(Constants.FIRESTORE_COLLECTION_NAME)
                .document(getDocumentId())
                .update(Constants.FIRESTORE_TOKEN_KEY, token)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Timber.d("token update success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Timber.d(e);
                    }
                });
    }

    public static String loadToursScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.tours_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadActivitiesScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.activities_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadMultiDayToursScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.multi_day_tours_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadDayTripsScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.day_trips_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadCityWalkingScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.city_walking_key), context.getString(R.string.seven_and_greater_value));
    }


    public static String loadSightseeingScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.sightseeing_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadEatAndDrinkScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.eat_drink_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadNightlifeScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.nightlife_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadHotelScore(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.hotel_key), context.getString(R.string.seven_and_greater_value));
    }

    public static String loadBookableOption(Context context, SharedPreferences sharedPreferences) {
        boolean isBookable = sharedPreferences.getBoolean(context.getString(R.string.bookable_key), false);

        if (isBookable) {
            return context.getString(R.string.bookable_enabled_value);
        } else {
            return context.getString(R.string.bookable_disabled_value);
        }
    }

    public static boolean isInternalBrowserEnabled(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(context.getString(R.string.webview_key), false);
    }

    public static String loadTTSOption(Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(R.string.tts_key), context.getString(R.string.tts_us_language_value));
    }
}