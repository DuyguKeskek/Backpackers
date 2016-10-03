package com.backpackers.android.ui.widget;

import com.backpackers.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EllipsizeTextView extends BaselineGridTextView {

    private static final String DEFAULT_ELLIPSIZE = "... ";

    private CharSequence mEllipsizeText;
    private int mEllipsizeIndex;
    private int mMaxLines;

    private boolean mIsExactlyMode;

    private int mColorClickableText;

    private StyleSpan mBoldSpan = new StyleSpan(Typeface.BOLD);

    private ClickableSpan mClickableSpan;

    private OnReadMoreListener mOnReadMoreListener;

    public EllipsizeTextView(Context context) {
        super(context, null);
        init(context, null);
    }

    public EllipsizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EllipsizeTextView);
        mEllipsizeIndex = a.getInt(R.styleable.EllipsizeTextView_ellipsize_index, 0);
        mEllipsizeText = a.getText(R.styleable.EllipsizeTextView_ellipsize_text);

        mColorClickableText = a.getColor(R.styleable.EllipsizeTextView_ellipsize_colorClickableText,
                ContextCompat.getColor(context, android.R.color.black));

        if (mEllipsizeText == null) {
            mEllipsizeText = DEFAULT_ELLIPSIZE;
        }
        a.recycle();

        mClickableSpan = new ReadMoreSpan();
    }


    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        mMaxLines = maxLines;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mIsExactlyMode = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY;
        final Layout layout = getLayout();
        if (layout != null) {
            if (isExceedMaxLine(layout) || isOutOfBounds(layout)) {
                adjustEllipsizeEndText(layout);
            }
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        if (mIsExactlyMode) {
            requestLayout();
        }
    }

    private boolean isExceedMaxLine(Layout layout) {
        return layout.getLineCount() > mMaxLines && mMaxLines > 0;
    }

    private boolean isOutOfBounds(Layout layout) {
        return layout.getHeight() > getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
    }

    private void adjustEllipsizeEndText(Layout layout) {
        final CharSequence originText = getText();
        final CharSequence restSuffixText = originText.subSequence(
                originText.length() - mEllipsizeIndex, originText.length());

        final int width = layout.getWidth() - getPaddingLeft() - getPaddingRight();
        final int maxLineCount = computeMaxLineCount(layout);
        final int lastLineWidth = (int) layout.getLineWidth(maxLineCount - 1);
        final int mLastCharacterIndex = layout.getLineEnd(maxLineCount - 1);

        final int suffixWidth = (int) (Layout.getDesiredWidth(mEllipsizeText, getPaint()) +
                Layout.getDesiredWidth(restSuffixText, getPaint())) + 1;

        if (lastLineWidth + suffixWidth > width) {
            final int widthDiff = lastLineWidth + suffixWidth - width;

            final int removedCharacterCount = computeRemovedEllipsizeEndCharacterCount(widthDiff,
                    originText.subSequence(0, mLastCharacterIndex));

            setText(originText.subSequence(0, mLastCharacterIndex - removedCharacterCount));
            //append(DEFAULT_ELLIPSIZE);
            setMovementMethod(LinkMovementMethod.getInstance());
            setHighlightColor(Color.TRANSPARENT);

            append(addClickableSpan(new SpannableString(mEllipsizeText)));
            //append(restSuffixText);
        } else {
            setText(originText.subSequence(0, mLastCharacterIndex));

            append(mEllipsizeText);
            append(restSuffixText);
        }
    }

    private int computeMaxLineCount(Layout layout) {
        int availableHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        for (int i = 0; i < layout.getLineCount(); i++) {
            if (availableHeight < layout.getLineBottom(i)) {
                return i;
            }
        }

        return layout.getLineCount();
    }

    private int computeRemovedEllipsizeEndCharacterCount(final int widthDiff, final CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }

        final List<Range<Integer>> characterStyleRanges = computeCharacterStyleRanges(text);
        final String textStr = text.toString();

        // prevent the subString from containing messy code when the given string contains emotion
        int characterIndex = text.length();
        int codePointIndex = textStr.codePointCount(0, characterIndex);
        int currentRemovedWidth = 0;

        while (codePointIndex > 0 && widthDiff > currentRemovedWidth) {
            codePointIndex--;
            characterIndex = textStr.offsetByCodePoints(0, codePointIndex);

            // prevent the subString from containing messy code when the given string contains CharacterStyle
            final Range<Integer> characterStyleRange =
                    computeCharacterStyleRange(characterStyleRanges, characterIndex);
            if (characterStyleRange != null) {
                characterIndex = characterStyleRange.getLower();
                codePointIndex = textStr.codePointCount(0, characterIndex);
            }

            currentRemovedWidth = (int) Layout.getDesiredWidth(
                    text.subSequence(characterIndex, text.length()),
                    getPaint());
        }

        return text.length() - textStr.offsetByCodePoints(0, codePointIndex);
    }

    private Range<Integer> computeCharacterStyleRange(List<Range<Integer>> characterStyleRanges, int index) {
        if (characterStyleRanges == null || characterStyleRanges.isEmpty()) {
            return null;
        }

        for (Range<Integer> characterStyleRange : characterStyleRanges) {
            if (characterStyleRange.contains(index)) {
                return characterStyleRange;
            }
        }

        return null;
    }

    private List<Range<Integer>> computeCharacterStyleRanges(CharSequence text) {
        final SpannableStringBuilder ssb = SpannableStringBuilder.valueOf(text);
        final CharacterStyle[] characterStyles = ssb.getSpans(0, ssb.length(), CharacterStyle.class);

        if (characterStyles == null || characterStyles.length == 0) {
            return Collections.emptyList();
        }

        List<Range<Integer>> ranges = new ArrayList<>();
        for (CharacterStyle characterStyle : characterStyles) {
            ranges.add(new Range<>(ssb.getSpanStart(characterStyle), ssb.getSpanEnd(characterStyle)));
        }

        return ranges;
    }

    private CharSequence addClickableSpan(SpannableString s) {
        s.setSpan(mBoldSpan, 0, s.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        s.setSpan(mClickableSpan, 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    /**
     * @param ellipsizeText  causes words in the text that are longer than the view is wide
     *                       to be ellipsized by used the text instead of broken in the middle.
     * @param ellipsizeIndex the index of the ellipsizeText will be inserted in the reverse order.
     */
    public void setEllipsizeText(CharSequence ellipsizeText, int ellipsizeIndex) {
        mEllipsizeText = ellipsizeText;
        mEllipsizeIndex = ellipsizeIndex;
    }

    public void setOnReadMoreListener(OnReadMoreListener onReadMoreListener) {
        mOnReadMoreListener = onReadMoreListener;
    }

    public interface OnReadMoreListener {
        void onReadMore(View v);
    }

    public static final class Range<T extends Comparable<? super T>> {

        private final T mLower;
        private final T mUpper;

        Range(final T lower, final T upper) {
            mLower = lower;
            mUpper = upper;

            if (lower.compareTo(upper) > 0) {
                throw new IllegalArgumentException("lower must be less than or equal to upper");
            }
        }

        public T getLower() {
            return mLower;
        }

        public T getUpper() {
            return mUpper;
        }

        public boolean contains(T value) {
            boolean gteLower = value.compareTo(mLower) >= 0;
            boolean lteUpper = value.compareTo(mUpper) < 0;

            return gteLower && lteUpper;
        }
    }

    private final class ReadMoreSpan extends ClickableSpan {

        @Override
        public void onClick(View widget) {
            mOnReadMoreListener.onReadMore(widget);
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(mColorClickableText);
            ds.setUnderlineText(false);
        }
    }
}
