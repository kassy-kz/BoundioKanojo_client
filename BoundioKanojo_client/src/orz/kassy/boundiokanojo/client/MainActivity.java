package orz.kassy.boundiokanojo.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import orz.kassy.boundiokanojo.client.R;
import twitter4j.AsyncTwitter;
import twitter4j.AsyncTwitterFactory;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * メインアクティビティクラス
 * Twitter認証
 * Timetickレシーバ設定
 * mention取得→サーバー処理
 * @author kashimoto
 */
public class MainActivity extends Activity implements OnClickListener {
	private static final int TWITTER_AUTHORIZE = 0;
    private static final String TAG = "ClientMain";
    //ローカルテスト用
    //private static final String SERVER_URL = "http://192.168.0.3:8888/telstore";
    private static final String SERVER_URL = "http://boundio-kanojo.appspot.com/telstore";
    private static final long MIKOKO_USER_ID = 419463310L;
    private static MainActivity mSelf;
	private Twitter mTwitter = null;
	private RequestToken mToken = null;
    private AsyncTwitter mAsyncTwitter;
	private AccessToken mAccessToken = null;
	private String mAuthorizeUrl = "";
	private ProgressDialog mDialog = null;
	private Handler mHandler = new Handler();
    private AuthAsyncTask mTask;
    private TextView mAuthStateText;

    public static MainActivity getInstance() {
        return mSelf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelf = this;
        setContentView(R.layout.main);        
        Button btnTmp = (Button) findViewById(R.id.btnTmpSend);
        btnTmp.setOnClickListener(this);
        mAuthStateText = (TextView) findViewById(R.id.mainAuthStateText);

        // 保存していたAccessToken取得
        mAccessToken = AppUtils.loadAccessToken(this);

        // 認証まだしてない場合だけ認証処理する
        if(mAccessToken == null) {
            mTask = new AuthAsyncTask(this);
            mTask.execute(0);
        // 認証が済んでいる場合はAsyncTwitterオブジェクト作る
        } else {
            mAuthStateText.setText(R.string.main_activity_authsuccess);
            AsyncTwitterFactory factory = new AsyncTwitterFactory();
            mAsyncTwitter = factory.getInstance();
            // mAsyncTwitter.addListener(mAsyncTwitterListener);
            mAsyncTwitter.setOAuthConsumer(AppUtils.CONSUMER_KEY, AppUtils.CONSUMER_SECRET);
            mAsyncTwitter.setOAuthAccessToken(mAccessToken);

            // ワーカースレッドでの作業用に同期Twitterも作っておく
            TwitterFactory factory2 = new TwitterFactory();
            mTwitter = factory2.getInstance();
            mTwitter.setOAuthConsumer(AppUtils.CONSUMER_KEY, AppUtils.CONSUMER_SECRET);
            mTwitter.setOAuthAccessToken(mAccessToken);

        }
    }
    
	@Override
	protected void onStop() {
		super.onStop();
		if(mTwitter != null) mTwitter.shutdown();
		//ImageCache.clear();
	}
	
