package com.spb.sezam.adapters;

import com.spb.sezam.management.Pictogram;
import com.spb.sezam.widged.GridViewItem;

public class GridViewHolder {
	
	public GridViewItem pictogramIcon;
	private Pictogram pictogram;
	
	public Pictogram getPictogram() {
		return pictogram;
	}
	public void setPictogram(Pictogram pictogram) {
		this.pictogram = pictogram;
	}
	
	
}
