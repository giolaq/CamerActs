package com.laquysoft.cameracts;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;


import java.lang.Math;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;


/**
 * Custom view that shows a pie chart and, optionally, a label.
 */
public class VotationChart extends ViewGroup {
    private HashMap<String, Item> mData = new HashMap<>();

    private float mTotal = 0.0f;

    private RectF mPieBounds = new RectF();

    private Paint mPiePaint;
    private Paint mShadowPaint;


    private float mHighlightStrength = 1.15f;


    private int mPieRotation;

    private OnCurrentItemChangedListener mCurrentItemChangedListener = null;

    private PieView mPieView;
    private String mCurrentItemLabel;

    private RectF mShadowBounds = new RectF();


    /**
     * Interface definition for a callback to be invoked when the current
     * item changes.
     */
    public interface OnCurrentItemChangedListener {
        void OnCurrentItemChanged(VotationChart source, String currentItemLabel);
    }

    /**
     * Class constructor taking only a context. Use this constructor to create
     * {@link com.laquysoft.cameracts.VotationChart} objects from your own code.
     *
     * @param context
     */
    public VotationChart(Context context) {
        super(context);
        init();
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor
     * is used by the layout engine to construct a {@link com.laquysoft.cameracts.VotationChart} from a set of
     * XML attributes.
     *
     * @param context
     * @param attrs   An attribute set which can contain attributes from
     *                {@link com.laquysoft.cameracts.R.styleable} as well as attributes inherited
     *                from {@link android.view.View}.
     */
    public VotationChart(Context context, AttributeSet attrs) {
        super(context, attrs);

        // attrs contains the raw values for the XML attributes
        // that were specified in the layout, which don't include
        // attributes set by styles or themes, and which may have
        // unresolved references. Call obtainStyledAttributes()
        // to get the final values for each attribute.
        //
        // This call uses R.styleable.PieChart, which is an array of
        // the custom attributes that were declared in attrs.xml.
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.VotationChart,
                0, 0
        );

        try {
            // Retrieve the values from the TypedArray and store into
            // fields of this class.
            //
            // The R.styleable.PieChart_* constants represent the index for
            // each custom attribute in the R.styleable.PieChart array.
            mHighlightStrength = a.getFloat(R.styleable.VotationChart_highlightStrength, 1.0f);
            mPieRotation = a.getInt(R.styleable.VotationChart_pieRotation, 0);
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }

        init();
    }



    /**
     * Set the current item by index. Optionally, scroll the current item into view. This version
     * is for internal use--the scrollIntoView option is always true for external callers.
     *
     * @param currentItemKey    The key of the current item.
     * @param scrollIntoView True if the pie should rotate until the current item is centered.
     *                       False otherwise. If this parameter is false, the pie rotation
     *                       will not change.
     */
    private void setCurrentItem(String currentItemKey, boolean scrollIntoView) {
        mCurrentItemLabel = currentItemKey;
        if (mCurrentItemChangedListener != null) {
            mCurrentItemChangedListener.OnCurrentItemChanged(this, currentItemKey);
        }

        invalidate();
    }



    /**
     * Add a new data item to this view. Adding an item adds a slice to the pie whose
     * size is proportional to the item's value. As new items are added, the size of each
     * existing slice is recalculated so that the proportions remain correct.
     *
     * @param label The label text to be shown when this item is selected.
     * @param value The value of this item.
     * @param color The ARGB color of the pie slice associated with this item.
     * @return The index of the newly added item.
     */
    public int addItem(String label, float value, int color) {
        Item it = new Item();
        it.mColor = color;
        it.mValue = value;

        // Calculate the highlight color. Saturate at 0xff to make sure that high values
        // don't result in aliasing.
        it.mHighlight = Color.argb(
                0xff,
                Math.min((int) (mHighlightStrength * (float) Color.red(color)), 0xff),
                Math.min((int) (mHighlightStrength * (float) Color.green(color)), 0xff),
                Math.min((int) (mHighlightStrength * (float) Color.blue(color)), 0xff)
        );
        mTotal += value;

        mData.put(label,it);

        onDataChanged();

        return mData.size();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing. Do not call the superclass method--that would start a layout pass
        // on this view's children. PieChart lays out its children in onSizeChanged().
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the shadow
        Log.d("VotingChart", "VotingChart " + mShadowBounds.toString());
        canvas.drawOval(mShadowBounds, mShadowPaint);

    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for a width based on our minimum
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();

        int w = Math.max(minw, MeasureSpec.getSize(widthMeasureSpec));

        // Whatever the width ends up being, ask for a height that would let the pie
        // get as big as it can
        int minh = w + getPaddingBottom() + getPaddingTop();
        int h = Math.min(MeasureSpec.getSize(heightMeasureSpec), minh);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //
        // Set dimensions for text, pie chart, etc
        //
        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());


        float ww = (float) w - xpad;
        float hh = (float) h - ypad;

        // Figure out how big we can make the pie.
        float diameter = Math.min(ww, hh);
        mPieBounds = new RectF(
                0.0f,
                0.0f,
                diameter,
                diameter);
        mPieBounds.offsetTo(getPaddingLeft(), getPaddingTop());


