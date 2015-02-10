package com.spb.sezam;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.spb.sezam.NavigationDrawerFragment.NavigationDrawerCallbacks;
import com.spb.sezam.adapters.GridViewAdapter;
import com.spb.sezam.adapters.GridViewHolder;
import com.spb.sezam.adapters.GroupAdapter;
import com.spb.sezam.management.ElementType;
import com.spb.sezam.management.GroupPictogram;
import com.spb.sezam.management.NameManager;
import com.spb.sezam.management.Pictogram;
import com.spb.sezam.management.PictogramManager;
import com.spb.sezam.utils.ErrorUtil;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.LinearLayout;

public class MessageActivity extends ActionBarActivity implements NavigationDrawerCallbacks, IPictogramHolder{
	
	private NavigationDrawerFragment mNavigationDrawerFragment;

	public static final String ICON_SPLIT_SYMBOLS = "|_";
	
	private static final int MESSAGE_RECIEVE_COUNT = 7;
	
	private List<String> messageToSend = new ArrayList<String>();
	//private List<Pictogram> pictogramsToSend = new ArrayList<>(); //maybe can messageToSend messageToSend
	private JSONArray allMessages = new JSONArray();
	private String activeUserName = null;
	private int activeUserId;
	
	private GroupAdapter firstLevelGroupAdapter = null;
	private GroupAdapter subGroupAdapter = null;
	private GridViewAdapter gridViewAdapter = null;
	//private MessageAdapter newMessageAdapter = null;
	
	private GridView subGroupsView = null;
	private GridView pictogramsGridView = null;
	//in pixels
	private int historyImageSize = 0;
	
	private Runnable recieveMessagesRunnable = null;
	/** For all Users */
	private final Handler handler = new Handler();
	
	private Menu menu;
	
	private View.OnClickListener onPictogramClickListener ;

	
	//--------------------------------VK listeners-----------------------------//
	private VKRequestListener messageSendListener  = new VKRequestListener(){

		@Override
		public void onComplete(VKResponse response) {
			
			LinearLayout formLayout = (LinearLayout) findViewById(R.id.linearLayout1);
			formLayout.removeAllViews();
			
			/*pictogramsToSend.clear();
			newMessageAdapter.updateView(pictogramsToSend);*/
			
			messageToSend.clear();
			Toast showSent = Toast.makeText(getApplicationContext(), "Сообщение отправлено", Toast.LENGTH_SHORT);
			showSent.show();
			recieveMessageHistory(MESSAGE_RECIEVE_COUNT);
		}

		@Override
		public void onError(VKError error) {
			ErrorUtil.showError(MessageActivity.this, error);
		}
		
	};
	
