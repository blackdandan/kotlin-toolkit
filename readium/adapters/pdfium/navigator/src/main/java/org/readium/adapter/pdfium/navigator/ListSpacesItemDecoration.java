package org.readium.adapter.pdfium.navigator;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author zhouyi
 * @Description: 列表item间距类
 * @date 2017/10/12 10:05
 */
public class ListSpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    private boolean mIncludeEdge = false;
    private int mOrientation = VERTICAL;

    public ListSpacesItemDecoration(int space) {
        this.space = space;
    }

    public ListSpacesItemDecoration(int space, int orientation) {
        this.space = space;
        this.mOrientation = orientation;
    }

    public ListSpacesItemDecoration(int space, int orientation, boolean isIncludeEdge) {
        this.space = space;
        this.mOrientation = orientation;
        this.mIncludeEdge = isIncludeEdge;

    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == HORIZONTAL) {
            outRect.right = space;
        } else {
            outRect.bottom = space;
        }
        // Add top margin only for the first item to avoid double space between items
        if (mIncludeEdge) {
            if (parent.getChildLayoutPosition(view) == 0) {
                if (mOrientation == HORIZONTAL) {
                    outRect.left = space;
                } else {
                    outRect.top = space;
                }
            }
        }

    }
}
