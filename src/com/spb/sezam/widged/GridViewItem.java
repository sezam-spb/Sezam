package com.spb.sezam.widged;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * For square elements
 * @author Serob
 *
 */
public class GridViewItem extends ImageButton {
	public GridViewItem(Context context) {
        super(context);
    }

    public GridViewItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec); // This is the key that will make the height equivalent to its width
    }
}
