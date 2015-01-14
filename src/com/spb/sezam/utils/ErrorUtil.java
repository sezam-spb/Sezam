package com.spb.sezam.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.vk.sdk.api.VKError;

public class ErrorUtil {

	private ErrorUtil(){
	}
	
	public static void showError(Context context, VKError error) {
		String message = error.errorMessage;
		if (message == null && error.apiError != null) {
			message = error.apiError.errorMessage;
		}

		if (error.httpError != null) {
			Log.e("Test", "Error in request: " + error.httpError.getMessage(), error.httpError);
		} else {
			Log.e("VK error", "Error in request: " + message);
			// showError(context, message);
		}
	}
	
	public static void showError(Context context, String message){
		new AlertDialog.Builder(context)
		        .setMessage(message)
		        .setPositiveButton("OK", null)
		        .show();
	}

}


