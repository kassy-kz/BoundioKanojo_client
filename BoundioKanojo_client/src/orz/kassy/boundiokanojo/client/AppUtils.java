package orz.kassy.boundiokanojo.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import twitter4j.auth.AccessToken;

public class AppUtils {
    public static final String PREF_FILE_NAME = "pref_file";
    public static final String CONSUMER_KEY = "RKGCMJeAVzR7fCHun8tIQQ";
    public static final String CONSUMER_SECRET = "9aQfuzFcwqnRZTFgO3dQIZb7ZZymFqxa1xBZfunPm5A";
    public static final String AGREEMENT_FLAG = "agreement_flag";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ACCESS_TOKEN_SECRET = "access_token_secret";
    public static final String AUTH_URL = "authurl";

    /**
     * ユーザーのこのアプリ使用合意フラグを保存する
     * @param context コンテキスト
     * @param agFlag フラグ
     */
    public static void saveAgreementFlag(Context context, boolean agFlag) {
        SharedPreferences shPref = context.getSharedPreferences(AppUtils.PREF_FILE_NAME,Context.MODE_PRIVATE);
        Editor e = shPref.edit();
        e.putBoolean(AppUtils.AGREEMENT_FLAG, agFlag);
        e.commit();
    }
    
    /**
     * ユーザーのアプリ使用合意フラグを返す
     * @param context コンテキスト
     * @return trueなら合意済
     */
    public static boolean loadAgreementFlag(Context context) {
        SharedPreferences shPref = context.getSharedPreferences(AppUtils.PREF_FILE_NAME,Context.MODE_PRIVATE);
        boolean agFlag = shPref.getBoolean(AGREEMENT_FLAG, false);
        return agFlag;
    }
    
    /**
     * アクセストークンを呼び出す
     * @param context コンテキスト
     * @return アクセストークン
     */
    public static AccessToken loadAccessToken(Context context) {
        SharedPreferences shPref = context.getSharedPreferences(AppUtils.PREF_FILE_NAME,Context.MODE_PRIVATE);
        String token       = shPref.getString(ACCESS_TOKEN, null);
        String tokenSecret = shPref.getString(ACCESS_TOKEN_SECRET, null);

        if(token != null && tokenSecret != null) {
            return new AccessToken(token, tokenSecret);
        } else {
            return null;
        }
    }

    /**
     * アクセストークンを保存する
     * @param context コンテキスト
     * @param accessToken アクセストークン
     */
    public static void saveAccessToken(Context context, AccessToken accessToken) {
        SharedPreferences shPref = context.getSharedPreferences(AppUtils.PREF_FILE_NAME,Context.MODE_PRIVATE);
        String token       = accessToken.getToken();
        String tokenSecret = accessToken.getTokenSecret();
        Editor e = shPref.edit();
        e.putString(AppUtils.ACCESS_TOKEN, token);
        e.putString(AppUtils.ACCESS_TOKEN_SECRET, tokenSecret);
        e.commit();
    }

}