	/**
	 * 別のActivityから戻ってきた時の処理
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	     // WebViewのOauthページの認証処理から帰ってきたとき
		if(requestCode == TWITTER_AUTHORIZE) {
            // 認証成功
			if(resultCode == 0) {
	            mAuthStateText.setText(R.string.main_activity_authsuccess);
				final String pincode = data.getExtras().getString("pincode");
				mDialog = new ProgressDialog(this);
				mDialog.setMessage(getString(R.string.wait_timeline_message));
				mDialog.setIndeterminate(true);
				mDialog.show();

				// ワーカースレッドでTwitterオブジェクト初期化とかを行う
				new Thread() {
					@Override
					public void run() {
						try {
						    // 認証が成功したあとの処理
							mAccessToken = mTwitter.getOAuthAccessToken(mToken, pincode);
							
							// 用済みインスタンス破棄
							mTwitter.shutdown();
							mTwitter = null;

							// Preferenceにアクセストークンを保存
							AppUtils.saveAccessToken(mSelf, mAccessToken);

							// asynctwitterオブジェクト生成
				            AsyncTwitterFactory factory = new AsyncTwitterFactory();
				            mAsyncTwitter = factory.getInstance();
				            //mAsyncTwitter.addListener(mAsyncTwitterListener);
				            mAsyncTwitter.setOAuthConsumer(AppUtils.CONSUMER_KEY, AppUtils.CONSUMER_SECRET);
				            mAsyncTwitter.setOAuthAccessToken(mAccessToken);
				            
				            // ワーカースレッドでの作業用に同期Twitterも作っておく
				            TwitterFactory factory2 = new TwitterFactory();
				            mTwitter = factory2.getInstance();
                            mTwitter.setOAuthConsumer(AppUtils.CONSUMER_KEY, AppUtils.CONSUMER_SECRET);
                            mTwitter.setOAuthAccessToken(mAccessToken);
				            
				            // サーバーアクセス、電話番号とかtwitterIdとか送信
                            // twitterオブジェクトならscreenNameとれるのにasyncだと取れない不思議
				            //sendPhoneIdToServer(mAsyncTwitter.getScreenName());
                            Log.i("user name = ",mTwitter.getScreenName());
                            sendPhoneIdToServer(mTwitter.getScreenName());
                            
                            // ここで美心をフォローする
                            User user = mTwitter.createFriendship(MIKOKO_USER_ID);
                            if(user == null) {
                                Log.w(TAG,"フォロー失敗");
                            } else {
                                Log.w(TAG,"フォロー成功");
                            }
						} catch(TwitterException e) {
							Log.d("TEST", "Exception", e);
						}
						// UIスレッドを呼び出して後処理
						mHandler.post(mRunnable_List_update);
					}
				}.start();					
			}
		}
	}
	
	/**
	 * サーバーに電話番号とかツイッターIDとか送信
	 * @param twitterId
	 */
	private void sendPhoneIdToServer(String twitterId) {
	    
        // ユーザーの電話番号を取得します
	    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
	    String phoneNumber = telephonyManager.getLine1Number();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("phone",phoneNumber));
        params.add(new BasicNameValuePair("twitter",twitterId));
        postRequest(params);
	} 

	public static void postRequest(List<NameValuePair> params) {
	    try {
	        HttpClient httpClient = new DefaultHttpClient();
	        HttpResponse httpResponse;
	        HttpPost request = new HttpPost(SERVER_URL);
	        request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
	        httpResponse = httpClient.execute(request);
	    } catch (UnsupportedEncodingException e) {
	        e.printStackTrace();
	    } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}     

	// WEB Oauth認証処理が終わってTwitterオブジェクト初期化も終わったあとのUI後処理
    private Runnable mRunnable_List_update = new Runnable() {
        @Override
        public void run() {
            mDialog.dismiss();
        }
    };

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            // 開発用ボタン
            case R.id.btnTmpSend:
                // サーバーアクセス、電話番号とかtwitterIdとか送信
                try {
                    // sendPhoneIdToServer(mTwitter.getScreenName());
                    //mAsyncTwitter.getUserListMembers(arg0, arg1)
                    sendPhoneIdToServer("kassy_dev");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }        
    }
    
    /**
     * 認証処理の非同期タスク　（WebViewでOAuthのページに飛ぶタスク）
     */
    public class AuthAsyncTask extends AsyncTask<Integer, Void, Integer>{
        private Activity mActivity;
        private static final int RESULT_OK = 0;
        private static final int RESULT_NG = -1;
        
        public AuthAsyncTask(Activity activity) {
            mActivity = activity;
        }

        // 認証処理の前処理 これはUIスレッドでの処理
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // ダイアログを表示
            mDialog = new ProgressDialog(mActivity);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setMessage("認証処理をしています...");
            mDialog.setCancelable(true);
            mDialog.show();
        }
        
        // 認証処理（ワーカースレッドでの処理）
        @Override
        protected Integer doInBackground(Integer... arg0) {

            // 保存したAccessToken取得
            mAccessToken = AppUtils.loadAccessToken(mActivity);
            
            // 初回の認証処理
            mTwitter = new TwitterFactory().getInstance();
            mTwitter.setOAuthConsumer(AppUtils.CONSUMER_KEY, AppUtils.CONSUMER_SECRET);
            try {
                mToken = mTwitter.getOAuthRequestToken();
                mAuthorizeUrl = mToken.getAuthorizationURL();
                return RESULT_OK;
            } catch (TwitterException e) {
                e.printStackTrace();
                return RESULT_NG;
            }
        }

        // 認証の後処理 （UIスレッドでの処理 WebViewのOAuthページに飛ぶ）
        @Override
        protected void onPostExecute (Integer result) {
            super.onPostExecute(result);
            if(result == RESULT_OK) {
                if(mDialog != null) {
                    mDialog.dismiss();
                }
                // WebViewを持つアクティビティを呼び出す
                Intent intent = new Intent(mActivity, TwitterAuthorizeActivity.class);
                intent.putExtra(AppUtils.AUTH_URL, mAuthorizeUrl);
                mActivity.startActivityForResult(intent, TWITTER_AUTHORIZE);
            } else if(result == RESULT_NG) {
                if(mDialog != null) {
                    mDialog.dismiss();
                }
                Toast.makeText(mActivity, R.string.twitter_auth_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}