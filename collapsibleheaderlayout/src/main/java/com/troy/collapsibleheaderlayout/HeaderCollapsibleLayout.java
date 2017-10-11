package com.troy.collapsibleheaderlayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;

/*
*  To function correctly, the bottom view has to be an implementation of NestedScrollingChild.
*  If not, please wrap your bottom view with a NestedScrollView
*/
public class HeaderCollapsibleLayout extends LinearLayout implements NestedScrollingParent, NestedScrollingChild, OnGlobalLayoutListener
{
    public static final int COLLAPSING = 1;
    public static final int COLLAPSED = 2;
    public static final int EXPANDING = 3;
    public static final int EXPANDED = 4;

    @IntDef({COLLAPSING, COLLAPSED, EXPANDING, EXPANDED})
    public @interface HeaderStatus
    {
    }

    private Context mContext;
    private OnViewFinishInflateListener mViewFinishInflateListener;
    private OnHeaderStatusChangedListener mHeaderStatusChangedListener;
    private int mScrollOffsetHeight = -1;
    private int mScrollOffsetHeightBackup = -1;
    private int mOverlayFooterLayoutId = -1;
    private boolean mSupportAutoExpand = true;
    private boolean mIsEnabled = true;
    @HeaderStatus
    private int mCurHeaderStatus;
    private ViewGroup mTopView;
    private ViewGroup mBottomView;

    private NestedScrollingParentHelper mParentHelper;
    private NestedScrollingChildHelper mChildHelper;

    public HeaderCollapsibleLayout(Context context)
    {
        super(context);

        init(context, null);
    }

