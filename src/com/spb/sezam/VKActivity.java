package com.spb.sezam;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class VKActivity extends Activity {

	 private static final String[] myScope = new String[] {
         VKScope.FRIENDS,
         VKScope.MESSAGES,
         VKScope.OFFLINE
	 };
	
	 
	 private final VKSdkListener sdkListener = new VKSdkListener() {
	        @Override
	        public void onCaptchaError(VKError captchaError) {
	            new VKCaptchaDialog(captchaError).show();
	        }

	        @Override
	        public void onTokenExpired(VKAccessToken expiredToken) {
	            VKSdk.authorize(myScope);
	        }

	        @Override
	        public void onAccessDenied(final VKError authorizationError) {
	            new AlertDialog.Builder(VKUIHelper.getTopActivity())
	                    .setMessage(authorizationError.toString())
	                    .show();
	        }

			@Override
			public void onReceiveNewToken(VKAccessToken newToken) {
				Log.e("Entering the app", "Entering the app! New token: " + newToken.userId);
				startActivity(MessageActivity.class);
			}

			@Override
			public void onAcceptUserToken(VKAccessToken token) {
				Log.e("onAcceptUserToken", "onAcceptUserToken don't know when");
				startActivity(MessageActivity.class);
			}
			
			@Override
			public void onRenewAccessToken(VKAccessToken token) {
				Log.e("onRenewAccessToken", "onRenewAccessToken don't know when");
			}
	    };
	    
	    
	 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk);
        
        //initialize Vk SDK
        VKUIHelper.onCreate(this);
        VKSdk.initialize(sdkListener, "4619202");
        
        Button b = (Button)findViewById(R.id.sign_in_button);
        //predefined in .xml as Войти
        if (VKSdk.wakeUpSession()) {
        	Log.e("wakeUp","wakeUp");
            startActivity(MessageActivity.class);
            //skzbi hamar shat el a
            b.setText("Выход!");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                	VKSdk.logout();
                	((Button)view).setText("Войти");
                }
            });
            //
            return;
        }
        
        
        
        b.setOnClickListener(new View.OnClickListener() {
        	
            @Override
            public void onClick(View view) {
            	if (VKSdk.isLoggedIn()){
            		Log.e("Uje logged in","Uje Loged in");
            	}
                VKSdk.authorize(myScope);
                if (VKSdk.isLoggedIn()){
            		Log.e("Uje logged in2","Uje Loged in2");
            	}
            }
        });
        
    }

	@Override
	protected void onResume() {
		super.onResume();
		VKUIHelper.onResume(this);
		
		Button b = (Button)findViewById(R.id.sign_in_button);
		if (VKSdk.isLoggedIn()) {
			b.setText("Выход!");
			b.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					VKSdk.logout();
					Button b = ((Button) view);
					b.setText("Войти");
					//bad code, need to be changed
					b.setOnClickListener(new View.OnClickListener() {
			            @Override
			            public void onClick(View view) {
			                VKSdk.authorize(myScope);
			            }
			        });
				}
			});
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VKUIHelper.onDestroy(this);
		if (VKSdk.isLoggedIn()){
			Log.e("Uje logged in","Uje Loged in destroyi vaxt");
        } 
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data);
	}

	private void startActivity(Class<? extends Activity> a) {
		Intent startNewActivityOpen = new Intent(this, a);
		startActivityForResult(startNewActivityOpen, 0);
	}
   
}



