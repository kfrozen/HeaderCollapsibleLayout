package com.troy.collapsibleheaderlayout;

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
