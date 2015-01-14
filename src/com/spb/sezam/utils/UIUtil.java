package com.spb.sezam.utils;

import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.Button;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.spb.sezam.R;
import com.spb.sezam.management.ElementType;
import com.spb.sezam.management.GroupPictogram;
import com.spb.sezam.management.Pictogram;

public class UIUtil {
	
	private UIUtil(){
	}
	
	/**
	 * Adds group's first pictogram to the top of the button
	 * @param btn {@link Button} to which pictogram icon will be added
	 * @param group {@link GroupPictogram} from which the first icon should be found
	 * @param res {@link Resources}
	 */
	public static void addGroupIconToButton(Button btn, GroupPictogram group, Resources res){
		String fileName = null;
		List<Pictogram> pictograms = group.getInnerPictograms();
		if(pictograms.size() != 0){
			Pictogram firstElement = pictograms.get(0);
			if(firstElement.getType() == ElementType.FILE){
				fileName = firstElement.getPathWithAssests();
			} else if(firstElement.getType() == ElementType.GROUP){
				if(((GroupPictogram)firstElement).getInnerPictograms().size() != 0){
					firstElement = ((GroupPictogram)firstElement).getInnerPictograms().get(0);
					fileName = firstElement.getPathWithAssests();
				}
			}
		}
		
		if (fileName != null){
			int iconSize = (int)res.getDimension(R.dimen.group_icon_size);
			ImageSize imageSize = new ImageSize(iconSize, iconSize);
			Bitmap image = ImageLoader.getInstance().loadImageSync(fileName, imageSize);
			BitmapDrawable drImage = new BitmapDrawable(res, image);
			//Log.e("", "Bitmap Width After Draw: " + image.getWidth());
			
			//set image t the top of the button
			btn.setCompoundDrawablesWithIntrinsicBounds(null, drImage, null, null);
			btn.setCompoundDrawablePadding(-15); //for text
			btn.setPadding(0,5,0,0); //for image
		}
	}

}
