package com.spb.sezam.adapters;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import com.spb.sezam.R;
import com.spb.sezam.management.GroupPictogram;
import com.spb.sezam.management.NameManager;
import com.spb.sezam.management.Pictogram;
import com.spb.sezam.utils.UIUtil;

public class GroupAdapter extends BaseAdapter{

	private final Context context;
	private List<? extends Pictogram> groups = new ArrayList<>();
	private boolean isSubGroup = false;
	
	static class ViewHolder {
		public Button subGroupItem;
	}
	
	public GroupAdapter(Context context, List<? extends Pictogram> groups){
		this.context = context;
		this.groups = groups;
		this.isSubGroup = false;
	}
	
	public GroupAdapter(Context context, List<? extends Pictogram> groups, boolean isSubGroup){
		this.context = context;
		this.groups = groups;
		this.isSubGroup = isSubGroup;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View elementView = convertView;
		
		// reuse views
		if(elementView == null){
			LayoutInflater inflater = (LayoutInflater)context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			elementView = inflater.inflate(R.layout.group_layout, parent, false);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.subGroupItem  = (Button)elementView.findViewById(R.id.subGroupItem);
			elementView.setTag(viewHolder);
		}
		
		// fill data
		ViewHolder holder = (ViewHolder) elementView.getTag();
		GroupPictogram group = getItem(position);
		String ruName = NameManager.getInstance().getGroupRuName(group.getPath());
		holder.subGroupItem.setText(ruName);
		//small font for subGroups
		if(isSubGroup){
			float fontInPx = context.getResources().getDimension(R.dimen.button_font_size);
			int fontInSp = (int) (fontInPx / context.getResources().getDisplayMetrics().scaledDensity);
			holder.subGroupItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontInSp - 2);
		}
		UIUtil.addGroupIconToButton(holder.subGroupItem, group, context.getResources());
		
		return elementView;
	}
	
	public void updateView(List<? extends Pictogram> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }
	
	@Override
	public int getCount() {
		return groups.size();
	}

	@Override
	public GroupPictogram getItem(int position) {
		return (GroupPictogram)groups.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	
}
