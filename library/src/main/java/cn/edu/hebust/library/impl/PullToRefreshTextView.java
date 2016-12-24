package cn.edu.hebust.library.impl;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import cn.edu.hebust.library.BasePullToRefreshView;

/**
 * Created by shixi_tianrui1 on 16-12-24.
 */

public class PullToRefreshTextView extends BasePullToRefreshView<TextView> {
    public PullToRefreshTextView(Context context) {
        super(context);
    }

    public PullToRefreshTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initContentView() {

    }

    @Nullable
    @Override
    protected LayoutParams getContentViewLayoutParams() {
        return null;
    }

    @Override
    protected boolean isTop() {
        return false;
    }

    @Override
    protected boolean isBottom() {
        return false;
    }
}
