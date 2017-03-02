package com.troy.demo.assist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;


public class CommonViewHolder extends RecyclerView.ViewHolder
{
    private TextView mTitle;

    public CommonViewHolder(View itemView)
    {
        super(itemView);

        mTitle = (TextView) itemView;
    }

    public void setData(DemoObjCommon obj)
    {
        if(obj == null) return;

        mTitle.setText(obj.getTitle());
    }
}
