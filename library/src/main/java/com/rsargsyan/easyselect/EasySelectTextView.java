package com.rsargsyan.easyselect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
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
import androidx.core.util.Pools.SimplePool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasySelectTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int MAX_POOL_SIZE = 1000;
    private static final SpanningStrategy DEFAULT_SPANNING_STRATEGY =
            CharacterSpanningStrategy.getInstance();

    private @ColorInt
    int selectionTextColor;
    private @ColorInt
    int selectionTextHighlightColor;
    private int initialSelectionStart;
    private int initialSelectionEnd;
    private int effectiveSelectionStart;
    private int effectiveSelectionEnd;
    private OnSelectionCompletedCallback onSelectionCompletedCallback;
    private SpanningStrategy spanningStrategy;
    private Spannable spannable;
    private SimplePool<EasySelectTouchableSpan> touchableSpanObjectPool;
    private SimplePool<EasySelectForegroundSpan> foregroundSpanObjectPool;
    private SimplePool<EasySelectBackgroundSpan> backgroundSpanObjectPool;

    public EasySelectTextView(@NonNull Context context) {
        this(context, null);
    }

    public EasySelectTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.easySelectTextViewStyle);
    }

    public EasySelectTextView(@NonNull Context context, @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setup();

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
        // reset, as it might contain obsolete entries
        foregroundSpanObjectPool = null;
    }

    @SuppressWarnings("unused")
    public void setSelectionTextHighlightColor(@ColorInt int selectionTextHighlightColor) {
        this.selectionTextHighlightColor = selectionTextHighlightColor;
        // reset, as it might contain obsolete entries
        backgroundSpanObjectPool = null;
    }

    public @ColorInt
    int getSelectionTextColor() {
        return selectionTextColor;
    }

    public @ColorInt
    int getSelectionTextHighlightColor() {
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
        reset();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setup() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setTextIsSelectable(true);
        setMovementMethod(TouchableMovementMethod.getInstance());
        setOnTouchListener((v, event) -> false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void reset() {
        spannable = (Spannable) getText();
        removeTouchableSpans();
        populateSpans();
    }

    @NonNull
    private SimplePool<EasySelectTouchableSpan> getTouchableSpanObjectPoolNullSafe() {
        if (touchableSpanObjectPool == null) {
            touchableSpanObjectPool = new SimplePool<>(MAX_POOL_SIZE);
        }
        return touchableSpanObjectPool;
    }

    @NonNull
    private SimplePool<EasySelectForegroundSpan> getForegroundSpanObjectPoolNullSafe() {
        if (foregroundSpanObjectPool == null) {
            foregroundSpanObjectPool = new SimplePool<>(MAX_POOL_SIZE);
        }
        return foregroundSpanObjectPool;
    }

    @NonNull
    private SimplePool<EasySelectBackgroundSpan> getBackgroundSpanObjectPoolNullSafe() {
        if (backgroundSpanObjectPool == null) {
            backgroundSpanObjectPool = new SimplePool<>(MAX_POOL_SIZE);
        }
        return backgroundSpanObjectPool;
    }

    @NonNull
    private SpanningStrategy getSpanningStrategyNullSafe() {
        if (spanningStrategy == null) {
            spanningStrategy = DEFAULT_SPANNING_STRATEGY;
        }
        return spanningStrategy;
    }

    private void handleSpanningStrategyUpdated() {
        removeTouchableSpans();
        populateSpans();
    }

    private void removeTouchableSpans() {
        for (EasySelectTouchableSpan span : spannable.getSpans(0, spannable.length(),
                EasySelectTouchableSpan.class)) {
            spannable.removeSpan(span);
            getTouchableSpanObjectPoolNullSafe().release(span);
        }
    }

    private EasySelectTouchableSpan obtainTouchableSpan() {
        EasySelectTouchableSpan span = getTouchableSpanObjectPoolNullSafe().acquire();
        return span == null ? new EasySelectTouchableSpan() : span;
    }

    private EasySelectForegroundSpan obtainForegroundSpan() {
        EasySelectForegroundSpan span = getForegroundSpanObjectPoolNullSafe().acquire();
        return span == null ? new EasySelectForegroundSpan(selectionTextColor) : span;
    }

    private EasySelectBackgroundSpan obtainBakcgroundSpan() {
        EasySelectBackgroundSpan span = getBackgroundSpanObjectPoolNullSafe().acquire();
        return span == null ? new EasySelectBackgroundSpan(selectionTextHighlightColor) : span;
    }

    private void populateSpans() {
        int[] spans = getSpanningStrategyNullSafe().getSpans(spannable);
        for (int i = 0; i < spans.length; i += 2) {
            spannable.setSpan(obtainTouchableSpan(), spans[i], spans[i + 1],
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
            if (span instanceof EasySelectForegroundSpan ||
                    span instanceof EasySelectBackgroundSpan) {
                spannable.removeSpan(span);
                if (span instanceof EasySelectForegroundSpan) {
                    getForegroundSpanObjectPoolNullSafe().release((EasySelectForegroundSpan) span);
                } else {
                    getBackgroundSpanObjectPoolNullSafe().release((EasySelectBackgroundSpan) span);
                }
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
        decorate(span, obtainForegroundSpan());
    }

    private void decorateBackground(@NonNull Object span) {
        decorate(span, obtainBakcgroundSpan());
    }

    public interface OnSelectionCompletedCallback {
        void onSelectionCompleted(String selectedString);
    }

    public interface SpanningStrategy {
        int[] getSpans(@NonNull Spannable spannable);
    }

    public static class CharacterSpanningStrategy implements SpanningStrategy {
        private CharacterSpanningStrategy() {}

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

        private static CharacterSpanningStrategy INSTANCE;
        public static CharacterSpanningStrategy getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new CharacterSpanningStrategy();
            }
            return INSTANCE;
        }
    }

    public static class WordSpanningStrategy implements SpanningStrategy {
        private WordSpanningStrategy() {}

        @Override
        public int[] getSpans(@NonNull Spannable spannable) {
            List<Integer> retList = new ArrayList<>();
            Pattern wordPattern = Pattern.compile("\\S+");
            Matcher matcher = wordPattern.matcher((CharSequence) spannable);
            while (matcher.find()) {
                retList.add(matcher.start());
                retList.add(matcher.end());
            }
            return toIntArray(retList);
        }

        private static WordSpanningStrategy INSTANCE;
        @SuppressWarnings("unused")
        public static WordSpanningStrategy getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new WordSpanningStrategy();
            }
            return INSTANCE;
        }
    }

    private static int[] toIntArray(@NonNull List<Integer> list) {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; ++i)
            ret[i] = list.get(i);
        return ret;
    }

    private class EasySelectTouchableSpan extends TouchableSpan {
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
    }

    private static class EasySelectForegroundSpan extends ForegroundColorSpan {
        public EasySelectForegroundSpan(int color) {
            super(color);
        }

        @SuppressWarnings("unused")
        public EasySelectForegroundSpan(@NonNull Parcel src) {
            super(src);
        }
    }

    private static class EasySelectBackgroundSpan extends BackgroundColorSpan {
        public EasySelectBackgroundSpan(int color) {
            super(color);
        }

        @SuppressWarnings("unused")
        public EasySelectBackgroundSpan(@NonNull Parcel src) {
            super(src);
        }
    }
}