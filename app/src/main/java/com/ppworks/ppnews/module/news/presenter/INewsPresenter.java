package com.ppworks.ppnews.module.news.presenter;

import com.ppworks.ppnews.base.BasePresenter;

/**
 * ClassName: INewsPresenter<p>
 * Author: Tomoya-Hoo<p>
 * Fuction: 新闻代理接口<p>
 * CreateDate: 2016/4/17 21:04<p>
 * UpdateUser: <p>
 * UpdateDate: <p>
 */
public interface INewsPresenter extends BasePresenter {

    /**
     * 频道排序或增删变化后调用此方法更新数据库
     */
    void operateChannelDb();

}
