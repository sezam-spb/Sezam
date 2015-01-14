package com.spb.sezam.utils;

import java.io.IOException;
import java.io.InputStream;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


/**
 * @deprecated Use {@link ImageLoader} instead
 * @author Serob
 *
 */
@Deprecated
public class BitmapDecoder {

	private BitmapDecoder() {
		
	}
	
	/**
	 * Decodes black-white(ALPHA_8 config) image as is 
	 * @param inStream {@link InputStream} containing file information
	 * @return {@link Bitmap} with ALPHA_8 config
	 */
	public static Bitmap decodeRealSizeImage(InputStream inStream){
		//Decode without inSampleSize
		return decodeBitmapALPHA_8(inStream, 0);
	}
	
	/**
	 * Decodes black-white(ALPHA_8 config) image with new smaller sizes.
	 * If the sizes are greater than real image size, then decodes as-is
	 * @param inStream {@link InputStream} containing file information
	 * @param width new width of the image
	 * @param hight new height of the image
	 * @return scaled {@link Bitmap} with ALPHA_8 config
	 */
	public static Bitmap decodeScaledImage(InputStream inStream, int width, int hight){
	    Bitmap b = null;

	    //Decode image size
	    BitmapFactory.Options o = new BitmapFactory.Options();
	    o.inJustDecodeBounds = true;
	    BitmapFactory.decodeStream(inStream, null, o);

	    int scale = 1;
	    if (o.outHeight > hight || o.outWidth > width) {
	        scale = (int)Math.pow(2, (int) Math.ceil(Math.log(width / 
	           (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
	    }

	    //Decode with inSampleSize
	    b = decodeBitmapALPHA_8(inStream, scale);
	    try {
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	    return b;
	}
	
	private static Bitmap decodeBitmapALPHA_8(InputStream inStream, int scale) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.ALPHA_8;
		opts.inSampleSize = scale;
		return BitmapFactory.decodeStream(inStream, null, opts);
	}
}
