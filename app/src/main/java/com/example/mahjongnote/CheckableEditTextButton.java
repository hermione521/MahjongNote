package com.example.mahjongnote;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableEditTextButton extends AppCompatEditText implements Checkable {

    private boolean stateEditText = true; // Otherwise checkable button
    private boolean checked = false;

    @ColorInt private int nonCheckedColor;
    @ColorInt private int checkedColor;

    public CheckableEditTextButton(Context context) {
        super(context);
    }

    public CheckableEditTextButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableEditTextButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void becomeCheckableButton() {
        stateEditText = false;

        setFocusable(false);
        setClickable(true);
        setCursorVisible(false);
        setChecked(false);

        nonCheckedColor = getContext().getColor(R.color.name_edit_text_background_started);
        checkedColor = getContext().getColor(R.color.name_edit_text_background_pressed);
    }

    @Override
    public void setChecked(boolean b) {
        if (!stateEditText) {
            checked = b;
            setBackgroundColor(checked ? checkedColor : nonCheckedColor);
        }
    }

    @Override
    public boolean isChecked() {
        return !stateEditText && checked;
    }

    @Override
    public void toggle() {
        if (!stateEditText) {
            setChecked(!checked);
        }
    }
}
