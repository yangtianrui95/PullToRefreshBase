package cn.edu.hebust.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
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
    protected ImageView mIvArrow;
    protected TextView mTvRefreshText;
    protected TextView mTvRefreshTime;
    protected ProgressBar mProgressBar;

    protected Context mContext;

    private Scroller mScroller;
    private int mInitScrollY;
    private int mLastY;

    private onRefreshListener mListener;
    private ViewStatus mCurrentStatus = ViewStatus.Idle;

    private enum ViewStatus {
        Refreshing, // 刷新中
        Idle,       // 闲置
        Pulling,    // 拉动中
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
        initLayoutView();
    }


    /**
     * 初始化HeaderView和ContentView
     */
    protected void initLayoutView() {
        initHeaderView();
        initContentView();
    }

    protected abstract void initContentView();

    protected void initHeaderView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mHeaderView = inflater.inflate(R.layout.pull_to_refresh_header, this);
        mIvArrow = (ImageView) mHeaderView.findViewById(R.id.id_iv_arrow);
        mTvRefreshText = (TextView) mHeaderView.findViewById(R.id.id_tv_refresh_text);
        mTvRefreshTime = (TextView) mHeaderView.findViewById(R.id.id_tv_refresh_time);
        mProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.id_pb_loading);

        //fixme need addView
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
        scrollTo(0, mInitScrollY);
    }

    /**
     * 是否滑动到顶端
     */
    abstract protected boolean isTop();


    public void setOnRefreshingListener(onRefreshListener listener) {
        mListener = listener;
    }

    public T getContentView() {
        return mContentView;
    }
}