        mShadowBounds = new RectF(
                mPieBounds.left + 10,
                mPieBounds.bottom + 10,
                mPieBounds.right - 10,
                mPieBounds.bottom + 20);

        // Lay out the child view that actually draws the pie.
        mPieView.layout((int) mPieBounds.left,
                (int) mPieBounds.top,
                (int) mPieBounds.right,
                (int) mPieBounds.bottom);

        onDataChanged();
    }

    /**
     * Calculate which pie slice is under the pointer, and set the current item
     * field accordingly.
     */
    private void calcCurrentItem() {
        int pointerAngle = (mPieRotation) % 360;
        for(Map.Entry<String, Item> entry : mData.entrySet()) {
            Item it = entry.getValue();
            if (it.mStartAngle <= pointerAngle && pointerAngle <= it.mEndAngle) {
                String key = entry.getKey();
                if (!key.equals(mCurrentItemLabel)) {
                    setCurrentItem(key, false);
                }
                break;
            }
        }
    }

    /**
     * Do all of the recalculations needed when the data array changes.
     */
    private void onDataChanged() {
        // When the data changes, we have to recalculate
        // all of the angles.
        int currentAngle = 0;
        for(Map.Entry<String, Item> entry : mData.entrySet()) {

            Item it = entry.getValue();
            it.mStartAngle = currentAngle;
            it.mEndAngle = (int) ((float) currentAngle + it.mValue * 360.0f / mTotal);
            currentAngle = it.mEndAngle;
            Log.d("PieView", "mAngles " + it.mStartAngle + "**"
                    + it.mEndAngle);

            // Recalculate the gradient shaders. There are
            // three values in this gradient, even though only
            // two are necessary, in order to work around
            // a bug in certain versions of the graphics engine
            // that expects at least three values if the
            // positions array is non-null.
            //
            it.mShader = new SweepGradient(
                    mPieBounds.width() / 2.0f,
                    mPieBounds.height() / 2.0f,
                    new int[]{
                            it.mHighlight,
                            it.mHighlight,
                            it.mColor,
                            it.mColor,
                    },
                    new float[]{
                            0,
                            (float) (360 - it.mEndAngle) / 360.0f,
                            (float) (360 - it.mStartAngle) / 360.0f,
                            1.0f
                    }
            );
        }


        calcCurrentItem();
    }

    /**
     * Initialize the control. This code is in a separate method so that it can be
     * called from both constructors.
     */
    private void init() {
        // Force the background to software rendering because otherwise the Blur
        // filter won't work.
        setLayerToSW(this);


        // Set up the paint for the pie slices
        mPiePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPiePaint.setStyle(Paint.Style.FILL);

        // Set up the paint for the shadow
        mShadowPaint = new Paint(0);
        mShadowPaint.setColor(0xff101010);
        mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

        // Add a child view to draw the pie. Putting this in a child view
        // makes it possible to draw it on a separate hardware layer that rotates
        // independently
        mPieView = new PieView(getContext());
        addView(mPieView);


        // In edit mode it's nice to have some demo data, so add that here.
        if (this.isInEditMode()) {
            Resources res = getResources();
            addItem("GREEN", 3, res.getColor(R.color.italy_green));
            addItem("RED", 4, res.getColor(R.color.italy_red));
            addItem("WHITE", 2, res.getColor(R.color.italy_white));
        }

    }


    private void setLayerToSW(View v) {
        if (!v.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    /**
     * Internal child class that draws the pie chart onto a separate hardware layer
     * when necessary.
     */
    private class PieView extends View {
        // Used for SDK < 11
        private float mRotation = 0;
        private Matrix mTransform = new Matrix();
        private PointF mPivot = new PointF();

        /**
         * Construct a PieView
         *
         * @param context
         */
        public PieView(Context context) {
            super(context);
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (Build.VERSION.SDK_INT < 11) {
                mTransform.set(canvas.getMatrix());
                mTransform.preRotate(mRotation, mPivot.x, mPivot.y);
                canvas.setMatrix(mTransform);
            }


            for (Map.Entry<String, Item> entry  : mData.entrySet()) {
                Item it = entry.getValue();
                mPiePaint.setShader(it.mShader);
                canvas.drawArc(mBounds,
                        360 - it.mEndAngle,
                        it.mEndAngle - it.mStartAngle,
                        true, mPiePaint);

            }

        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            Log.d("PieView", "onSizeChanged " + w + "**"
                    +h);

            mBounds = new RectF(0, 0, w, h);
        }

        RectF mBounds;

        public void setPivot(float x, float y) {
            mPivot.x = x;
            mPivot.y = y;
            if (Build.VERSION.SDK_INT >= 11) {
                setPivotX(x);
                setPivotY(y);
            } else {
                invalidate();
            }
        }
    }



    /**
     * Maintains the state for a data item.
     */
    private class Item {
        public float mValue;
        public int mColor;

        // computed values
        public int mStartAngle;
        public int mEndAngle;

        public int mHighlight;
        public Shader mShader;
    }


    public void reset() {
        mTotal = 0;
    }

}
