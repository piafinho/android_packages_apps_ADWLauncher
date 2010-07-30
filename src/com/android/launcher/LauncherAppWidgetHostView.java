/*
*    Copyright 2010 AnderWeb (Gustavo Claramunt) <anderweb@gmail.com>
*
*    This file is part of ADW.Launcher.
*
*    ADW.Launcher is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    ADW.Launcher is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with ADW.Launcher.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.android.launcher;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

/**
 * {@inheritDoc}
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView {

    private static final long WIDGET_LONG_CLICK_TIMEOUT = 700;

    private static final String TAG = "LauncherAppWidgetHostView";

    private boolean mHasPerformedLongPress;

    private CheckForLongPress mPendingCheckForLongPress;

    private LayoutInflater mInflater;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        try {
            super.dispatchRestoreInstanceState(container);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float startY;

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            startY = ev.getY();
            postCheckForLongClick();
            break;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(ev.getY() - startY) < 5)
                return false;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            mHasPerformedLongPress = false;
            if (mPendingCheckForLongPress != null) {
                removeCallbacks(mPendingCheckForLongPress);
            }
            break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, WIDGET_LONG_CLICK_TIMEOUT);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }

}
