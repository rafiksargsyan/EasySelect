package com.rsargsyan.easyselect;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.view.MotionEvent;
import android.view.View;

public abstract class TouchableSpan extends CharacterStyle implements UpdateAppearance {

    public abstract boolean onTouch(View widget, MotionEvent m);

    @Override
    public abstract void updateDrawState(TextPaint ds);
}
