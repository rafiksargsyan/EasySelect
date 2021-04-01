package com.rsargsyan.easyselect;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

public class TouchableMovementMethod extends ScrollingMovementMethod {
    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
                                MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        x -= widget.getTotalPaddingLeft();
        y -= widget.getTotalPaddingTop();

        x += widget.getScrollX();
        y += widget.getScrollY();

        Layout layout = widget.getLayout();
        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        TouchableSpan[] link = buffer.getSpans(off, off, TouchableSpan.class);

        if (link.length != 0) {
            return link[0].onTouch(widget, event);
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    public static MovementMethod getInstance() {
        if (instance == null)
            instance = new TouchableMovementMethod();

        return instance;
    }

    private static TouchableMovementMethod instance;
}
