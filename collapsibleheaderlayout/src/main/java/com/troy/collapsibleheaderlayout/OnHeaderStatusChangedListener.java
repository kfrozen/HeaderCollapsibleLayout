package com.troy.collapsibleheaderlayout;

public interface OnHeaderStatusChangedListener
{
    void onHeaderStartCollapsing();

    void onHeaderCollapsed();

    void onHeaderStartExpanding();

    void onHeaderExpanded();

    //This event will be continuously sent while header layout is moving
    void onHeaderOffsetChanged();
}
