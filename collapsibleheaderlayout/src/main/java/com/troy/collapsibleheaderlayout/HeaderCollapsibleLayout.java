package com.troy.collapsibleheaderlayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/*
 * To function correctly, the bottom view has to be an implementation of NestedScrollingChild.
 * If not, please wrap your bottom view with a NestedScrollView.
 */
public class HeaderCollapsibleLayout extends LinearLayout implements NestedScrollingParent,
        NestedScrollingChild, OnGlobalLayoutListener {

    public interface OnViewFinishInflateListener {
        void onViewFinishInflate();
    }

    @SuppressWarnings("WeakerAccess")
    @IntDef({COLLAPSING, COLLAPSED, EXPANDING, EXPANDED})
    public @interface HeaderStatus {
    }

    public static final int COLLAPSING = 1;
    public static final int COLLAPSED = 2;
    public static final int EXPANDING = 3;
    public static final int EXPANDED = 4;

    private static final Object S_LOCK = HeaderCollapsibleLayout.class;

    protected boolean mIsEnabled = true;
    protected boolean mIsScrollingDown;
    protected boolean mIsBeingDragged;
    @HeaderStatus
    protected int mCurHeaderStatus;

    private Context mContext;
    private OnViewFinishInflateListener mViewFinishInflateListener;
    private List<OnHeaderStatusChangedListener> mHeaderStatusChangedListeners;
    private OnHeaderStatusChangedListener mSingleHeaderStatusChangedListener;
    private int mOrgHeaderHeight = -1;
    private int mOrgHeaderHeightBackup = -1;
    private int mOverlayFooterLayoutId = -1;
    private int mOvershootDistance;
    private boolean mSupportFlingAction;
    private boolean mAutoDrawerModeEnabled = true;
    private boolean mDefaultExpand = true;
    private ViewGroup mTopView;
    private ViewGroup mBottomView;

    private NestedScrollingParentHelper mParentHelper;
    private NestedScrollingChildHelper mChildHelper;
    private Animator mBounceBackForOvershooting;
    private int lastHeaderHeight;  // Record of header height each time before it changes

    private float lastVelocityY = -0.1F;
    private int unconsumedDy;

    public HeaderCollapsibleLayout(Context context) {
        super(context);

        init(context, null);
    }

    public HeaderCollapsibleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public HeaderCollapsibleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;

        mCurHeaderStatus = EXPANDED;

        setOrientation(VERTICAL);

        initStyleable(context, attrs);

        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);

        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        ViewTreeObserver observer = getViewTreeObserver();
        if (observer.isAlive()) {
            observer.addOnGlobalLayoutListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ViewTreeObserver observer = getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeGlobalOnLayoutListener(this);
        }
    }

    @Override
    final public void onGlobalLayout() {
        if (mOrgHeaderHeight != -1) {
            return;
        }

        if (mTopView == null || mTopView.getMeasuredHeight() == 0) {
            return;
        }

        mOrgHeaderHeight = mTopView.getMeasuredHeight();
        if (mOvershootDistance < 0) {
            mOvershootDistance = 0;
        } else if (mOvershootDistance > (Integer.MAX_VALUE - getHeight())) {
            getHeight();
        }

        if (mOverlayFooterLayoutId != -1) {
            View overlayFooter = mTopView.findViewById(mOverlayFooterLayoutId);

            if (overlayFooter != null) {
                mOrgHeaderHeight = mOrgHeaderHeight - overlayFooter.getMeasuredHeight();
            }
        }

        mOrgHeaderHeightBackup = mOrgHeaderHeight;

        if (mViewFinishInflateListener != null) {
            mViewFinishInflateListener.onViewFinishInflate();
        }

        onFirstLayout();

        requestLayout();

        if (!mDefaultExpand) {
            collapse();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        if (mOrgHeaderHeight == -1) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//            return;
//        }
//
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//
//        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height + mOrgHeaderHeight, MeasureSpec.EXACTLY));
    }

    protected void onFirstLayout() {
    }

    private void initStyleable(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HeaderCollapsibleLayout, 0, 0);

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_topPanelLayoutId)) {
            initTopView(a.getResourceId(R.styleable.HeaderCollapsibleLayout_topPanelLayoutId, -1), this);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_bottomPanelLayoutId)) {
            initBottomView(a.getResourceId(R.styleable.HeaderCollapsibleLayout_bottomPanelLayoutId, -1), this);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_overlayFooterLayoutId)) {
            mOverlayFooterLayoutId = a.getResourceId(R.styleable.HeaderCollapsibleLayout_overlayFooterLayoutId, -1);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_supportFlingAction)) {
            mSupportFlingAction = a.getBoolean(R.styleable.HeaderCollapsibleLayout_supportFlingAction, false);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_autoDrawerModeEnabled)) {
            mAutoDrawerModeEnabled = a.getBoolean(R.styleable.HeaderCollapsibleLayout_autoDrawerModeEnabled, true);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_defaultExpand)) {
            mDefaultExpand = a.getBoolean(R.styleable.HeaderCollapsibleLayout_defaultExpand, true);
        }

        if (a.hasValue(R.styleable.HeaderCollapsibleLayout_overshootDistance)) {
            mOvershootDistance = a.getInteger(R.styleable.HeaderCollapsibleLayout_overshootDistance, 0);
        }

        if (mTopView != null) {
            addView(mTopView);
        }

        if (mBottomView != null) {
            addView(mBottomView);
        }

        a.recycle();
    }

    private void initTopView(int layoutId, ViewGroup parent) {
        if (layoutId == -1) {
            return;
        }

        mTopView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    private void initBottomView(int layoutId, ViewGroup parent) {
        if (layoutId == -1) {
            return;
        }

        mBottomView = (ViewGroup) LayoutInflater.from(mContext).inflate(layoutId, parent, false);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ▼ Public methods starts
    ///////////////////////////////////////////////////////////////////////////

    @Deprecated
    public void setOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback) {
        mSingleHeaderStatusChangedListener = callback;
    }

    @Deprecated
    public void removeOnHeaderStatusChangedListener() {
        mSingleHeaderStatusChangedListener = null;
    }

    public void addOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback) {
        synchronized (S_LOCK) {
            if (mHeaderStatusChangedListeners == null) {
                mHeaderStatusChangedListeners = new ArrayList<>();
            }
            if (mHeaderStatusChangedListeners.contains(callback)) {
                return;
            }
            mHeaderStatusChangedListeners.add(callback);
        }
    }

    public void removeOnHeaderStatusChangedListener(OnHeaderStatusChangedListener listener) {
        if (mHeaderStatusChangedListeners == null) {
            return;
        }
        mHeaderStatusChangedListeners.remove(listener);
    }

    public void setOnViewFinishInflateListener(OnViewFinishInflateListener listener) {
        mViewFinishInflateListener = listener;
    }

    public void removeOnViewFinishInflateListener() {
        mViewFinishInflateListener = null;
    }

    /**
     * Reset status.
     */
    public void reset() {
        mCurHeaderStatus = EXPANDED;
    }

    @HeaderStatus
    public int getCurrentHeaderStatus() {
        return mCurHeaderStatus;
    }

    public void collapse() {
        changeHeaderHeightTo(0);

        mCurHeaderStatus = COLLAPSED;

        lastVelocityY = 0.1F;  // To make sure next fling action performs well
    }

    public void smoothCollapse() {
        smoothChangeHeaderHeightTo(0, new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCurHeaderStatus = COLLAPSING;

                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderStartCollapsing();
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderStartCollapsing();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCurHeaderStatus = COLLAPSED;

                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderCollapsed();
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderCollapsed();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        lastVelocityY = 0.1F;  // To make sure next fling action performs well
    }

    public View getTopView() {
        return mTopView;
    }

    public void expand() {
        changeHeaderHeightTo(mOrgHeaderHeight);

        mCurHeaderStatus = EXPANDED;

        lastVelocityY = -0.1F;  // To make sure next fling action performs well
    }

    public void smoothExpand() {
        smoothChangeHeaderHeightTo(mOrgHeaderHeight, new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCurHeaderStatus = EXPANDING;

                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderStartExpanding();
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderStartExpanding();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mCurHeaderStatus = EXPANDED;

                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderExpanded();
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderExpanded();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        lastVelocityY = -0.1F; // To make sure next fling action performs well
    }

    public void disableCollapsing() {
        if (mOrgHeaderHeight != 0) {
            mOrgHeaderHeightBackup = mOrgHeaderHeight;
            mOrgHeaderHeight = 0;
        }

        mIsEnabled = false;
    }

    public void enableCollapsing() {
        mOrgHeaderHeight = mOrgHeaderHeightBackup;
        mIsEnabled = true;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    /* ▲ Public methods ends */

    ///////////////////////////////////////////////////////////////////////////
    // NestedScrollingChild
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    ///////////////////////////////////////////////////////////////////////////
    // NestedScrollingParent
    ///////////////////////////////////////////////////////////////////////////

    /*
     * stop intercepting nested scroll event when header layout has been shown or hidden.
     */
    private boolean shouldConsumeNestedScroll(int dy) {
        if (dy > 0) {
            //return getScrollY() < mOrgHeaderHeight;
            return mTopView.getHeight() > 0;
        } else {
            //return getScrollY() > -mOvershootDistance;
            return mTopView.getHeight() < mOrgHeaderHeight + mOvershootDistance;
        }
    }

    /*
     * prevent the view to be over scrolled by a long drag move
     */
    private boolean isReachedEdge(int dy) {
        if (dy > 0) {
            //return dy > (mOrgHeaderHeight - getScrollY());
            return dy > mTopView.getHeight();
        } else {
            //return Math.abs(dy) > (getScrollY() + mOvershootDistance);
            return Math.abs(dy) > (mOrgHeaderHeight + mOvershootDistance) - mTopView.getHeight();

        }
    }

    private void changeHeaderHeightTo(int desHeight) {
        LayoutParams temp = (LayoutParams) mTopView.getLayoutParams();
        temp.height = desHeight;
        mTopView.setLayoutParams(temp);
    }

    private Animator smoothScrollTo(int desY, long duration, AnimatorListener listener) {
        ObjectAnimator yTranslate = ObjectAnimator.ofInt(this, "scrollY", desY);
        yTranslate.setInterpolator(new DecelerateInterpolator());
        yTranslate.setDuration(duration);
        if (listener != null) {
            yTranslate.addListener(listener);
        }
        yTranslate.start();
        return yTranslate;
    }

    private Animator smoothChangeHeaderHeightTo(int desHeight, AnimatorListener listener) {
        return smoothChangeHeaderHeightTo(desHeight, 300L, listener);
    }

    private Animator smoothChangeHeaderHeightTo(int desHeight, long duration, @Nullable AnimatorListener listener) {
        /*
		ObjectAnimator xTranslate = ObjectAnimator.ofInt(this, "scrollX", desX);
		ObjectAnimator yTranslate = ObjectAnimator.ofInt(this, "scrollY", desY);

		yTranslate.setInterpolator(new DecelerateInterpolator());
		yTranslate.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				if (mHeaderStatusChangedListeners != null)
					for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
						l.onHeaderOffsetChanged(getScrollY(), mOrgHeaderHeight,
								(getScrollY() * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
					}

			}
		});
		*/
        if (desHeight < 0) {
            return null;
        }

        final LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(mTopView.getLayoutParams());
        final LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(mTopView.getLayoutParams());
        endParams.height = desHeight;
        ValueAnimator animator = ValueAnimator.ofObject(new TypeEvaluator<LayoutParams>() {
            @Override
            public LayoutParams evaluate(float fraction, LayoutParams startValue, LayoutParams endValue) {
                LayoutParams temp = (LayoutParams) mTopView.getLayoutParams();
                lastHeaderHeight = mTopView.getHeight();
                temp.height = (int) (startValue.height + (endValue.height - startValue.height) * fraction);
                return temp;
            }
        }, startParams, endParams);

        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LayoutParams layoutParams = (LayoutParams) animation.getAnimatedValue();
                if (mTopView != null) {
                    mTopView.setLayoutParams(layoutParams);
                }
                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderOffsetChanged(mOrgHeaderHeight - layoutParams.height, mOrgHeaderHeight,
                                ((mOrgHeaderHeight - layoutParams.height) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderOffsetChanged(mOrgHeaderHeight - layoutParams.height, mOrgHeaderHeight,
                            ((mOrgHeaderHeight - layoutParams.height) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                }
            }
        });
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();

        /*
		AnimatorSet animators = new AnimatorSet();
		animators.setDuration(duration);
		animators.playTogether(xTranslate, yTranslate);
		if (listener != null) animators.addListener(listener);
		animators.start();
        */

        return animator;
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);

        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onStopNestedScroll(View target) {
        lastHeaderHeight = 0;
        mIsBeingDragged = false;
        mParentHelper.onStopNestedScroll(target);
        stopNestedScroll();

        if (mOvershootDistance > 0
                && ((mTopView.getHeight() > mOrgHeaderHeight) || (mOrgHeaderHeight == 0
                && getScrollY() < 0))) {
            if (mBounceBackForOvershooting != null && mBounceBackForOvershooting.isStarted()) {
                mBounceBackForOvershooting.cancel();
            }
            if (mOrgHeaderHeight > 0 && mTopView.getHeight() > mOrgHeaderHeight) {
                mBounceBackForOvershooting = smoothChangeHeaderHeightTo(mOrgHeaderHeight, 600L, null);
            } else if (mOrgHeaderHeight == 0 && getScrollY() < 0) {
                mBounceBackForOvershooting = smoothScrollTo(mOrgHeaderHeight, 600L, null);
            }
            return;
        }

        if (!mAutoDrawerModeEnabled || mCurHeaderStatus == EXPANDED || mCurHeaderStatus == COLLAPSED) {
            return;
        }
        // Drawer adsorb effect
        if (mIsScrollingDown) {
            smoothChangeHeaderHeightTo(mOrgHeaderHeight, new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIsEnabled) {
                        if (mHeaderStatusChangedListeners != null) {
                            for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                                l.onHeaderExpanded();
                            }
                        }

                        if (mSingleHeaderStatusChangedListener != null) {
                            mSingleHeaderStatusChangedListener.onHeaderExpanded();
                        }
                    }

                    mCurHeaderStatus = EXPANDED;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        } else {
            smoothChangeHeaderHeightTo(0, new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mIsEnabled) {
                        if (mHeaderStatusChangedListeners != null) {
                            for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                                l.onHeaderCollapsed();
                            }
                        }

                        if (mSingleHeaderStatusChangedListener != null) {
                            mSingleHeaderStatusChangedListener.onHeaderCollapsed();
                        }
                    }

                    mCurHeaderStatus = COLLAPSED;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
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
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        unconsumedDy = dyUnconsumed;

        //final int oldScrollY = getScrollY();
        final int headerHeight = mTopView.getHeight();

        //if (dyUnconsumed < 0 && oldScrollY <= 0 && mIsEnabled) //Scrolling down and header has totally expanded
        if (dyUnconsumed < 0 && headerHeight >= mOrgHeaderHeight && mIsEnabled) {
            if (mCurHeaderStatus != EXPANDED) {
                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderExpanded();
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderExpanded();
                }

                mCurHeaderStatus = EXPANDED;
            }
        }

        int actualPerformedDy;
        int actualConsumedDy;
        boolean isReachedEdge;

        //if (oldScrollY > -mOvershootDistance && oldScrollY <= mOrgHeaderHeight) {
        if (headerHeight >= 0 && headerHeight < mOrgHeaderHeight + mOvershootDistance) {
            if (isReachedEdge = isReachedEdge(dyUnconsumed)) {
                if (dyUnconsumed < 0) {
                    //actualPerformedDy = -(getScrollY() + mOvershootDistance);
                    actualPerformedDy = -(mOrgHeaderHeight - headerHeight + mOvershootDistance);
                } else {
                    //actualPerformedDy = mOrgHeaderHeight - getScrollY();
                    actualPerformedDy = headerHeight;
                }
                actualConsumedDy = actualPerformedDy;
            } else {
                if (headerHeight > mOrgHeaderHeight) {  // The layout has already been dragged to overshoot
                    actualPerformedDy = dyUnconsumed / 3;
                } else {
                    actualPerformedDy = dyUnconsumed;
                }
                actualConsumedDy = dyUnconsumed;
            }

            // The value of actualConsumedDy and actualPerformedDy can be different only when in overshoot mode
            //scrollBy(0, actualPerformedDy);
            if (actualPerformedDy != 0 && mIsBeingDragged) {
                final LayoutParams tempLp = (LayoutParams) mTopView.getLayoutParams();
                lastHeaderHeight = headerHeight;
                tempLp.height = headerHeight - actualPerformedDy;
                mTopView.setLayoutParams(tempLp);
            }

            // Scrolling down, and child has consumed part of(not all) the scrolling event
            if (dyUnconsumed < 0) {

                //if (oldScrollY <= mOrgHeaderHeight * 0.88 && mIsEnabled) //Give 12% buffer height here when sending out the expanding event, for better user experience
                if (headerHeight >= mOrgHeaderHeight * 0.12 && mIsEnabled) {
                    if (mHeaderStatusChangedListeners != null) {
                        for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                            l.onHeaderOffsetChanged(mOrgHeaderHeight - headerHeight, mOrgHeaderHeight,
                                    ((mOrgHeaderHeight - headerHeight) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                        }
                    }

                    if (mSingleHeaderStatusChangedListener != null) {
                        mSingleHeaderStatusChangedListener.onHeaderOffsetChanged(mOrgHeaderHeight - headerHeight, mOrgHeaderHeight,
                                ((mOrgHeaderHeight - headerHeight) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                    }

                    if (mCurHeaderStatus != EXPANDING) {
                        if (mCurHeaderStatus == COLLAPSED) {
                            if (mHeaderStatusChangedListeners != null) {
                                for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                                    l.onHeaderStartExpanding();
                                }
                            }

                            if (mSingleHeaderStatusChangedListener != null) {
                                mSingleHeaderStatusChangedListener.onHeaderStartExpanding();
                            }

                            mCurHeaderStatus = EXPANDING;
                        }
                    }
                }
            }

            //int myConsumed = isReachedEdge ? actualConsumedDy : getScrollY() - oldScrollY;
            int myConsumed = isReachedEdge ? actualConsumedDy : actualPerformedDy;
            int myUnconsumed = dyUnconsumed - myConsumed;

            dispatchNestedScroll(0, myConsumed, 0, myUnconsumed, null);
        } else if (mOrgHeaderHeight == 0 && mOvershootDistance > 0 && getScrollY() > -mOvershootDistance) {
            actualPerformedDy = dyUnconsumed / 3;
            scrollBy(0, actualPerformedDy);
            dispatchNestedScroll(0, actualPerformedDy, 0, dyUnconsumed - actualPerformedDy, null);
        }
    }

    /*
     * When scrolling up, first intercept the scrolling event to collapse the header, then give the event back to its child
     * When scrolling down, let the child consume the scrolling first
     * Also see onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed)
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        int fixedDy = dy;
        if (mIsScrollingDown && mIsBeingDragged) {
            // The body layout height is dynamically changing, and as well as the return of getY() which is a relative value.
            // And thus it will lead to wrong calculation of dy.
            // Here we make a manually adjust for dy value to correct the wrong dy caused by the changing of body height.
            fixedDy = dy < 0 ? dy : dy - Math.abs(lastHeaderHeight - mTopView.getHeight());
        }
        if (Math.abs(fixedDy) > 3) {
            mIsScrollingDown = (fixedDy < 0);
            mIsBeingDragged = true;
        }
        if (!dispatchNestedPreScroll(dx, dy, consumed, null)) {
            if (!shouldConsumeNestedScroll(fixedDy)) {
                return;
            }

            if (mBounceBackForOvershooting != null && mBounceBackForOvershooting.isStarted()) {
                mBounceBackForOvershooting.cancel();
            }
            if (fixedDy < 0) {
                if (fixedDy != dy) {
                    onNestedScroll(this, 0, 0, 0, fixedDy);
                }
                return;  // Scrolling down event would not be handled here
            }

            final int headerHeight = mTopView.getHeight();

            if (mCurHeaderStatus != COLLAPSING) {
                //if (getScrollY() >= -mOvershootDistance && getScrollY() < mOrgHeaderHeight && mIsEnabled) {
                if (headerHeight > 0 && headerHeight < (mOrgHeaderHeight + mOvershootDistance) && mIsEnabled) {
                    if (mHeaderStatusChangedListeners != null) {
                        for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                            l.onHeaderStartCollapsing();
                        }
                    }

                    if (mSingleHeaderStatusChangedListener != null) {
                        mSingleHeaderStatusChangedListener.onHeaderStartCollapsing();
                    }

                    mCurHeaderStatus = COLLAPSING;
                }
            }

            int actualPerformedDy;

            if (isReachedEdge(fixedDy)) {
                actualPerformedDy = headerHeight;

            } else {
                actualPerformedDy = fixedDy;
            }

            //scrollBy(0, actualPerformedDy);
            if (actualPerformedDy != 0 && mIsBeingDragged) {
                final LayoutParams tempLp = (LayoutParams) mTopView.getLayoutParams();
                lastHeaderHeight = mTopView.getHeight();
                tempLp.height = mTopView.getHeight() - actualPerformedDy;
                mTopView.setLayoutParams(tempLp);
            }

            if (mIsEnabled) {
                if (mHeaderStatusChangedListeners != null) {
                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                        l.onHeaderOffsetChanged(mOrgHeaderHeight - headerHeight, mOrgHeaderHeight,
                                ((mOrgHeaderHeight - headerHeight) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                    }
                }

                if (mSingleHeaderStatusChangedListener != null) {
                    mSingleHeaderStatusChangedListener.onHeaderOffsetChanged(mOrgHeaderHeight - headerHeight, mOrgHeaderHeight,
                            ((mOrgHeaderHeight - headerHeight) * 1.0f) / mOrgHeaderHeight, mIsScrollingDown);
                }
            }

            //if (dy > 0 && getScrollY() >= mOrgHeaderHeight && mIsEnabled) {
            if (mTopView.getHeight() == 0 && mIsEnabled) {
                if (mCurHeaderStatus != COLLAPSED) {
                    if (mHeaderStatusChangedListeners != null) {
                        for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                            l.onHeaderCollapsed();
                        }
                    }

                    if (mSingleHeaderStatusChangedListener != null) {
                        mSingleHeaderStatusChangedListener.onHeaderCollapsed();
                    }

                    mCurHeaderStatus = COLLAPSED;
                }
            }

            consumed[0] = 0;
            consumed[1] = dy;
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return !consumed;
    }

    /*
     * When fling up and last-time fling was down side, smoothly collapse the header.
     */
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (mSupportFlingAction) {
            if (velocityY > 0 && lastVelocityY < 0) {
                if (mCurHeaderStatus != COLLAPSED) {
                    // Smoothly collapsing the header
                    smoothChangeHeaderHeightTo(0, new AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mCurHeaderStatus = COLLAPSING;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (mIsEnabled) {
                                if (mHeaderStatusChangedListeners != null) {
                                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                                        l.onHeaderCollapsed();
                                    }
                                }

                                if (mSingleHeaderStatusChangedListener != null) {
                                    mSingleHeaderStatusChangedListener.onHeaderCollapsed();
                                }
                            }

                            mCurHeaderStatus = COLLAPSED;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                }

                lastVelocityY = velocityY;

                return true;
            }

            // Fling down and has unconsumed vertical value, should handle this fling
            if (velocityY < 0 && unconsumedDy < 0) {

                // Smoothly expanding the header, related callbacks would be called in onNestedScroll
                if (mCurHeaderStatus != EXPANDED) {
                    smoothChangeHeaderHeightTo(mOrgHeaderHeight, new AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mCurHeaderStatus = EXPANDING;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (mIsEnabled) {
                                if (mHeaderStatusChangedListeners != null) {
                                    for (OnHeaderStatusChangedListener l : mHeaderStatusChangedListeners) {
                                        l.onHeaderExpanded();
                                    }
                                }

                                if (mSingleHeaderStatusChangedListener != null) {
                                    mSingleHeaderStatusChangedListener.onHeaderExpanded();
                                }
                            }

                            mCurHeaderStatus = EXPANDED;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                }
            }
        }

        lastVelocityY = velocityY;

        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }
}
