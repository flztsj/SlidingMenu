package com.jsheng.slidingmenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by shengjun on 2016/12/24.
 */

public class SlidingMenu extends HorizontalScrollView {

    private final static String TAG = SlidingMenu.class.getSimpleName();

    // 响应 SlidingMenu 拖动的触摸范围默认值
    private final static int DEFAULT_SLIDING_WIDTH = 50;
    // Menu 和 Content 之间的间隔
    private final static int DEFAULT_SLIDING_PADDING = 160;

    // mContentView 的最小缩放比例
    private final static float LEAST_CONTENT_SCALE = 0.9f;

    // 响应 SlidingMenu 拖动的触摸范围
    private int mSlidingCrack;
    // mMenuWidth = mScreenWidth - mSlidingPadding
    private int mSlidingPadding;

    private List<SlidingListener> mListeners;
    private int mLayoutDirection;

    private ViewGroup mMenuView;
    private ViewGroup mContentView;

    private int mMenuWidth;
    private int mHalfMenuWidth;
    private int mOpenPosition;
    private int mClosePosition;
    private boolean isMenuOpen;

    private int mScreenWidth;
    private boolean isLayoutComplete;

    private float mLastMotionX;

    private boolean mTouching = false;
    private boolean mTouchMoving = false;

    /**
     * 不要在回调的接口函数中处理耗时任务
     */
    public interface SlidingListener {

        void onMenuSlide(View menuView, float slideOffset);

        void onMenuOpened(View menuView);

        void onMenuClosed(View menuView);
    }

    public void setSlidingListener(SlidingListener listener) {
        if (mListeners != null) {
            mListeners.add(listener);
        }
    }

    public void setSlidingViews(ViewGroup menu, ViewGroup content) {
        mMenuView = menu;
        mContentView = content;
    }

    public void setSlidingWidth(int slidingWidth) {
        mSlidingCrack = Math.min(slidingWidth, mSlidingCrack);
    }

    public void setSlidingPadding(int slidingPadding) {
        mSlidingPadding = slidingPadding;
    }

    public SlidingMenu(Context context) {
        this(context, null);
    }

