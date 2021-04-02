package com.rsargsyan.easyselect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasySelectTextView extends androidx.appcompat.widget.AppCompatTextView {
    private @ColorInt int selectionTextColor;
    private @ColorInt int selectionTextHighlightColor;
    private int initialSelectionStart;
    private int initialSelectionEnd;
    private int effectiveSelectionStart;
    private int effectiveSelectionEnd;
    private OnSelectionCompletedCallback onSelectionCompletedCallback;
    private SpanningStrategy spanningStrategy;

    private Spannable spannable;


    public EasySelectTextView(@NonNull Context context) {
        this(context, null);
    }

    public EasySelectTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.easySelectTextViewStyle);
    }

    public EasySelectTextView(@NonNull Context context, @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();

        final Resources.Theme theme = context.getTheme();

        final TypedArray a =
                theme.obtainStyledAttributes(attrs, R.styleable.EasySelectTextView,
                        defStyleAttr, 0);

        // TODO: First get attributes from "textAppearance" if set, then override if needed

        selectionTextColor =
                a.getColor(R.styleable.EasySelectTextView_selectedTextColor, getCurrentTextColor());

        selectionTextHighlightColor =
                a.getColor(R.styleable.EasySelectTextView_selectedTextHighlightColor,
                        getHighlightColor());

        a.recycle();
    }

    @SuppressWarnings("unused")
    public void setSelectionTextColor(@ColorInt int selectionTextColor) {
        this.selectionTextColor = selectionTextColor;
    }

    @SuppressWarnings("unused")
    public void setSelectionTextHighlightColor(@ColorInt int selectionTextHighlightColor) {
        this.selectionTextHighlightColor = selectionTextHighlightColor;
    }

    public @ColorInt int getSelectionTextColor() {
        return selectionTextColor;
    }

    public @ColorInt int getSelectionTextHighlightColor() {
        return selectionTextHighlightColor;
    }

    public void setOnSelectionCompletedCallback(OnSelectionCompletedCallback callback) {
        onSelectionCompletedCallback = callback;
    }

    public void setSpanningStrategy(SpanningStrategy strategy) {
        spanningStrategy = strategy;
        handleSpanningStrategyUpdated();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, BufferType.SPANNABLE);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setTextIsSelectable(true);
        setMovementMethod(TouchableMovementMethod.getInstance());
        setOnTouchListener((v, event) -> false);

        spannable = (Spannable) getText();
        if (spanningStrategy == null) {
            spanningStrategy = new CharacterSpanningStrategy();
        }
        removeSpans(TouchableSpan.class);
        populateSpans();
    }

    private void handleSpanningStrategyUpdated() {
        removeSpans(TouchableSpan.class);
        populateSpans();
    }

    private <T> void removeSpans(Class<T> type)  {
        for (Object span : spannable.getSpans(0, spannable.length(), type)) {
            spannable.removeSpan(span);
        }
    }

    private void populateSpans() {
        int[] spans = spanningStrategy.getSpans(spannable);
        for (int i = 0; i < spans.length; i += 2) {
            TouchableSpan touchableSpan = new TouchableSpan() {
                @Override
                public boolean onTouch(View widget, MotionEvent m) {
                    int spanStart = spannable.getSpanStart(this);
                    int spanEnd = spannable.getSpanEnd(this);

                    if (m.getAction() == MotionEvent.ACTION_DOWN) {
                        initialSelectionStart = spanStart;
                        initialSelectionEnd = spanEnd;
                        effectiveSelectionStart = spanStart;
                        effectiveSelectionEnd = spanEnd;
                    }

                    int oldEffectiveSelectionStart = effectiveSelectionStart;
                    int oldEffectiveSelectionEnd = effectiveSelectionEnd;
                    effectiveSelectionStart = (Math.min(spanStart, initialSelectionStart));
                    effectiveSelectionEnd = (Math.max(spanEnd, initialSelectionEnd));

                    decorateSelection(oldEffectiveSelectionStart, oldEffectiveSelectionEnd,
                            effectiveSelectionStart, effectiveSelectionEnd);

                    if (m.getAction() == MotionEvent.ACTION_UP) {
                        if (onSelectionCompletedCallback != null) {
                            String selectedText =
                                    spannable.subSequence(effectiveSelectionStart,
                                            effectiveSelectionEnd).toString();
                            onSelectionCompletedCallback.onSelectionCompleted(selectedText);
                        }
                        removeDecoration(0, spannable.length());
                    }

                    return true;
                }

                @Override
                public void updateDrawState(TextPaint ds) { }
            };
            spannable.setSpan(touchableSpan, spans[i], spans[i + 1],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void decorateSelection(int oldSelectionStart, int oldSelectionEnd,
                                   int newSelectionStart, int newSelectionEnd) {
        if (oldSelectionStart == newSelectionStart && oldSelectionEnd == newSelectionEnd) {
            decorateSelection(newSelectionStart, newSelectionEnd);
            return;
        }
        if (newSelectionEnd > oldSelectionEnd) {
            decorateSelection(oldSelectionEnd, newSelectionEnd);
        }
        if (newSelectionEnd < oldSelectionEnd) {
            removeDecoration(newSelectionEnd, oldSelectionEnd);
        }
        if (newSelectionStart < oldSelectionStart) {
            decorateSelection(newSelectionStart, oldSelectionStart);
        }
        if (newSelectionStart > oldSelectionStart) {
            removeDecoration(oldSelectionStart, newSelectionStart);
        }
    }

    private void removeDecoration(int selectionStart, int selectionEnd) {
        Object[] selectedSpans =
                spannable.getSpans(selectionStart, selectionEnd, Object.class);
        for (Object span : selectedSpans) {
            if (span instanceof ForegroundColorSpan || span instanceof BackgroundColorSpan) {
                spannable.removeSpan(span);
            }
        }
    }

    private void decorateSelection(int selectionStart, int selectionEnd) {
        Object[] selectedSpans =
                spannable.getSpans(selectionStart, selectionEnd, TouchableSpan.class);
        for (Object span : selectedSpans) {
            decorateForeground(span);
            decorateBackground(span);
        }
    }

    private void decorate(@NonNull Object span, @NonNull Object decoration) {
        spannable.setSpan(decoration, spannable.getSpanStart(span),
                spannable.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void decorateForeground(@NonNull Object span) {
        decorate(span, new ForegroundColorSpan(selectionTextColor));
    }

    private void decorateBackground(@NonNull Object span) {
        decorate(span, new BackgroundColorSpan(selectionTextHighlightColor));
    }

    public interface OnSelectionCompletedCallback {
        void onSelectionCompleted(String selectedString);
    }

    public interface SpanningStrategy {
        int[] getSpans(@NonNull Spannable spannable);
    }

    public static class CharacterSpanningStrategy implements SpanningStrategy {
        @Override
        public int[] getSpans(@NonNull Spannable spannable) {
            int length = spannable.length();
            int[] ret = new int[2 * length];
            for (int i = 0, j = 0; i < length; ++i, j += 2) {
                ret[j] = i;
                ret[j + 1] = i + 1;
            }
            return ret;
        }
    }

    public static class WordSpanningStrategy implements SpanningStrategy {
        @Override
        public int[] getSpans(@NonNull Spannable spannable) {
            String spannableStr = ((CharSequence)spannable).toString();
            List<Integer> retList = new ArrayList<>();
            Pattern wordPattern = Pattern.compile("\\S+");
            Matcher matcher = wordPattern.matcher(spannableStr);
            while (matcher.find()) {
                retList.add(matcher.start());
                retList.add(matcher.end());
            }
            return toIntArray(retList);
        }
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; ++i)
            ret[i] = list.get(i);
        return ret;
    }
}
