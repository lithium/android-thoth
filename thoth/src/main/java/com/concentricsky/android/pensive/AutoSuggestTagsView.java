package com.concentricsky.android.pensive;

import android.content.Context;
import android.database.Cursor;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

/**
 * Created by wiggins on 5/19/13.
 */
public class AutoSuggestTagsView extends AutoCompleteTextView {

    /* implement all the super constructors to inflate from xml properly */
    public AutoSuggestTagsView(Context context) { this(context, null); }
    public AutoSuggestTagsView(Context context, AttributeSet attrs) { this(context, attrs, android.R.attr.autoCompleteTextViewStyle); }
    public AutoSuggestTagsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        SimpleCursorAdapter  adapter = new SimpleCursorAdapter(context,
                android.R.layout.simple_list_item_1,
                null, new String[] {"title"}, new int[] {android.R.id.text1}, 0);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                return cursor.getString(cursor.getColumnIndexOrThrow("title"));
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                String text = null;
                if (charSequence != null) {
                    text = charSequence.toString();
                    int idx = text.lastIndexOf(',');
                    if (idx != -1) {
                        text = text.substring(idx+1);
                    }
                    text = text.trim();
                }
                Cursor cursor = ThothDatabaseHelper.getInstance().getTagCursor(text, true);
                return cursor;
            }
        });
        setAdapter(adapter);
    }


    @Override
    protected void replaceText(CharSequence text) {
        Editable current = getText();
        int st = current.toString().lastIndexOf(',');
        if (st == -1) {
//            if (current.length() > 0) {
//                setText(current.toString().trim()+", "+text+", ");
//            }
//            else
                setText(text+", ");
        } else {
            int en = current.length();
            current.replace(Math.min(st+1, en), en, " "+text+", ");
        }
        setSelection(getText().length());
    }
}
