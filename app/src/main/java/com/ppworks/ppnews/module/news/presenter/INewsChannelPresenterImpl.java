package com.ppworks.ppnews.module.news.presenter;

import com.ppworks.ppnews.base.BasePresenterImpl;
import com.ppworks.ppnews.greendao.NewsChannelTable;
import com.ppworks.ppnews.module.news.model.INewsChannelInteractor;
import com.ppworks.ppnews.module.news.model.INewsChannelInteractorImpl;
import com.ppworks.ppnews.module.news.view.INewsChannelView;
import com.ppworks.ppnews.utils.RxBus;
import java.util.List;
import java.util.Map;

/**
 * ClassName:INewsChannelPresenterImpl <p>
 * Author: Tomoya-Hoo<p>
 * Fuction: 新闻频道管理代理接口实现<p>
 * CreateDate: 2016/4/20 14:02<p>
 * UpdateUser: <p>
 * UpdateDate: <p>
 */
public class INewsChannelPresenterImpl
        extends BasePresenterImpl<INewsChannelView, Map<Boolean, List<NewsChannelTable>>>
        implements INewsChannelPresenter {

    private INewsChannelInteractor<Map<Boolean, List<NewsChannelTable>>>
            mNewsChannelInteractor;

    private boolean mChannelChange;

    public INewsChannelPresenterImpl(INewsChannelView newsChannelView) {
        super(newsChannelView);
        mNewsChannelInteractor = new INewsChannelInteractorImpl();
        // 初始化
        mSubscription = mNewsChannelInteractor.channelDbOperate(this, "", null);
    }

    @Override
    public void onDestroy() {
        RxBus.get().post("channelChange", mChannelChange);
        super.onDestroy();
    }

    @Override
    public void onItemAddOrRemove(String channelName, boolean selectState) {
        mChannelChange = true;
        // 增删操作
        mSubscription = mNewsChannelInteractor.channelDbOperate(this, channelName, selectState);
    }

    @Override
    public void onItemSwap(int fromPos, int toPos) {
        mChannelChange = true;
        mSubscription = mNewsChannelInteractor.channelDbSwap(this, fromPos, toPos);
    }

    @Override
    public void requestSuccess(Map<Boolean, List<NewsChannelTable>> data) {
        // 只有初始化才调用到
        mView.initTwoRecyclerView(data.get(true), data.get(false));
    }

}