    public SlidingMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSlidingMenu();


        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenu);

        mSlidingCrack = a.getDimensionPixelSize(R.styleable.SlidingMenu_slidingCrack, DEFAULT_SLIDING_WIDTH);
        mSlidingPadding = a.getDimensionPixelSize(R.styleable.SlidingMenu_slidingPadding, DEFAULT_SLIDING_PADDING);

        a.recycle();
    }

    private void initSlidingMenu() {
        mScreenWidth = Utils.getScreenWidth(this);
        mOpenPosition = -1;
        mClosePosition = -1;
        mLayoutDirection = -1;

        mListeners = new ArrayList<>();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private int getOpenPosition() {
        if (mOpenPosition == -1) {
            mOpenPosition = isLtrDirection() ? 0 : mMenuWidth;
        }
        return mOpenPosition;
    }

    private int getClosePosition() {
        if (mClosePosition == -1) {
            mClosePosition = isLtrDirection() ? mMenuWidth : 0;
        }
        return mClosePosition;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!isLayoutComplete) {
            if (mMenuView == null || mContentView == null) {
                ViewGroup wrapper = (ViewGroup) getChildAt(0);
                mMenuView = (ViewGroup) wrapper.getChildAt(0);
                mContentView = (ViewGroup) wrapper.getChildAt(1);
            }
            mMenuWidth = mScreenWidth - mSlidingPadding;
            mHalfMenuWidth = mMenuWidth / 2;
            mMenuView.getLayoutParams().width = mMenuWidth;
            mContentView.getLayoutParams().width = mScreenWidth;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // 默认状态下，Menu 是关闭的
            setScrollX(getClosePosition());
            isLayoutComplete = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent \n");
        Log.d(TAG, "ev : " + ev.toString());
        boolean currentIntercept = true;

        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            final int evX = (int) ev.getX();
            if (isMenuOpen) {
                if (canIgnoreInMenuOpen(evX)) {
                    requestDisallowInterceptTouchEvent(true);
                    currentIntercept = false;
                }
            } else {
                if (canIgnoreInMenuClose(evX)) {
                    requestDisallowInterceptTouchEvent(true);
                    currentIntercept = false;
                }
            }
        }

        return super.onInterceptTouchEvent(ev) || currentIntercept;
    }

    private boolean canCloseInMenuOpen(int evX) {
        return isLtrDirection() ? (evX > mScreenWidth - mSlidingPadding) : (evX < mSlidingPadding);
    }

    private boolean canIgnoreInMenuOpen(int evX) {
        return isLtrDirection() ? (evX < mScreenWidth - mSlidingPadding) : (evX > mSlidingPadding);
    }

    private boolean canIgnoreInMenuClose(int evX) {
        return isLtrDirection() ? (evX > mSlidingCrack) : (evX < mScreenWidth - mSlidingCrack);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onTouchEvent \n");
        Log.d(TAG, "ev : " + ev.toString());

        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTouchMoving = false;
                mTouching = true;
                mLastMotionX =  ev.getX();
                break;
            }
            case MotionEvent.ACTION_MOVE : {
                float evX = ev.getX();
                if (evX != mLastMotionX) {
                    mTouchMoving = true;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mTouching = false;
                float evX = ev.getX();
                if (mTouchMoving && isCanCloseMenu()) {
                    closeMenu();
                    return true;
                }
                if (mTouchMoving && isCanOpenMenu()) {
                    openMenu();
                    return true;
                }
                if (!mTouchMoving && isSingleTapCanCloseMenu((int) evX)) {
                    closeMenu();
                    return true;
                }
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    private boolean isCanCloseMenu() {
        return isLtrDirection() ? getScrollX() > mHalfMenuWidth : getScrollX() < mHalfMenuWidth;
    }


    private boolean isCanOpenMenu() {
        return isLtrDirection() ? getScrollX() < mHalfMenuWidth : getScrollX() > mHalfMenuWidth;
    }

    private boolean isSingleTapCanCloseMenu(int evX) {
        if (isMenuOpen) {
            return isLtrDirection() ? evX > mScreenWidth - mSlidingPadding : evX < mSlidingPadding;
        } else {
            return false;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        handleScrollChanged(l);
    }

    private void handleScrollChanged(int l) {
        float scale;
        if (isLtrDirection()) {
            // 默认关闭的情况下，l 的大小就是menuWidth，所以从关到开滚动时 l 从大到小
            scale = l * 1.0f / mMenuWidth;
        } else {
            // 默认关闭的情况下，l 的大小就是0，所以从关到开滚动时 l 从小到大
            scale = (mMenuWidth - l) * 1.0f / mMenuWidth;
        }
        float contentScale = LEAST_CONTENT_SCALE + scale * (1 - LEAST_CONTENT_SCALE);

        if (!mTouching && contentScale == 1.0f) {
            isMenuOpen = false;
        } else if (!mTouching && contentScale == LEAST_CONTENT_SCALE) {
            isMenuOpen = true;
        }

        changContentView(contentScale, isLtrDirection());

        for (SlidingListener listener : mListeners) {
            listener.onMenuSlide(mMenuView, scale);
        }
    }

    private void changContentView(float scale, boolean isLtr) {
//        Log.d(TAG, "changContentView(" + scale + ", " + isLtr + ")");
        if (mContentView == null) {
            throw new IllegalArgumentException("mContentView is null");
        }
        // mContentView 的大小是 match_parent，为了在缩放时贴合 mMenu，需要将
        // PivotX 设置为和 mMenu相交的那条边的坐标
        mContentView.setPivotX(isLtr ? 0 : mContentView.getWidth());
        mContentView.setPivotY(mContentView.getHeight() / 2);
        mContentView.setScaleX(scale);
        mContentView.setScaleY(scale);
    }

    public void openMenu() {
        smoothScrollTo(getOpenPosition(), 0);
        if (!isMenuOpen) {
            isMenuOpen = true;
            for (SlidingListener listener : mListeners) {
                listener.onMenuOpened(mMenuView);
            }
        }
    }

    public void closeMenu() {
        smoothScrollTo(getClosePosition(), 0);
        if (isMenuOpen) {
            isMenuOpen = false;
            for (SlidingListener listener : mListeners) {
                listener.onMenuClosed(mMenuView);
            }
        }
    }

    /**
     * 是否是正常的从左到右的布局方向
     *
     * @return true, 是LTR；false，是RTL。
     */
    private boolean isLtrDirection() {
        if (mLayoutDirection == -1) {
            mLayoutDirection = Utils.getLayoutDirection(this);
        }
        return mLayoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isLayoutComplete = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isLayoutComplete = false;
    }

    private boolean inChild(int x, int y) {
        return inView(x, y, mMenuView) && inView(x, y, mContentView);
    }

    private boolean inView(int x, int y, View view) {
        return !(y < view.getTop()
                || y >= view.getBottom()
                || x < view.getLeft() - getScrollX()
                || x >= view.getRight() - getScrollX());
    }
}