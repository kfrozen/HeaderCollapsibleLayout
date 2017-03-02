# HeaderCollapsibleLayout
A wrapper layout that can easily split your current layout into header and body, and provides smooth header collapsing action with related event callbacks.

----------

**Basic steps to wrap your original layout to obtain a collapsible header layout:**



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
            app:overlayFooterLayoutId="@+id/demo_bottom_overlay"/>
            <!--app:supportAutoExpand="false" />-->

3. You are all set! Ready to soar ---

**Note that:**

1. If there was NOT a view in your body layout that implemented the *NestedScrollingChild*, please wrap your body layout with a *NestedScrollView*.
2. Setting **app:supportAutoExpand="false"** means the header will not automatically expand when user perform a fling action, instead of which the header will move with user's finger until totally expanded.
3. We also provide a set of event callbacks:

		public interface OnHeaderStatusChangedListener
		{
		    void onHeaderStartCollapsing();
		
		    void onHeaderCollapsed();
		
		    void onHeaderStartExpanding();
		
		    void onHeaderExpanded();
		
		    //This event will be continuously sent while header layout is moving
		    void onHeaderOffsetChanged();
		}

	Set this to your HeaderCollapsibleLayout instance by calling the below method in case you wanna do something response to the collapsing events.

		public void setOnHeaderStatusChangedListener(OnHeaderStatusChangedListener callback)
