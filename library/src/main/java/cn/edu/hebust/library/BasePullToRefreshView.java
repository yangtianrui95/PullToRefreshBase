package cn.edu.hebust.library;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by shixi_tianrui1 on 16-12-17.
 * 对任何View扩展实现下拉刷新功能
 */

public abstract class BasePullToRefreshView<T extends View> extends ViewGroup {

    private static final String TAG = "BasePullToRefreshView";

    // 内容布局
    protected T mContentView;
    protected View mHeaderView;
    protected View mFooterView;

    protected ImageView mIvArrow;
    protected TextView mTvRefreshText;
    protected TextView mTvRefreshTime;
    protected ProgressBar mProgressBar;

    protected Context mContext;

    private Scroller mScroller;
    private int mInitScrollY;
    private int mLastY;
    private int mYOffset;
    private int mRefreshSlop = mInitScrollY / 2; // 上拉过程中,开始刷新的阈值

    private onRefreshListener mListener;
    private ViewStatus mCurrentStatus = ViewStatus.IDLE;
    private LayoutInflater mInflater;

    /**
     * 当前View的状态
     */
    private enum ViewStatus {
        REFRESHING("refreshing"), // 刷新中
        IDLE("idle"),       // 闲置
        PULLING("pulling"),    // 拉动中
        LOADING("loading"),;    // 加载中

        private String statusDesc;

        ViewStatus(String status) {
            statusDesc = status;
        }

        @Override
        public String toString() {
            return statusDesc;
        }
    }

    public interface onRefreshListener {
        void onRefresh();
    }

    public BasePullToRefreshView(Context context) {
        this(context, null);
    }

    public BasePullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasePullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mScroller = new Scroller(mContext);
        initLayoutView();
    }


    /**
     * 初始化HeaderView和ContentView
     */
    protected void initLayoutView() {
        initHeaderView();
        initContentView();
        LayoutParams contentParams = getContentViewLayoutParams();
        addView(mContentView, contentParams);
        initFooterView();
    }

    abstract protected void initContentView();

    @Nullable
    abstract protected LayoutParams getContentViewLayoutParams();

    protected void initHeaderView() {
        mHeaderView = mInflater.inflate(R.layout.pull_to_refresh_header, this);
        mIvArrow = (ImageView) mHeaderView.findViewById(R.id.id_iv_arrow);
        mTvRefreshText = (TextView) mHeaderView.findViewById(R.id.id_tv_refresh_text);
        mTvRefreshTime = (TextView) mHeaderView.findViewById(R.id.id_tv_refresh_time);
        mProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.id_pb_loading);
        addView(mHeaderView);
    }


    protected void initFooterView() {
        mFooterView = mInflater.inflate(R.layout.pull_to_refresh_footer, this);
        addView(mFooterView);
    }


    /**
     * 此处需测量Header和ContentView的高度之和
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            totalHeight += child.getMeasuredHeight();
        }
        setMeasuredDimension(widthSize, totalHeight);
    }


    /**
     * 将HeaderView和ContentView从上到下布局
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(left, top, child.getMeasuredWidth(), child.getMeasuredHeight());
            top += child.getMeasuredHeight();
        }
        // 将Header隐藏
        mInitScrollY = mHeaderView.getMeasuredHeight() + getPaddingTop();
        mRefreshSlop = mInitScrollY / 2;
        scrollTo(0, mInitScrollY);
    }

    /**
     * 是否滑动到顶端
     */
    abstract protected boolean isTop();

    abstract protected boolean isBottom();


    public void setOnRefreshingListener(onRefreshListener listener) {
        mListener = listener;
    }

    public T getContentView() {
        return mContentView;
    }


    /**
     * ContentView 已经滑动到顶部，并且正在下拉时，拦截此事件，否则由ContentView处理
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent: " + ev);
        final int action = ev.getAction();
        switch (action) {
            // 手指抬起时,不再拦截事件
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                return false;

            case MotionEvent.ACTION_DOWN:
                mLastY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                mYOffset = (int) (ev.getRawY() - mLastY);
                // 到顶部,并且继续下拉
                if (mYOffset > 0 && isTop()) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent: " + ev);
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 处理HeaderView拖动逻辑
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: " + event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int curY = (int) event.getRawY();
                mYOffset = curY - mLastY;
                if (mCurrentStatus != ViewStatus.LOADING) {
                    changeScrollY(mYOffset);
                }
                rotateHeaderArrow();
                changeTips();
                break;
            case MotionEvent.ACTION_UP:
                doRefresh();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 执行刷新操作
     */
    private void doRefresh() {
        changeHeadViewStatus();
        if (mCurrentStatus == ViewStatus.REFRESHING && mListener != null) {
            mListener.onRefresh();
        }
    }

    private void changeHeadViewStatus() {
        if (getScrollY() > mRefreshSlop) { // 开始刷新
            mScroller.startScroll(getScrollX(), getScrollY(), 0, -getScrollY());
            mCurrentStatus = ViewStatus.REFRESHING;
            //  todo 显示Loading状态, 图片,进度条等

        } else { // 恢复原位
            mScroller.startScroll(getScrollX(), getScrollY(), 0, mInitScrollY - getScrollY());
            mCurrentStatus = ViewStatus.IDLE;
        }
        invalidate();
    }


    private void changeTips() {
        Log.d(TAG, "changeTips: ");
    }

    /**
     * 旋转刷新箭头
     */
    private void rotateHeaderArrow() {
        Log.d(TAG, "rotateHeaderArrow: ");
    }


    /**
     * 下拉操作
     */
    private void changeScrollY(int distance) {
        // 最大值为ScrollY(HeaderView隐藏), 最小值为0(HeaderView显示)
        int curY = getScrollY();
        // 下拉
        if (distance > 0 && curY - distance > getPaddingTop()) {
            scrollBy(0, -distance);
        } else if (distance < 0 && curY - distance <= mInitScrollY) { // 上拉
            scrollBy(0, -distance);
        }
        curY = getScrollY();
        Log.d(TAG, "changeScrollY: curY=" + curY);
        // 不刷新
        if (curY > 0 && curY < mRefreshSlop) {
            mCurrentStatus = ViewStatus.PULLING;
        } else if (curY > 0 && curY > mRefreshSlop) { // 开始刷新
            mCurrentStatus = ViewStatus.REFRESHING;
        }

    }
}
