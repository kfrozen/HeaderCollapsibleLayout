package com.troy.demo.assist;

import java.io.Serializable;

public class DemoObjCommon implements Serializable
{
    private static final long serialVersionUID = -1410586636142065546L;

    private String title;

    public DemoObjCommon(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }
}
