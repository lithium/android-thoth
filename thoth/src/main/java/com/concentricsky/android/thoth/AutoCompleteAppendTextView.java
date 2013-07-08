package com.concentricsky.android.thoth;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * Created by wiggins on 5/19/13.
 */
public class AutoCompleteAppendTextView extends AutoCompleteTextView {

    /* implement all the super constructors to inflate from xml properly */
    public AutoCompleteAppendTextView(Context context) {
        super(context);
    }

    public AutoCompleteAppendTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoCompleteAppendTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void replaceText(CharSequence text) {
        Editable current = getText();
        int st = current.toString().lastIndexOf(',');
        if (st == -1) {
            if (current.length() > 0) {
                setText(current.toString().trim()+", "+text+", ");
            }
            else
                setText(text+", ");
        } else {
            int en = current.length();
            current.replace(Math.min(st+2, en), en, text+", ");
        }
        setSelection(getText().length());
    }
}
