package com.troy.demo.assist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.troy.demo.R;

import java.util.ArrayList;


public class RecyclerViewAdapter extends RecyclerView.Adapter<CommonViewHolder>
{
    private ArrayList<DemoObjCommon> mDataSet;
    private Context mContext;
    private OnClickListener mOnItemClickListener;

    public RecyclerViewAdapter(ArrayList<DemoObjCommon> dataSet, Context context)
    {
        mDataSet = dataSet;

        mContext = context;
    }

    public void appendDataSet(ArrayList<DemoObjCommon> dataSet)
    {
        if(mDataSet == null) mDataSet = new ArrayList<>();

        mDataSet.addAll(dataSet);
    }

    public void setOnItemClickListener(OnClickListener listener)
    {
        mOnItemClickListener = listener;
    }

    @Override
    public CommonViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        return new CommonViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_recycler_view, parent, false));
    }

    @Override
    public void onBindViewHolder(CommonViewHolder holder, int position)
    {
        if(holder == null) return;

        holder.setData(mDataSet.get(position));
    }

    public String getItemTitle(int position)
    {
        return mDataSet.get(position).getTitle();
    }

    @Override
    public int getItemCount()
    {
        return mDataSet.size();
    }
}
