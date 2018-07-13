## Description ##
A wrapper layout that can easily split your current layout into header and body, and provides smooth header collapsing action with related event callbacks.

## Demo ##
![](https://github.com/kfrozen/HeaderCollapsibleLayout/raw/master/logo/HCLayoutGif.gif)


## Usage ##
	dependencies {
	    compile 'com.troy.collapsibleheaderlayout:collapsibleheaderlayout:2.0.3'
	}

## Basic steps to wrap your original layout to obtain a collapsible header layout##
1. Split the original layout to header and body, then put them in separate layout files, for example:

		comp_collapsible_layout_header.xml
		comp_collapsible_layout_body.xml

2. Apply the HeaderCollapsibleLayout to your layout file, and link the above two parts in, like this:

		<com.troy.collapsibleheaderlayout.HeaderCollapsibleLayout
            android:id="@+id/default_header_collapsible_layout_id"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:topPanelLayoutId="@layout/comp_collapsible_layout_header"
            app:bottomPanelLayoutId="@layout/comp_collapsible_layout_body"
            app:overlayFooterLayoutId="@+id/demo_bottom_overlay"
            app:overshootDistance="5000"/>
            <!--app:supportAutoExpand="false" />-->

3. You are all set! Ready to soar ---

**WHAT'S NEW IN 2.0.X:**

1. Support **overscroll** action. When the header view has been expanded, if user kept scrolling down, the header view would be stretched to response to the overscroll action, and then bounce back when user released.
To enable the overscroll effect, just give a positive integer to the attribute **app:overshootDistance**, which means the max overscroll distance in pixel.

2. The params of the event callback **onHeaderOffsetChanged** has been changed from void onHeaderOffsetChanged(int verticalOffset, float headerCollapsedPercentage) to void onHeaderOffsetChanged(int verticalOffset, int headerHeight, float headerCollapsedPercentage, boolean isScrollingDown).
For the details of the params, please see below.

3. Deprecated the previous setOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback) method, please use addOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback) as the replacement.

4. Added a new callback **OnViewFinishInflateListener**, which would be invoked when the HeaderCollapsibleLayout has been totally inflated. Please see the details about this below.

**Note that:**

1. If there was NOT a view in your child layout that implemented the *NestedScrollingChild*, please wrap your header/body layout with a *NestedScrollView*.
2. Setting **app:supportAutoExpand="false"** means the header will not automatically expand when user perform a fling action, instead of which the header will move with user's finger until totally expanded.
3. We also provide a set of event callbacks:

		public interface OnHeaderStatusChangedListener
		{
		    void onHeaderStartCollapsing();
		
		    void onHeaderCollapsed();
		
		    void onHeaderStartExpanding();
		
		    void onHeaderExpanded();
            
             /**
             * Called when the {@link HeaderCollapsibleLayout}'s layout offset has been changed. This allows
             * child views to implement custom behavior based on the offset (for instance pinning a
             * view at a certain y value).
             *
             * @param verticalOffset            the vertical offset for the parent {@link HeaderCollapsibleLayout}, in px
             * @param headerHeight				the total collapsible offset, in px
             * @param headerCollapsedPercentage the latest percentage of the collapsed part of the header view.
             * @param isScrollingDown 			whether the layout is scrolling down
             */
             void onHeaderOffsetChanged(int verticalOffset, int headerHeight, float headerCollapsedPercentage, boolean isScrollingDown);
		}

	Add this to your HeaderCollapsibleLayout instance by calling the below method in case you wanna do something response to the collapsing events. Please remember to remove it when not needed anymore.

		public void addOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback)
		
		public void removeOnHeaderStatusChangedListener(OnHeaderStatusChangedListener listener)
		
	Also, we provide a callback when the wrapper view has been inflated, where you may need to do some initialize actions. Apply the below callbacks:
	    
	    public void setOnViewFinishInflateListener(OnViewFinishInflateListener listener)
	    
	    public void removeOnViewFinishInflateListener()
