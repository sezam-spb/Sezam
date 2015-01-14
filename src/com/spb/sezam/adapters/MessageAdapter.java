package com.spb.sezam.adapters;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.spb.sezam.R;
import com.spb.sezam.management.Pictogram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class MessageAdapter extends BaseAdapter {

	private Context context;
	private List<Pictogram> pictograms = new ArrayList<>();
	
	static class ViewHolder {
		public ImageView messageItem;
	}
	
	public MessageAdapter(Context context, List<Pictogram> pictograms){
		this.context = context;
		this.pictograms = pictograms;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View elementView = convertView;
		
		// reuse views
		if(elementView == null){
			LayoutInflater inflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			elementView = inflater.inflate(R.layout.message_element_layout, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.messageItem  = (ImageView)elementView.findViewById(R.id.messageItem);
			elementView.setTag(viewHolder);
		}
		
		// fill data
		ViewHolder holder = (ViewHolder) elementView.getTag();
		Pictogram pic = getItem(position);
		
		ImageLoader imageLoader = ImageLoader.getInstance();
		int messageHeight = (int)context.getResources().getDimension(R.dimen.new_message_height);
		ImageSize imageSize = new ImageSize(messageHeight, messageHeight); //to economy memory
		Bitmap bitmap = imageLoader.loadImageSync(pic.getPathWithAssests(), imageSize);
		
		BitmapDrawable drBitmap = new BitmapDrawable(context.getResources(), bitmap); 
		holder.messageItem.setBackground(drBitmap);
		
		//imageLoader.displayImage(pic.getPathWithAssests(), holder.messageItem);
		return elementView;
	}
	
	public void updateView(List<Pictogram> pictograms) {
        this.pictograms = pictograms;
        notifyDataSetChanged();
    }
	
	@Override
	public int getCount() {
		return pictograms.size();
	}

	@Override
	public Pictogram getItem(int position) {
		return pictograms.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
}