    public HeaderCollapsibleLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        init(context, attrs);
    }

    public HeaderCollapsibleLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs)
    {
        mContext = context;

        mCurHeaderStatus = EXPANDED;

        setOrientation(VERTICAL);

        initStyleable(context, attrs);

        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);

        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if(getViewTreeObserver().isAlive()) getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if(getViewTreeObserver().isAlive()) getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout()
    {
        if (mScrollOffsetHeight == -1)
        {
            if (mTopView == null)
            {
                return;
            }

            mScrollOffsetHeight = mTopView.getMeasuredHeight();

            if (mOverlayFooterLayoutId != -1)
            {
                View overlayFooter = mTopView.findViewById(mOverlayFooterLayoutId);

                if (overlayFooter != null)
                {
                    mScrollOffsetHeight = mScrollOffsetHeight - overlayFooter.getMeasuredHeight();
                }
            }

            mScrollOffsetHeightBackup = mScrollOffsetHeight;

            if (mViewFinishInflateListener != null)
            {
                mViewFinishInflateListener.onViewFinishInflate();
            }

            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        if(mScrollOffsetHeight == -1)
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            return;
        }

        int height = MeasureSpec.getSize(heightMeasureSpec);

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height + mScrollOffsetHeight, MeasureSpec.EXACTLY));
    }

    private void initStyleable(Context context, AttributeSet attrs)
    {
        if (attrs == null)
        {
            return;
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HeaderCollapsibleLayout, 0, 0);

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_topPanelLayoutId))
        {
            initTopView(a.getResourceId(R.styleable.HeaderCollapsibleLayout_topPanelLayoutId, -1), this);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_bottomPanelLayoutId))
        {
            initBottomView(a.getResourceId(R.styleable.HeaderCollapsibleLayout_bottomPanelLayoutId, -1), this);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_overlayFooterLayoutId))
        {
            mOverlayFooterLayoutId = a.getResourceId(R.styleable.HeaderCollapsibleLayout_overlayFooterLayoutId, -1);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_supportAutoExpand))
        {
            mSupportAutoExpand = a.getBoolean(R.styleable.HeaderCollapsibleLayout_supportAutoExpand, true);
        }

        if (mTopView != null) addView(mTopView);

        if (mBottomView != null) addView(mBottomView);

        a.recycle();
    }

    private void initTopView(int layoutId, ViewGroup parent)
    {
        if (layoutId == -1) return;

        mTopView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    private void initBottomView(int layoutId, ViewGroup parent)
    {
        if (layoutId == -1) return;

        mBottomView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    /*********
     * Public methods starts
     ****************/
    public void setOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback)
    {
        this.mHeaderStatusChangedListener = callback;
    }

    public void removeOnHeaderStatusChangedListener()
    {
        this.mHeaderStatusChangedListener = null;
    }

    public void setOnViewFinishInflateListener(OnViewFinishInflateListener listener)
    {
        mViewFinishInflateListener = listener;
    }

    public void removeOnViewFinishInflateListener()
    {
        mViewFinishInflateListener = null;
    }

    public void reset()
    {
        mCurHeaderStatus = EXPANDED;
    }

    @HeaderStatus
    public int getCurrentHeaderStatus()
    {
        return mCurHeaderStatus;
    }

    public void collapse()
    {
        scrollTo(0, mScrollOffsetHeight);

        mCurHeaderStatus = COLLAPSED;

        lastVelocityY = 0.1f; //To make sure next fling action performs well
    }

    public void smoothCollapse()
    {
        smoothScrollTo(0, mScrollOffsetHeight, new AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                mCurHeaderStatus = COLLAPSING;

                if(mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderStartCollapsing();
            }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                mCurHeaderStatus = COLLAPSED;

                if(mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderCollapsed();
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {

            }

            @Override
            public void onAnimationRepeat(Animator animation)
            {

            }
        });

        lastVelocityY = 0.1f; //To make sure next fling action performs well
    }

    public void expand()
    {
        scrollTo(0, 0);

        mCurHeaderStatus = EXPANDED;

        lastVelocityY = -0.1f; //To make sure next fling action performs well
    }

    public void smoothExpand()
    {
        smoothScrollTo(0, 0, new AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation)
            {
                mCurHeaderStatus = EXPANDING;

                if(mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderStartExpanding();
            }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                mCurHeaderStatus = EXPANDED;

                if(mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderExpanded();
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {

            }

            @Override
            public void onAnimationRepeat(Animator animation)
            {

            }
        });

        lastVelocityY = -0.1f; //To make sure next fling action performs well
    }

    public void disableCollapsing()
    {
        if(mScrollOffsetHeight != 0)
        {
            mScrollOffsetHeightBackup = mScrollOffsetHeight;

            mScrollOffsetHeight = 0;
        }

        mIsEnabled = false;
    }

    public void enableCollapsing()
    {
        mScrollOffsetHeight = mScrollOffsetHeightBackup;

        mIsEnabled = true;
    }

    public boolean isEnabled()
    {
        return mIsEnabled;
    }

    /*********
     * Public methods ends
     ****************/

    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled)
    {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled()
    {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes)
    {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll()
    {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent()
    {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow)
    {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow)
    {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed)
    {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY)
    {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    // NestedScrollingParent

    /*
    *  stop intercepting nested scroll event when header layout has been shown or hidden.
    */
    private boolean shouldConsumeNestedScroll(int dy)
    {
        if (dy > 0)
        {
            return getScrollY() < mScrollOffsetHeight;
        }
        else
        {
            return getScrollY() > 0;
        }
    }

    /*
    *  prevent the view to be over scrolled by a long drag move
    */
    private boolean isOverScroll(int dy)
    {
        if (dy < 0)
        {
            return Math.abs(dy) > getScrollY();
        }
        else
        {
            return dy > mScrollOffsetHeight - getScrollY();
        }
    }

    private void smoothScrollTo(int desX, int desY, AnimatorListener listener)
    {
        ObjectAnimator xTranslate = ObjectAnimator.ofInt(this, "scrollX", desX);
        ObjectAnimator yTranslate = ObjectAnimator.ofInt(this, "scrollY", desY);

        yTranslate.addUpdateListener(new AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderOffsetChanged((int) (mScrollOffsetHeight * animation.getAnimatedFraction()), animation.getAnimatedFraction());
            }
        });

        AnimatorSet animators = new AnimatorSet();
        animators.setDuration(240L);
        animators.playTogether(xTranslate, yTranslate);
        if (listener != null) animators.addListener(listener);
        animators.start();
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes)
    {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes)
    {
        mParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);

        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View target)
    {
        mParentHelper.onStopNestedScroll(target);

        stopNestedScroll();
    }

    /*
    *  The sequence of the below callbacks should be onNestedPreScroll --> onNestedScroll --> onNestedPreFling --> onNestedFling
    *  The fling related callbacks would only be called when a fling event detected.
    */

    /*
    *  When scrolling down and dyUnconsumed is a non-zero value, means the child has consumed part of the scrolling event,
    *  here should expand the header.
    *  Also see onNestedPreScroll(View target, int dx, int dy, int[] consumed)
    */
    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    {
        unconsumedDy = dyUnconsumed;

        final int oldScrollY = getScrollY();

        if (dyUnconsumed < 0 && oldScrollY <= 0 && mIsEnabled) //Scrolling down and header has totally expanded
        {
            if (mCurHeaderStatus != EXPANDED)
            {
                if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderExpanded();

                mCurHeaderStatus = EXPANDED;
            }
        }

        int actualPerformedDy;
        boolean isOverScroll;

        if (oldScrollY > 0 && oldScrollY <= mScrollOffsetHeight)
        {
            if (isOverScroll = isOverScroll(dyUnconsumed))
            {
                if (dyUnconsumed < 0)
                    actualPerformedDy = -getScrollY();
                else
                    actualPerformedDy = mScrollOffsetHeight - getScrollY();
            }
            else
            {
                actualPerformedDy = dyUnconsumed;
            }

            scrollBy(0, actualPerformedDy);

            if (dyUnconsumed < 0) //Scrolling down, and child has consumed part of(not all) the scrolling event
            {
                if (oldScrollY <= mScrollOffsetHeight * 0.88 && mIsEnabled) //Give 12% buffer height here when sending out the expanding event, for better user experience
                {
                    if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderOffsetChanged(getScrollY(), (getScrollY() * 1.0f) / mScrollOffsetHeight);

                    if (mCurHeaderStatus != EXPANDING)
                    {
                        if (mCurHeaderStatus == COLLAPSED)
                        {
                            if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderStartExpanding();

                            mCurHeaderStatus = EXPANDING;
                        }
                    }
                }
            }

            final int myConsumed = isOverScroll ? actualPerformedDy : getScrollY() - oldScrollY;
            final int myUnconsumed = actualPerformedDy - myConsumed;

            dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null);
        }
    }

    /*
    *  When scrolling up, first intercept the scrolling event to collapse the header, then give the event back to its child
    *  When scrolling down, let the child consume the scrolling first
    *  Also see onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
    */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed)
    {
        if (!dispatchNestedPreScroll(dx, dy, consumed, null))
        {
            if (dy < 0) return;   //Scrolling down event would not be handled here

            if (!shouldConsumeNestedScroll(dy)) return;

            if (mCurHeaderStatus != COLLAPSING)
            {
                if (getScrollY() >= 0 && getScrollY() < mScrollOffsetHeight && mIsEnabled)
                {
                    if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderStartCollapsing();

                    mCurHeaderStatus = COLLAPSING;
                }
            }

            int actualPerformedDy;

            if (isOverScroll(dy))
            {
                if (dy < 0)
                    actualPerformedDy = getScrollY();
                else
                    actualPerformedDy = mScrollOffsetHeight - getScrollY();
            }
            else
            {
                actualPerformedDy = dy;
            }

            scrollBy(0, actualPerformedDy);

            if (mHeaderStatusChangedListener != null && mIsEnabled) mHeaderStatusChangedListener.onHeaderOffsetChanged(getScrollY(), (getScrollY() * 1.0f) / mScrollOffsetHeight);

            if (dy > 0 && getScrollY() >= mScrollOffsetHeight && mIsEnabled)
            {
                if (mCurHeaderStatus != COLLAPSED)
                {
                    if (mHeaderStatusChangedListener != null) mHeaderStatusChangedListener.onHeaderCollapsed();

                    mCurHeaderStatus = COLLAPSED;
                }
            }

            consumed[0] = 0;
            consumed[1] = dy;
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed)
    {
        return !consumed;
    }

    private float lastVelocityY = -0.1f;
    private int unconsumedDy;

    /*
    *  When fling up and last-time fling was down side, smoothly collapse the header.
    */
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY)
    {
        if (velocityY > 0 && lastVelocityY < 0)
        {
            if(mCurHeaderStatus != COLLAPSED)
            {
                //Smoothly collapsing the header
                smoothScrollTo(0, mScrollOffsetHeight, new AnimatorListener()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        mCurHeaderStatus = COLLAPSING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        if (mHeaderStatusChangedListener != null && mIsEnabled)
                        {
                            mHeaderStatusChangedListener.onHeaderCollapsed();
                        }

                        mCurHeaderStatus = COLLAPSED;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation)
                    {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation)
                    {

                    }
                });
            }

            lastVelocityY = velocityY;

            return true;
        }
        else if (velocityY < 0 && unconsumedDy < 0) //Fling down and has unconsumed vertical value, should handle this fling
        {
            if (mSupportAutoExpand && mCurHeaderStatus != EXPANDED)
            {
                //Smoothly expanding the header, related callbacks would be called in onNestedScroll
                smoothScrollTo(0, 0, new AnimatorListener()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        mCurHeaderStatus = EXPANDING;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation)
                    {
                        if (mHeaderStatusChangedListener != null && mIsEnabled)
                        {
                            mHeaderStatusChangedListener.onHeaderExpanded();
                        }

                        mCurHeaderStatus = EXPANDED;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation)
                    {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation)
                    {

                    }
                });
            }
        }

        lastVelocityY = velocityY;

        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes()
    {
        return mParentHelper.getNestedScrollAxes();
    }

    public interface OnViewFinishInflateListener
    {
        void onViewFinishInflate();
    }

}