	private VKRequestListener messageRecieveListener  = new VKRequestListener(){

		@Override
		public void onComplete(VKResponse response) {    
	        //List<String[]> ourMessages = null; 
	        try {
	        	JSONArray messages = response.json.getJSONObject("response").getJSONArray("items");
	        	//ourMessages = filterMessages(messages, true);
	        	//show images
	            //decodeTextToImages(ourMessages);
	        	
	            //must be only when Activity starts
	        	
	        	JSONArray newMessages = findNewMessages(allMessages, messages);
	        	int length = newMessages.length();
	        	if(length != 0){
		            showHistory(newMessages);
		            scorllDown((ScrollView)findViewById(R.id.scrollView1));
		            if(isThereRecieved(newMessages) && newMessages != messages){
		            	Toast showSent = Toast.makeText(getApplicationContext(), "Получено новое сообщение", Toast.LENGTH_SHORT);
		    			showSent.show();
		            }
		            
		            //mark received new messages as read
		            //should be some trick here to prevent mark as read when drawer is opened
		            //if(!mNavigationDrawerFragment.isDrawerOpen()){
			            StringBuilder messagsIds = new StringBuilder();
			            //this approach is not good for first call
			            for(int i=0; i < length; i++){
			            	int messId = newMessages.getJSONObject(i).getInt("id");
			            	messagsIds.append(messId);
			            	if(i != (length - 1)){
			            		messagsIds.append(",");
			            	}
			            }
			            VKRequest request = new VKRequest("messages.markAsRead", VKParameters.from(
				        		"message_ids", messagsIds.toString()));
						request.executeWithListener(markAsReadListener);
						//
		        	//}
		            
	        	}
	            allMessages = messages;
	            //
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onError(VKError error) {
			Log.e("Message recieve", "Error on Message recieve");
			ErrorUtil.showError(MessageActivity.this, error);
		}
	};
	
	private VKRequestListener markAsReadListener  = new VKRequestListener(){
		
		@Override
		public void onError(VKError error) {
		//	ActivityUtil.showError(MessageActivity.this, error);
		}
	};
	//---------------------------End of VK listenres---------------------------//
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message);
		
		subGroupsView = (GridView)findViewById(R.id.subGroups_view);
		//subGroupsView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);
		pictogramsGridView = (GridView)findViewById(R.id.gridView1);
		
		historyImageSize = (int)(getResources().getDimension(R.dimen.new_message_height)/1.2);
		
		new ManagersInitializer().execute();
		initImageLoader();
		View view = findViewById(R.id.container);
		view.setVisibility(View.INVISIBLE); //or gone
		//createButtonsFromDrawables();
		//createButtonsFromAssets();
		initOnPictogramClickListener();
		//PictogramManager.getInstance().init(MessageActivity.this);
		
		
		/*ImageButton btn1 = (ImageButton)findViewById(R.id.imageButton1);
        btn1.setOnClickListener(this);

        ImageButton btn2 = (ImageButton)findViewById(R.id.imageButton2);
        btn2.setOnClickListener(this);

        ImageButton btn3 = (ImageButton)findViewById(R.id.imageButton3);
        btn3.setOnClickListener(this);

        ImageButton btn4 = (ImageButton)findViewById(R.id.imageButton4);
        btn4.setOnClickListener(this);*/

		mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		
		mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
		
//        Intent intent = getIntent();
        //String activefriend = intent.getStringExtra(FriendsActivity.EXTRA_MESSAGE);
        
//		try {
//			JSONObject activeFriend = new JSONObject(intent.getStringExtra(FriendsActivity.EXTRA_MESSAGE));
//			activeFriendName = activeFriend.getString("first_name") + " " + activeFriend.getString("last_name");
//			activeFriendId = activeFriend.getInt("id");
//		} catch (JSONException e) {
//			// TODO To be handled
//			e.printStackTrace();
//		}
        
        /*TextView txt = (TextView)findViewById(R.id.textView1);
        txt.setText(activeFriendName);*/
        //scorllDown((ScrollView)findViewById(R.id.scrollView1));
        //decodeTextToImages();
        
        //up button for actionbar
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#aaaaaa")));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		/*case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;*/
		case R.id.action_exit:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Вы уверены?").setPositiveButton("Да", dialogClickListener).setNegativeButton("Нет", dialogClickListener).show();
			return true;
		case R.id.action_email:
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"annasezam@gmail.com"});
			i.putExtra(Intent.EXTRA_SUBJECT, "Письмо администратору");
			i.putExtra(Intent.EXTRA_TEXT   , "\nОтправлено с приложения Sezam");
			try {
			    startActivity(Intent.createChooser(i, "Send mail..."));
			} catch (android.content.ActivityNotFoundException ex) {
			    Toast.makeText(MessageActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.action_about:
			AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
			aboutBuilder.setMessage(R.string.about_app)
		       .setCancelable(false)
		       .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                return;
		           }
		       });
			AlertDialog alert = aboutBuilder.create();
			alert.show();
			return true;
		/*case R.id.action_message:
			if(unReadDialogsCount > 0){
				//should be changed
				//NavUtils.navigateUpFromSameTask(this);
				mNavigationDrawerFragment.openDrawer();
				return true;
			}*/
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which){
	        case DialogInterface.BUTTON_POSITIVE:
	        	Log.e("token=", VKSdk.getAccessToken().userId);
	        	VKSdk.logout();
	        	
	        	// ------------------------BAD COPY-----------------------
	        	if(handler != null){
	    			handler.removeCallbacks(recieveMessagesRunnable);
	    			//handler.removeCallbacks(checkUnreadMessagesRunnable);
	    		}
	        	startActivity(VKActivity.class);
	        	setContentView(R.layout.activity_vk);

				Button b = (Button) findViewById(R.id.sign_in_button);
				// predefined in .xml as Войти
				if (VKSdk.wakeUpSession()) {
					Log.e("wakeUp", "wakeUp");
					//startActivity(FriendsActivity.class);
					// skzbi hamar shat el a
					//b.setText("Выход!");
					b.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							VKSdk.logout();
							((Button) view).setText("Войти");
						}
					});
					//
					return;
				}

				b.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						if (VKSdk.isLoggedIn()) {
							Log.e("Uje logged in", "Uje Loged in");
						}
						 String[] myScope = new String[] {
					         VKScope.FRIENDS,
					         VKScope.MESSAGES,
					         VKScope.OFFLINE
						 };						
						VKSdk.authorize(myScope);
						if (VKSdk.isLoggedIn()) {
							Log.e("Uje logged in2", "Uje Loged in2");
						}
					}
				});
				//// -------------------------------------------
	            
	            
	            //Yes button clicked
	            break;

	        case DialogInterface.BUTTON_NEGATIVE:
	            //No button clicked
	            break;
	        }
	    }
	};	
	
	@Override
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
		this.menu = menu;
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_message_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	public void showHistory(JSONArray messages) throws JSONException{
		String messageString = null;
		JSONObject messageJson = null;
		String[] messageArr = null;
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		
		for (int i = messages.length() - 1; i >= 0; i--) {
			messageJson = messages.getJSONObject(i);
			TextView nameView = new TextView(MessageActivity.this);
			if(messageJson.getInt("out") == 1){
				nameView.setText("Я");
				nameView.setTextColor(Color.BLACK);
			} else {
				nameView.setText(activeUserName);
				nameView.setTextColor(Color.BLUE);
			}
			nameView.setTypeface(null, Typeface.BOLD);
			historyLayout.addView(nameView);
			
			messageString = messageJson.getString("body").trim();
			messageArr =  messageString.split("\\" + ICON_SPLIT_SYMBOLS);
			
			LinearLayout messageLinerLayout = new LinearLayout(MessageActivity.this);
			HorizontalScrollView messageScrollView = new HorizontalScrollView(MessageActivity.this);
			historyLayout.addView(messageScrollView);
			messageScrollView.addView(messageLinerLayout);
			
			//Analyze message parts
			for(String text : messageArr){
				showTextWithImages(text, messageLinerLayout);
			}
		}
	}
	
	/**
	 * Shows text into the history layout as it is. Should be called if there is no info about image icon in the text.
	 * @param text Text to be shown
	 * @param lLayout {@link TextView} with {@code 'text'} parameter will be added to this layout
	 */
	private void showTextAsString(String text, LinearLayout lLayout) {
		//if there is no info about image icon in the text
		
		if(text == null || "".equals(text)){
			return;
		}
		
		TextView textView = new TextView(MessageActivity.this);
		textView.setText(text);
		lLayout.addView(textView);
	}
	
	@Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	Log.w("mtav", "mtav ste");
    }
	
	private void showTextWithImages(String text, LinearLayout lLayout){
		if(text == null || "".equals(text)){
			return;
		}
		
		ImageView image = new ImageView(MessageActivity.this);
		setImageViewSize(image, historyImageSize, 2);
		
		String path = NameManager.getInstance().getFileEngName(text);
		if(path != null){
			String pathWithAssets = "assets://" + PictogramManager.BASE_FOLDER + File.separator + path ;
			ImageLoader.getInstance().displayImage(pathWithAssets, image);
			lLayout.addView(image);
		} else {
			showTextAsString(text, lLayout);
		}
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		VKUIHelper.onResume(this);
	}
	
	public void backButton(View v){
        LinearLayout formLayout = (LinearLayout)findViewById(R.id.linearLayout1);
        if(messageToSend.size() != 0){
        	formLayout.removeViewAt(formLayout.getChildCount() - 1 );
        	messageToSend.remove(messageToSend.size() - 1);
        	HorizontalScrollView hView = (HorizontalScrollView)findViewById(R.id.new_mess_scroll);
        	scrollRight(hView);
        	
        	/*pictogramsToSend.remove(pictogramsToSend.size() - 1);
        	newMessageAdapter.updateView(pictogramsToSend);*/
        }
	}	
	
	public void sendMessage(View v){
		if(messageToSend.size() > 0){
	        StringBuilder messageString = new StringBuilder();
	        for(String msg : messageToSend){
	        	messageString.append(msg);
	        }
	        long guId = new Date().getTime();
	        VKRequest request = new VKRequest("messages.send", VKParameters.from(
		        		"user_id", String.valueOf(activeUserId), 
		        		"message", messageString.toString(), "guid", guId));
			request.executeWithListener(messageSendListener);
		}
		
		//test for picture
		
		 //VKApi.uploadWallPhotoRequest(image, userId, groupId)
		 
//		VKRequest request = new VKRequest("photos.getMessagesUploadServer");
//		request.executeWithListener(messageSendListener);
	}
	
	public void recieveMessageHistory(int messagesCount){
		if(messagesCount > 0){
			VKRequest request = new VKRequest("messages.getHistory", VKParameters.from("user_id", activeUserId, "count", messagesCount));
			request.executeWithListener(messageRecieveListener);
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
			Date date = new Date();
			Log.i("Time messages", "Messages recive at " + sdf.format(date));
		}
	}
	
	
	public void addImageNameToSendMessages(String imageName){
		messageToSend.add(ICON_SPLIT_SYMBOLS + imageName + ICON_SPLIT_SYMBOLS); 
	}

	private void scorllDown(final ScrollView view) {
		setScrollViewDirection(view, ScrollView.FOCUS_DOWN);
	}
	
	private void scrollRight(final HorizontalScrollView view){
		setScrollViewDirection(view, ScrollView.FOCUS_RIGHT);
	}
	
	private void setScrollViewDirection(final ScrollView view, final int direction){
		view.post(new Runnable() {
	        @Override
	        public void run() {
	        	view.fullScroll(direction);
	        }
	    });
	}
	
	private void setScrollViewDirection(final HorizontalScrollView view, final int direction){
		view.post(new Runnable() {
	        @Override
	        public void run() {
	        	view.fullScroll(direction);
	        }
	    });
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(handler != null){
			handler.removeCallbacks(recieveMessagesRunnable);
			//handler.removeCallbacks(checkUnreadMessagesRunnable);
		}
	}

	private void removeMessageHistory(){
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		historyLayout.removeAllViews();
	}
	
	private void recieveMessagePeriodicly() {
		recieveMessagesRunnable = new Runnable() {
			public void run() {
				recieveMessageHistory(MESSAGE_RECIEVE_COUNT);
				handler.postDelayed(this, 5000);
			}
		};
		
		handler.postDelayed(recieveMessagesRunnable, 5000);
	}
	
	
	
	public JSONArray findNewMessages(JSONArray oldList, JSONArray newList) throws JSONException{
		if(newList == null || newList.length() == 0){
			return new JSONArray();
		}
		if(oldList == null || oldList.length() == 0){
			return newList;
		}
		
		//need to find oldList[0] in newList
		JSONArray onlyNew = new JSONArray();
		for(int i = 0; i < newList.length(); i++){
			//we believe in VK API that every message should have its unique id...
			JSONObject messageInNew = newList.getJSONObject(i);
			if(messageInNew.getInt("id") == oldList.getJSONObject(0).getInt("id")){
				break;
			} else {
				onlyNew.put(messageInNew);
			}
		}
		return onlyNew;
	}
	
	/**
	 * Returns {@code true} if there is any received message in the list, otherwise returns {@code false} 
	 * @param messages The list of messages to be check
	 * @return {@code boolean}
	 * @throws JSONException
	 */
	private boolean isThereRecieved(JSONArray messages) throws JSONException{
		JSONObject message = null;
		for (int i = 0; i < messages.length(); i++) {
			message = messages.getJSONObject(i);
			if(message.getInt("out") == 0){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Uses JAVA reflection
	 */
	/*private void createButtonsFromDrawables(){
		//using reflection
		LinearLayout lLayout = (LinearLayout) findViewById(R.id.linearLayout2);
		Field[] drawableFields = R.drawable.class.getFields();
		int resId;
		for(Field field : drawableFields){
			String fieldName = field.getName();
			if(fieldName.startsWith("image_") && !fieldName.endsWith("thumb")){
				try {
					resId = field.getInt(null);
					ImageButton btn = new ImageButton(MessageActivity.this);
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					params.leftMargin = 0;
					
					btn.setImageResource(resId);
					btn.setContentDescription(fieldName);
					btn.setLayoutParams(params);
					
					btn.setOnClickListener(new View.OnClickListener() {
			            @Override
			            public void onClick(View view) {
			            	LinearLayout piktogram = (LinearLayout)findViewById(R.id.linearLayout1);
			        		ImageView image = new ImageView(MessageActivity.this);
			        		
			        		String bgResourceName = view.getContentDescription() + "_thumb";
			        		addImageNameToSendMessages(bgResourceName);
			        		try {
								int bgResourceId = R.drawable.class.getField(bgResourceName).getInt(null);
								image.setBackgroundResource(bgResourceId);
			        		} catch (IllegalAccessException
									| IllegalArgumentException
									| NoSuchFieldException e) {
								e.printStackTrace();
							}
			    	        
			    	        piktogram.addView(image);
			            }
			        });
					
					lLayout.addView(btn);
					
				} catch (IllegalAccessException | IllegalArgumentException e) {
					e.printStackTrace();
					continue;
				}
			}
		}
		
	}*/
	
	
	
	/**
	 * Uses assets
	 */
	private void createButtonsFromAssets(){
		GridView lLayout = (GridView) findViewById(R.id.gridView1);
		
		/*LinearLayout firstLevelGorups = (LinearLayout)findViewById(R.id.linearLayout_groups);
		List<Pictogram> list = PictogramManager.getInstance().init(MessageActivity.this).getPictograms();
		
		NameManager nManager = NameManager.getInstance();
		XmlPullParser parser = getResources().getXml(R.xml.base);
		nManager.init(parser);
		
		for(Pictogram pic : list){
			Button group = new Button(MessageActivity.this);
			String ruName = nManager.getGroupRuName(pic.getPath());
			group.setText(ruName);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 1.0f);
			group.setLayoutParams(params);
			firstLevelGorups.addView(group);
		}*/
		
		//adapter
		//lLayout.setAdapter(new GridViewAdapter(MessageActivity.this, MessageActivity.this, list));
		
		/*try {
			for(String name : am.list("test")){
				ImageButton btn = new ImageButton(MessageActivity.this);
				
				//Bitmap a = BitmapFactory.decodeStream(am.open(name));
				BitmapDrawable bd = new BitmapDrawable(getResources(), am.open("test"+ File.separator + name));
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.leftMargin = 0;
				
				//btn.setBackground(bd);
				btn.setImageDrawable(bd);
				btn.setLayoutParams(params);
				lLayout.addView(btn);
				
				//now in other clas, use it
				String pattern = "[^\\.]([^.]*)$";
			    Pattern afterLastDot = Pattern.compile(pattern);
			    Matcher m = afterLastDot.matcher(name);
			    if(m.find()){
					name = m.group(0);
			    }
				btn.setContentDescription(name); 
				
				btn.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	LinearLayout piktogram = (LinearLayout)findViewById(R.id.linearLayout1);
		        		ImageView image = new ImageView(MessageActivity.this);
		        		
		        		String bgResourceName = (String)view.getContentDescription();
		        		addImageNameToSendMessages(bgResourceName);
		        		
		        		AssetManager am = getAssets();
		        		try {
			        		BitmapDrawable bd = new BitmapDrawable(getResources(), am.open("test" + File.separator + bgResourceName));
			        		
			        		image.setBackground(bd);
			        		piktogram.addView(image);
							
		        		} catch (IOException e) {
							e.printStackTrace();
						}
		            }
		        });
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		VKUIHelper.onDestroy(this);
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
		
	
	/*private void decodeTextToImages(List<String[]> messages){
	LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
	String[] message = null;
	for(int i = messages.size() - 1; i >= 0; i--){
		message = messages.get(i);
		LinearLayout innerLayout = new LinearLayout(MessageActivity.this);
		historyLayout.addView(innerLayout);
		
		//tufta mas
		ImageView image = null;
		for(String word : message){
			image = new ImageView(MessageActivity.this); 
			if("чувствовать".equals(word)){
				image.setBackgroundResource(R.drawable.image_1_thumb);
			} else if("я".equals(word)){
				image.setBackgroundResource(R.drawable.image_2_thumb);
			} else if("хорошо".equals(word)){
				image.setBackgroundResource(R.drawable.image_3_thumb);
			} else if("чувствовать себя".equals(word)){
				image.setBackgroundResource(R.drawable.image_4_thumb);
			}
			innerLayout.addView(image);
		}
		//
	}
}*/
	
	/*public List<String[]> filterMessages(JSONArray messages, boolean incomeOnly) throws JSONException{
	String message = null;
	List<String[]> ourMessages = new ArrayList<String[]>();
	JSONObject messageJson = null;
	
	for(int i=0; i < messages.length(); i++ ){
		messageJson = messages.getJSONObject(i);
		int out = messageJson.getInt("out");
		
		if(incomeOnly && out == 1){
			continue;
		}
		
		message =  messageJson.getString("body").trim();
		if(message.startsWith(APP_SEPARATOR_MESSAGE)){
			message = message.replace(APP_SEPARATOR_MESSAGE, "");
			String[] messageArr =  message.split(",");
			ourMessages.add(messageArr);
		}
	}
	return ourMessages;
}

public List<String[]> filterMessages(JSONArray messages) throws JSONException{
	return filterMessages(messages, false);
}*/
	
	/////////---------------- Newly added ----------------------
	/// commented for place holder, 
	////////----------------------------------------------------
	
	/**
     * A placeholder fragment containing a simple view.
     */
    /*public static class PlaceholderFragment extends Fragment {
        *//**
         * The fragment argument representing the section number for this
         * fragment.
         *//*
        private static final String ARG_SECTION_NUMBER = "section_number";

        *//**
         * Returns a new instance of this fragment for the given section
         * number.
         *//*
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MessageActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }*/
    
	
	//activity should implement 
	/// NavigationDrawerFragment.NavigationDrawerCallbacks class
	@Override
    public void onNavigationDrawerItemSelected(JSONObject user) {
		String incomingUserName = null;
		int incomingUserId = -1;
		try {
			incomingUserName = user.getString("first_name") + 
					" " + user.getString("last_name");
			incomingUserId = user.getInt("id");
		} catch (JSONException e) {
			// TODO To be handled
			e.printStackTrace();
		}
		if(incomingUserId != activeUserId){
			initForUser(user);
			activeUserName = incomingUserName;
			activeUserId = incomingUserId;
			
			setTitle(activeUserName);
			//first time call with more messages
			recieveMessageHistory(50); 
			//then as written in recieveMessagePeriodicly
			recieveMessagePeriodicly();
			//checkUnreadeMessagesPeriodicly();
		}
    }
    
	private void initForUser(JSONObject user){
		LinearLayout historyLayout = (LinearLayout)findViewById(R.id.messageHistory);
		historyLayout.removeAllViews();
		LinearLayout formLayout = (LinearLayout)findViewById(R.id.linearLayout1);
		formLayout.removeAllViews();
		
		messageToSend.clear();
		/*pictogramsToSend.clear();
		if(newMessageAdapter == null){
			newMessageAdapter = new MessageAdapter(MessageActivity.this, pictogramsToSend);
			HorizontialListView newMessageLayout = (HorizontialListView)findViewById(R.id.newMessage);
			newMessageLayout.setAdapter(newMessageAdapter);
		} else {
			newMessageAdapter.updateView(pictogramsToSend);
		}*/
		
		//method is called first time
		if(activeUserName == null){
			View view = findViewById(R.id.container);
			view.setVisibility(View.VISIBLE);
			
			View helloView = findViewById(R.id.helloView);
			helloView.setVisibility(View.GONE);
			//hide mnacacner@
		} else{
			handler.removeCallbacks(recieveMessagesRunnable);
			//handler.removeCallbacks(checkUnreadMessagesRunnable);
		}
	}
	
	/**
	 * For square images
	 * @param image
	 * @param size
	 */
	private void setImageViewSize(ImageView image, int size, int margin){
		LinearLayout.LayoutParams par = new LinearLayout.LayoutParams(size, size);
		par.setMargins(margin, margin, margin, margin);
		//TODO: not affect in bottom margin
		image.setLayoutParams(par);
	}
	
	
	@Override
	public OnClickListener getOnPictogramClickListener() {
		return onPictogramClickListener;
	}
	
	private void initOnPictogramClickListener() {
		onPictogramClickListener = new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				
				final ImageView image = new ImageView(MessageActivity.this);
				//translated to pixels
				int size = (int)getResources().getDimension(R.dimen.new_message_height)-5;
				setImageViewSize(image, size, 2);
				
				Pictogram pic = ((GridViewHolder)view.getTag()).getPictogram(); //was set in adapter
				String picRuName  = NameManager.getInstance().getFileRuName(pic.getPath());
				addImageNameToSendMessages(picRuName);
				
				//for gridview
				/*pictogramsToSend.add(pic);
				newMessageAdapter.updateView(pictogramsToSend);
				final HorizontialListView newMessageLayout = (HorizontialListView)findViewById(R.id.newMessage);
				newMessageLayout.post(new Runnable() {
					@Override
					public void run() {
						//scroll
						newMessageLayout.setSelection(pictogramsToSend.size());
						if(pictogramsToSend.size() > getResources().getInteger(R.integer.new_message_icons_count)){
							newMessageLayout.setSelection(pictogramsToSend.size());
							//newMessageLayout.scrollTo(pictogramsToSend.size());
						}
					}
				});*/
				
				final LinearLayout piktogramsLayout = (LinearLayout) findViewById(R.id.linearLayout1);
				ImageLoader imageLoader = ImageLoader.getInstance();
				imageLoader.displayImage(pic.getPathWithAssests(), image, new SimpleImageLoadingListener() {
					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
						//BitmapDrawable bd = new BitmapDrawable(getResources(), loadedImage);
						//((ImageView)view).setBackground(bd);
						piktogramsLayout.addView(image);
						HorizontalScrollView hView = (HorizontalScrollView)findViewById(R.id.new_mess_scroll);
						scrollRight(hView);
					}
				});
				
			}
		};
	}
	
	 /*public void onSectionAttached(int number) {
		 
	 }
	 */
	
	//------------------Async tasks-------------//
	
	private class ManagersInitializer extends AsyncTask<Void, String, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			
			//init NameManager
			XmlPullParser parser = getResources().getXml(R.xml.catalog);
			NameManager.getInstance().init(parser);
			
			//init PictogramManager
			PictogramManager.getInstance().init(getAssets());
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			PictogramManager pManager = PictogramManager.getInstance();
			NameManager nManager = NameManager.getInstance();
			//LinearLayout firstLevelGorups = (LinearLayout)findViewById(R.id.linearLayout_groups);
			GridView firstLevelGorups = (GridView)findViewById(R.id.firstLevelGroups_view);
			updateFirstLevelGroupAdapter(firstLevelGorups, pManager.getFirstLevelGroups());
			
			//create view for first level groups
			/*for(GroupPictogram pic : pManager.getFirstLevelGroups()){
				Button groupBtn = new Button(MessageActivity.this);
				String ruName = nManager.getGroupRuName(pic.getPath());
				groupBtn.setText(ruName);
				int fontSize = (int)getResources().getDimension(R.dimen.button_font_size);
				groupBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
				groupBtn.setBackgroundResource(R.drawable.group_button_effect);
				groupBtn.setTag(pic);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
	                    LayoutParams.MATCH_PARENT,
	                    LayoutParams.MATCH_PARENT, 1.0f);
				//params.setMargins(2, 0, 2, 0);
				groupBtn.setLayoutParams(params);
				groupBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						GroupPictogram pic = ((GroupPictogram)v.getTag());
						updateAdapters(pic.getInnerPictograms());
					}
				});
				UIUtil.addGroupIconToButton(groupBtn, pic, getResources());
				firstLevelGorups.addView(groupBtn);
			}*/
			
		}
	}
	//------------------End of Async tasks-------------//	

	private void updateFirstLevelGroupAdapter(final GridView groupView, List<? extends Pictogram> pictograms){
		if(firstLevelGroupAdapter == null){
			firstLevelGroupAdapter = new GroupAdapter(MessageActivity.this, pictograms);
			groupView.setAdapter(firstLevelGroupAdapter);
			groupView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
					GroupPictogram gp = (GroupPictogram)groupView.getItemAtPosition(position);
					updateAdapters(gp.getInnerPictograms());
				}
			});
			//clicks first element
			groupView.performItemClick(groupView.getAdapter().getView(0, null, null), 
					0, groupView.getAdapter().getItemId(0));
			
		} else {
			firstLevelGroupAdapter.updateView(pictograms);
			
		}
	}
	
	
	private void updateAdapters(List<Pictogram> pictograms){
		View subgroupsContainer = findViewById(R.id.subGroups_container);
		if(pictograms.size() == 0){
			updateSubGroupAdapter(pictograms);
			updatedateGridViewAdapter(pictograms);
		} else {
			//assume all other should have the same type
			if(pictograms.get(0).getType() == ElementType.FILE){
				subgroupsContainer.setVisibility(View.GONE);
				updateSubGroupAdapter(new ArrayList<Pictogram>());
				updatedateGridViewAdapter(pictograms);
			} else if(pictograms.get(0).getType() == ElementType.GROUP){
				subgroupsContainer.setVisibility(View.VISIBLE);
				updateSubGroupAdapter(pictograms);
				subGroupsView.setItemChecked(0, true);
				updatedateGridViewAdapter(((GroupPictogram)pictograms.get(0)).getInnerPictograms());
			}
		}
		
	}
	
	private void updateSubGroupAdapter(List<Pictogram> pictograms){
		if(subGroupAdapter == null){
			subGroupAdapter = new GroupAdapter(MessageActivity.this, pictograms, true);
			subGroupsView.setAdapter(subGroupAdapter);
			subGroupsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
					GroupPictogram gp = (GroupPictogram)subGroupsView.getItemAtPosition(position);
					updatedateGridViewAdapter(gp.getInnerPictograms());
				}
			});
			//not allow to scroll
			/*subGroupsView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(event.getAction() == MotionEvent.ACTION_MOVE){
			            return true;
			        }
			        return false;
				}
			});*/
			
		} else {
			subGroupAdapter.updateView(pictograms);
			
		}
	}
	
		
	private void updatedateGridViewAdapter(List<Pictogram> pictograms){
			if(gridViewAdapter == null){
				gridViewAdapter = new GridViewAdapter(MessageActivity.this, MessageActivity.this, pictograms);
				pictogramsGridView.setAdapter(gridViewAdapter);
				//Not allowed for imageButton 
				//must be changed to ImageView
				/*pictogramsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
						Pictogram pic = (Pictogram)pictogramsGridView.getItemAtPosition(position);
						
						
						LinearLayout piktogramsLayout = (LinearLayout) findViewById(R.id.linearLayout1);
						ImageView image = new ImageView(MessageActivity.this);

						String picRuName = NameManager.getInstance().getFileRuName(pic.getPath());
						addImageNameToSendMessages(picRuName);

						//maybe change to tag, because if image in message part scales
						//it affects to button
						
						ImageLoader imageLoader = ImageLoader.getInstance();
						imageLoader.displayImage(pic.getPathWithAssests(), image, new SimpleImageLoadingListener() {
							@Override
							public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
								BitmapDrawable bd = new BitmapDrawable(getResources(), loadedImage);
								((ImageView)view).setBackground(bd);
							}
						});
						piktogramsLayout.addView(image);
						Log.e("asd", "asd");
						
					}
				});*/
			} else {
				gridViewAdapter.updateView(pictograms);
			}
			pictogramsGridView.post(new Runnable() {
				@Override
				public void run() {
					pictogramsGridView.setSelection(0);//moothScrollToPosition(0);
				}
			});
			//pictogramsGridView.smoothScrollToPosition(0);
			//pictogramsGridView.smoothScrollToPositionFromTop(0, 0, 200);
			
		}
	
	
	private void initImageLoader(){
		//Get the imageloader.
		ImageLoader imageLoader = ImageLoader.getInstance();
		
		//Create image options.
		DisplayImageOptions options = new DisplayImageOptions.Builder()
	    .cacheInMemory(true)
	    .imageScaleType(ImageScaleType.EXACTLY) //Only need for group buttons, need to be changed
	    .bitmapConfig(Bitmap.Config.ALPHA_8) //because our images are black/white
	    .build();
		
		//Create a config with those options.
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.defaultDisplayImageOptions(options)
	    .build(); 
		
		//Initialize the imageloader.
		imageLoader.init(config);
	}
	
}
