package com.ppworks.ppnews.widget.refresh;import android.animation.Animator;import android.animation.AnimatorListenerAdapter;import android.animation.ValueAnimator;import android.content.Context;import android.os.Build;import android.support.v4.view.ViewConfigurationCompat;import android.support.v7.widget.GridLayoutManager;import android.support.v7.widget.LinearLayoutManager;import android.support.v7.widget.RecyclerView;import android.support.v7.widget.StaggeredGridLayoutManager;import android.util.AttributeSet;import android.view.MotionEvent;import android.view.View;import android.view.ViewConfiguration;import android.view.ViewTreeObserver;import android.widget.RelativeLayout;import com.ppworks.ppnews.R;import com.socks.library.KLog;/** * ClassName: UcRefreshLayout<p> * Author:Tomoya-Hoo<p> * Fuction: 下拉刷新的布局<p> * CreateDate:2016/3/9 19:47<p> * UpdateUser:<p> * UpdateDate:<p> */public class RefreshLayout extends RelativeLayout {    // 隐藏的状态    private static final int HIDE = 0;    // 下拉刷新的状态    private static final int PULL_TO_REFRESH = 1;    // 松开刷新的状态    private static final int RELEASE_TO_REFRESH = 2;    // 正在刷新的状态    private static final int REFRESHING = 3;    // 当前状态    private int mCurrentState = HIDE;    // 头部的高度    private int mHeaderViewHeight;    // 内容View    private View mContentView;    // 内容view为recyclerview    private RecyclerView mRecyclerView;    // 内容view的第一个可视位置    private int mFirstPosition = -1;    // 可视位置数组    private int[] mVisiblePositions;    // 头部    private RefreshHead mUcRefreshHead;    // 控制头部显示移除的动画    private ValueAnimator mControlHeadAnimator;    // 移动的距离与头部的高度的比值    private float mRatio;    // 最小的移动值    private int mTouchSlop;    // 移动的Y坐标距离    private float mMoveY;    // 按下时的Y坐标，作为移动距离参考的基点    private float mDownY;    // 头部刚开始在头部刷新往下滑动的标志位    private boolean mIsLoadingPullDown;    // 头部上滑的时候，刚好隐藏过渡到列表滑动的时候，此标志用    // 来模拟action_down事件避免列表直接接受action_move事件导致瞬间调动之前累计移动的距离    private boolean mIsPostDown;    // 标志是否执行过action_down的动作,用于辨别：往上滑动的时候是在头部显示停留在顶部执行刷新动画执行时往上滑    // 还是列表滑动到位置0往下滑再往上滑的情况    private boolean mIsDownAction;    // 记录分发事件时的Y坐标，用于在执行刷新动画时往下滑到某一位置停留，等待刷新动画执行完毕自动隐藏时，更新基点    private float mRecordY;    // 记录手指按下动作的事件    private long mOnTouchTime;    // 是否模拟点击事件    private boolean mMonitorClick;    // 刷新的监听    private OnRefreshListener mListener;    // 是否响应下拉刷新    private boolean mRefreshable = true;    public RefreshLayout(Context context, AttributeSet attrs) {        super(context, attrs);        init(context);    }    private void init(Context context) {        mTouchSlop = ViewConfigurationCompat                .getScaledPagingTouchSlop(ViewConfiguration.get(getContext()));        mUcRefreshHead = new DiamondRefreshHead(context);        mUcRefreshHead.setId(R.id.refresh_head);        // 添加头部        addView(mUcRefreshHead);        measureHeadHeight();    }    /**     * 测量刷新头部的高度     */    private void measureHeadHeight() {        mUcRefreshHead.getViewTreeObserver()                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {                    @Override                    public void onGlobalLayout() {                        // 算出头部高度                        mHeaderViewHeight = mUcRefreshHead.getMeasuredHeight();                        // KLog.e("头部高度：" + -mHeaderViewHeight);                        // 隐藏刷新头部                        mUcRefreshHead.setPadding(0, -mHeaderViewHeight, 0, 0);                        // 移除监听                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {                            mUcRefreshHead.getViewTreeObserver().removeOnGlobalLayoutListener(this);                        } else {                            mUcRefreshHead.getViewTreeObserver().removeGlobalOnLayoutListener(this);                        }                    }                });    }    @Override    protected void onFinishInflate() {        super.onFinishInflate();        // 填充完成后拿到内容view        mContentView = getChildAt(1);        final LayoutParams params = (LayoutParams) mContentView.getLayoutParams();        params.addRule(RefreshLayout.BELOW, R.id.refresh_head);        if (mContentView instanceof RecyclerView) {            // 如果是recyclerview，mRecyclerView赋值            mRecyclerView = (RecyclerView) mContentView;        }    }    @Override    public boolean dispatchTouchEvent(MotionEvent ev) {        if (!mRefreshable) {            mDownY = ev.getY();            return super.dispatchTouchEvent(ev);        }        calculateRecyclerViewFirstPosition();        // 记录Y坐标        mRecordY = ev.getY();        if (mFirstPosition != 0) {            // 位置不为0时，不处理头部情况            mIsPostDown = false;            if (mControlHeadAnimator == null || !mControlHeadAnimator.isRunning()) {                // 控制头部动画在执行时，列表不能滚动                // 由于列表滚动的时候下面的ACTION_DOWN不执行，这里记录基准点                mDownY = ev.getY();                if (mUcRefreshHead.isLoading()) {                    // 头部在执行刷新的时候列表在往下滑，这时并没有在顶部显示的时候                    mIsLoadingPullDown = false;                }                // 由于列表滚动的时候下面的ACTION_DOWN不执行，这里将执行看down的标志位设为false                mIsDownAction = false;                //KLog.e("开始列表自己滚动");                // 分发事件，让列表滑动                return super.dispatchTouchEvent(ev);            }        }        if (mMonitorClick) {            // KLog.e("模拟点击事件");            if (ev.getAction() == MotionEvent.ACTION_DOWN) {                // KLog.e("模拟抬起事件");                ev.setAction(MotionEvent.ACTION_UP);            }            mMonitorClick = false;            return super.dispatchTouchEvent(ev);        }        switch (ev.getAction()) {            case MotionEvent.ACTION_DOWN:                mOnTouchTime = System.currentTimeMillis();                mDownY = ev.getY();                mMoveY = 0;                //KLog.e("ACTION_DOWN: " + mDownY);                mIsDownAction = true;                if (mUcRefreshHead.isLoading() && mUcRefreshHead                        .getPaddingTop() > -mHeaderViewHeight || (mControlHeadAnimator != null && mControlHeadAnimator                        .isRunning())) {                    // 一开始就是头部在顶部显示刷新动画的情况                    KLog.e("一开始就是头部在显示刷新动画的情况");                    mIsLoadingPullDown = true;                }                break;            case MotionEvent.ACTION_MOVE:                //KLog.e("ACTION_MOVE: " + ev.getY());                // 算出偏移量                mMoveY = ev.getY() - mDownY;                if (mControlHeadAnimator != null && mControlHeadAnimator.isRunning()) {                    // 头部动画在执行时，不处理                    return true;                }                if (mMoveY > 0) {                    // 往下滑的情况                    if (mUcRefreshHead.isReadyLoad() && mMoveY * 0.35f > mHeaderViewHeight) {                        // 头部准备好刷新了，状态更新为释放刷新                        mCurrentState = RELEASE_TO_REFRESH;                    } else {                        // 头部还没准备好刷新，状态更新为下拉刷新                        mCurrentState = PULL_TO_REFRESH;                    }                    if (mIsLoadingPullDown) {                        // 一开始就是头部在顶部显示刷新动画的情况，此时参考的paddingTop为0                        // KLog.e("加载动画运行下拉，此时paddingTop为0");                        mUcRefreshHead.setPadding(0, (int) (0 + mMoveY * 0.35f), 0, 0);                    } else {                        // KLog.e("下拉，屏蔽事件自己处理");                        // 参考的paddingTop为-mHeaderViewHeight                        mUcRefreshHead                                .setPadding(0, (int) (-mHeaderViewHeight + mMoveY * 0.35f), 0, 0);                        // 记录下拉距离与头部高度的比值                        mRatio = mMoveY * 1.0f / 2.5f / mHeaderViewHeight;                        // 头部执行下拉效果                        if (!mUcRefreshHead.isLoading()) mUcRefreshHead.performPull(mRatio);                    }                    // 事件自己处理掉了                    return true;                } else if (mMoveY < 0) {                    // 往上滑的情况                    // KLog.e(mIsLoadingPullDown + ";" + mIsDownAction + ";" + mUcRerfreshHead.getPaddingTop() + ";" + mUcRerfreshHead.isLoading());                    if (mIsLoadingPullDown && mIsDownAction && mUcRefreshHead                            .getPaddingTop() > -mHeaderViewHeight && mUcRefreshHead.isLoading()) {                        // 一开始就是头部在顶部显示刷新动画的情况，此时参考的paddingTop为0                        // KLog.e("已经是刷新状态往上滑动: " + mUcRerfreshHead.getPaddingTop());                        mUcRefreshHead.setPadding(0, (int) (0 + mMoveY * 0.35f), 0, 0);                        // 头部状态更新为下拉刷新                        mCurrentState = PULL_TO_REFRESH;                        // 事件自己处理掉了                        return true;                    } else {                        // 其他情况肯定是头部上滑到隐藏了，头部状态更新为隐藏状态                        mCurrentState = HIDE;                    }                    // 上滑到头部隐藏了，此时事件交给列表自己处理了                    if (!mIsPostDown) {                        ev.setAction(MotionEvent.ACTION_DOWN);                        mIsPostDown = true;                        // KLog.e("ev.setAction(MotionEvent.ACTION_DOWN)");                    } else {                        ev.setAction(MotionEvent.ACTION_MOVE);                        // KLog.e("ev.setAction(MotionEvent.ACTION_MOVE)");                    }                    if (mUcRefreshHead.getPaddingTop() > -mHeaderViewHeight) {                        // 保证最后一定完全隐藏头部                        mUcRefreshHead.setPadding(0, -mHeaderViewHeight, 0, 0);                    }                    // KLog.e("列表滚动");                    return super.dispatchTouchEvent(ev);                }                break;            case MotionEvent.ACTION_UP:                // 手指抬起的时候                mIsPostDown = false;                mIsLoadingPullDown = false;                mIsDownAction = false;                // KLog.e(System.currentTimeMillis() - mOnTouchTime + ";" + mMoveY + ";" + mTouchSlop);                if (System.currentTimeMillis() - mOnTouchTime <= 1000 && Math                        .abs(mMoveY) < mTouchSlop) {                    // KLog.e("响应点击事件");                    mMoveY = 0;                    mMonitorClick = true;                    ev.setAction(MotionEvent.ACTION_DOWN);                    dispatchTouchEvent(ev);                    return true;                }                mMoveY = 0;                // KLog.e("手指抬起");                if (mControlHeadAnimator != null && mControlHeadAnimator                        .isRunning() || mCurrentState == HIDE || mUcRefreshHead                        .getPaddingTop() == -mHeaderViewHeight) {                    // 控制头部动画在执行，头部已经隐藏时，此时分发事件                    // KLog.e("手指抬起不处理");                    return super.onTouchEvent(ev);                }                // 开启控制头部的动画                mControlHeadAnimator = new ValueAnimator();                if (mCurrentState == RELEASE_TO_REFRESH) {                    // 状态为释放刷新的情况，做显示头部的动画                    mControlHeadAnimator.setIntValues(mUcRefreshHead.getPaddingTop(), 0);                } else if (mCurrentState == PULL_TO_REFRESH) {                    // 状态为下拉刷新的情况，做隐藏头部的动画                    mControlHeadAnimator                            .setIntValues(mUcRefreshHead.getPaddingTop(), -mHeaderViewHeight);                } else {                    // 其它状态做隐藏头部的动画                    mControlHeadAnimator                            .setIntValues(mUcRefreshHead.getPaddingTop(), -mHeaderViewHeight);                }                mControlHeadAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {                    @Override                    public void onAnimationUpdate(ValueAnimator animation) {                        mUcRefreshHead.setPadding(0, (Integer) animation.getAnimatedValue(), 0, 0);                        if (mCurrentState == PULL_TO_REFRESH) {                            // 移除的时候，头部升降动画                            mUcRefreshHead                                    .performPull(mRatio * (1 - animation.getAnimatedFraction()));                        }                    }                });                mControlHeadAnimator.addListener(new AnimatorListenerAdapter() {                    @Override                    public void onAnimationEnd(Animator animation) {                        super.onAnimationEnd(animation);                        if (mCurrentState == RELEASE_TO_REFRESH && mUcRefreshHead.isReadyLoad()) {                            // 释放刷新，头部开启刷新动画                            mUcRefreshHead.performLoading();                            mCurrentState = REFRESHING;                            if (mListener != null) {                                mListener.onRefreshing();                            }                        }                    }                });                mControlHeadAnimator.setDuration(250);                mControlHeadAnimator.start();                return true;        }        return super.dispatchTouchEvent(ev);    }    /**     * 计算RecyclerView当前第一个完全可视位置     */    private void calculateRecyclerViewFirstPosition() {        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {            // 判断LayoutManager类型获取第一个完全可视位置            if (mRecyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {                if (mVisiblePositions == null) {                    mVisiblePositions = new int[((StaggeredGridLayoutManager) mRecyclerView                            .getLayoutManager()).getSpanCount()];                }                ((StaggeredGridLayoutManager) mRecyclerView.getLayoutManager())                        .findFirstCompletelyVisibleItemPositions(mVisiblePositions);                mFirstPosition = mVisiblePositions[0];            } else if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {                mFirstPosition = ((GridLayoutManager) mRecyclerView.getLayoutManager())                        .findFirstCompletelyVisibleItemPosition();            } else {                mFirstPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())                        .findFirstCompletelyVisibleItemPosition();            }        }    }    @Override    protected void onDetachedFromWindow() {        super.onDetachedFromWindow();        if (mControlHeadAnimator != null && mControlHeadAnimator.isRunning()) {            mControlHeadAnimator.removeAllUpdateListeners();            mControlHeadAnimator.removeAllListeners();            mControlHeadAnimator.cancel();            mControlHeadAnimator = null;        }    }    /**     * 设置监听更新的接口     *     * @param listener     */    public void setRefreshListener(OnRefreshListener listener) {        mListener = listener;    }    public void setRefreshable(boolean enable) {        if (mRefreshable != enable) {            mRefreshable = enable;        }    }    /**     * 更新时监听的接口     */    public interface OnRefreshListener {        void onRefreshing();    }    /**     * 刷新完毕调用，清除头部动画，隐藏头部     */    public void refreshFinish() {        mUcRefreshHead.post(new Runnable() {            @Override            public void run() {                mUcRefreshHead.performLoaded();                if (mUcRefreshHead.getPaddingTop() <= -mHeaderViewHeight) {                    return;                }                mControlHeadAnimator = new ValueAnimator();                mControlHeadAnimator                        .setIntValues(mUcRefreshHead.getPaddingTop(), -mHeaderViewHeight);                mControlHeadAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {                    @Override                    public void onAnimationUpdate(ValueAnimator animation) {                        mUcRefreshHead.setPadding(0, (Integer) animation.getAnimatedValue(), 0, 0);                    }                });                mControlHeadAnimator.addListener(new AnimatorListenerAdapter() {                    @Override                    public void onAnimationEnd(Animator animation) {                        super.onAnimationEnd(animation);                        mIsLoadingPullDown = false;                        mDownY = mRecordY;                    }                });                mControlHeadAnimator.setDuration(250);                mControlHeadAnimator.start();            }        });    }}