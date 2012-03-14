package orz.kassy.boundiokanojo.client;

import orz.kassy.boundiokanojo.client.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class EntryActivity extends Activity implements OnClickListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 合意済ならすぐメインに飛ぶ
        if(AppUtils.loadAgreementFlag(this)) {
            gotoMainActivity();
            return;
        }
        setContentView(R.layout.entry);      
        Button btnOk = (Button) findViewById(R.id.btnEntryOK);
        Button btnNg = (Button) findViewById(R.id.btnEntryNG);
        btnOk.setOnClickListener(this);
        btnNg.setOnClickListener(this);
        
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnEntryNG:
                // 処理をせずに終わる
                finish();
                break;
            case R.id.btnEntryOK:
                // 合意を保存
                AppUtils.saveAgreementFlag(this,true);
                // メインアクティビティに飛ぶ
                gotoMainActivity();
                break;
            default:
                break;
        }
    }
    
    /**
     * メインアクティビティに飛ぶ。そしてこのアクティビティを消す
     */
    private void gotoMainActivity() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }
    
}
