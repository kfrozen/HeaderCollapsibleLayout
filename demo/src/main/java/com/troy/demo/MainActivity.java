package com.troy.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.troy.collapsibleheaderlayout.HeaderCollapsibleLayout;
import com.troy.collapsibleheaderlayout.OnHeaderStatusChangedListener;
import com.troy.demo.assist.DemoObjCommon;
import com.troy.demo.assist.RecyclerViewAdapter;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnHeaderStatusChangedListener
{
    private RecyclerView mList;
    private HeaderCollapsibleLayout mHeaderCollapsibleLayout;

    private RecyclerViewAdapter mAdapter;
    private ArrayList<DemoObjCommon> mDataSet = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initDataSet();

        initComponent();
    }

    private void initDataSet()
    {
        for (int i = 0; i < 10; i++)
        {
            mDataSet.add(new DemoObjCommon("Body Item #" + i));
        }
    }

    private void initComponent()
    {
        mHeaderCollapsibleLayout = (HeaderCollapsibleLayout) findViewById(R.id.default_header_collapsible_layout_id);

        mList = (RecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new RecyclerViewAdapter(mDataSet, this);

        mList.setLayoutManager(new LinearLayoutManager(this));

        mList.setAdapter(mAdapter);

        mHeaderCollapsibleLayout.setOnHeaderStatusChangedListener(this);
    }

    @Override
    public void onHeaderStartCollapsing()
    {
        //TODO
    }

    @Override
    public void onHeaderCollapsed()
    {
        //TODO
    }

    @Override
    public void onHeaderStartExpanding()
    {
        //TODO
    }

    @Override
    public void onHeaderExpanded()
    {
        //TODO
    }

    @Override
    public void onHeaderOffsetChanged()
    {
        //TODO This event will be continuously sent while header layout is moving
    }
}
