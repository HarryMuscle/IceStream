/*
 * HorizontalListView.java v1.5
 *
 *
 * The MIT License
 * Copyright (c) 2011 Paul Soucy (paul@dev-smart.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

// The following code has been changed by Stonyx
// Copyright (C) 2011 Stonyx

package info.icefilms.icestream.browse;

// The preceding code has been changed by Stonyx

// The following code has been added by Stonyx
// Copyright (C) 2011 Stonyx

import android.view.SoundEffectConstants;

// The preceding code has been added by Stonyx

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Scroller;
import java.util.LinkedList;
import java.util.Queue;

public class HorizontalListView extends AdapterView<ListAdapter>
{
  public boolean mAlwaysOverrideTouch = true;
  protected ListAdapter mAdapter;
  private int mLeftViewIndex = -1;
  private int mRightViewIndex = 0;
  protected int mCurrentX;
  protected int mNextX;
  private int mMaxX = Integer.MAX_VALUE;
  private int mDisplayOffset = 0;
  protected Scroller mScroller;
  private GestureDetector mGesture;
  private Queue<View> mRemovedViewQueue = new LinkedList<View>();
  private OnItemSelectedListener mOnItemSelected;
  private OnItemClickListener mOnItemClicked;
  private OnItemLongClickListener mOnItemLongClicked;
  private boolean mDataChanged = false;

  public HorizontalListView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initView();
  }

  private synchronized void initView()
  {
    mLeftViewIndex = -1;
    mRightViewIndex = 0;
    mDisplayOffset = 0;
    mCurrentX = 0;
    mNextX = 0;
    mMaxX = Integer.MAX_VALUE;
    mScroller = new Scroller(getContext());
    mGesture = new GestureDetector(getContext(), mOnGesture);
  }

  @Override
  public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener)
  {
    mOnItemSelected = listener;
  }

  @Override
  public void setOnItemClickListener(AdapterView.OnItemClickListener listener)
  {
    mOnItemClicked = listener;
  }

  @Override
  public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener)
  {
    mOnItemLongClicked = listener;
  }

  private DataSetObserver mDataObserver = new DataSetObserver()
  {
    @Override
    public void onChanged()
    {
      synchronized (HorizontalListView.this)
      {
        mDataChanged = true;
      }
      invalidate();
      requestLayout();
    }

    @Override
    public void onInvalidated()
    {
      reset();
      invalidate();
      requestLayout();
    }
  };

  @Override
  public ListAdapter getAdapter()
  {
    return mAdapter;
  }

  @Override
  public View getSelectedView()
  {
    // TODO: implement
    return null;
  }

  @Override
  public void setAdapter(ListAdapter adapter)
  {
    if (mAdapter != null)
    {
      mAdapter.unregisterDataSetObserver(mDataObserver);
    }
    mAdapter = adapter;
    mAdapter.registerDataSetObserver(mDataObserver);
    reset();
  }

  private synchronized void reset()
  {
    initView();
    removeAllViewsInLayout();
    requestLayout();
  }

  @Override
  public void setSelection(int position)
  {
    // TODO: implement
  }

  private void addAndMeasureChild(final View child, int viewPos)
  {
    LayoutParams params = child.getLayoutParams();
    if (params == null)
    {
      // The following code has had the following changed made by Stonyx:
      // - "LayoutParams.FILL_PARENT" replaced by "LayoutParams.MATCH_PARENT"
      // Copyright (C) 2011 Stonyx

      params = new LayoutParams(LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT);
      
      // The preceding code has been changed by Stonyx
    }

    // The following code has been added by Stonyx
    // Copyright (C) 2011 Stonyx

    // Save the padding information
    int[] padding = { child.getPaddingLeft(), child.getPaddingTop(),
                    child.getPaddingRight(), child.getPaddingBottom() };
    
    // Set the background to match the default list selector background
    child.setBackgroundDrawable(getResources().
        getDrawable(android.R.drawable.list_selector_background));
    
    // Restore the padding since it gets reset when setting a new background
    child.setPadding(padding[0], padding[1], padding[2], padding[3]);
    
    // The preceding code has been added by Stonyx
    
    addViewInLayout(child, viewPos, params, true);
    child.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
  }

  @Override
  protected synchronized void onLayout(boolean changed, int left, int top,
                                       int right, int bottom)
  {
    super.onLayout(changed, left, top, right, bottom);

    if (mAdapter == null)
    {
      return;
    }

    if (mDataChanged)
    {
      int oldCurrentX = mCurrentX;
      initView();
      removeAllViewsInLayout();
      mNextX = oldCurrentX;
      mDataChanged = false;
    }

    if (mScroller.computeScrollOffset())
    {
      int scrollx = mScroller.getCurrX();
      mNextX = scrollx;
    }

    if (mNextX <= 0)
    {
      mNextX = 0;
      mScroller.forceFinished(true);
    }
    if(mNextX >= mMaxX)
    {
      mNextX = mMaxX;
      mScroller.forceFinished(true);
    }

    int dx = mCurrentX - mNextX;

    removeNonVisibleItems(dx);
    fillList(dx);
    positionItems(dx);

    mCurrentX = mNextX;

    if (!mScroller.isFinished())
    {
      post(new Runnable()
      {
        public void run()
        {
          requestLayout();
        }
      });
    }
  }

  private void fillList(final int dx)
  {
    int edge = 0;
    View child = getChildAt(getChildCount() - 1);
    if (child != null)
    {
      edge = child.getRight();
    }
    fillListRight(edge, dx);

    edge = 0;
    child = getChildAt(0);
    if (child != null)
    {
      edge = child.getLeft();
    }
    fillListLeft(edge, dx);
  }

  private void fillListRight(int rightEdge, final int dx)
  {
    while (rightEdge + dx < getWidth() && mRightViewIndex < mAdapter.getCount())
    {
      View child = mAdapter.getView(mRightViewIndex, mRemovedViewQueue.poll(), this);
      addAndMeasureChild(child, -1);
      rightEdge += child.getMeasuredWidth();

      if (mRightViewIndex == mAdapter.getCount() - 1)
      {
        mMaxX = mCurrentX + rightEdge - getWidth();
      }

      if (mMaxX < 0)
      {
        mMaxX = 0;
      }
      mRightViewIndex++;
    }
  }

  private void fillListLeft(int leftEdge, final int dx)
  {
    while(leftEdge + dx > 0 && mLeftViewIndex >= 0)
    {
      View child = mAdapter.getView(mLeftViewIndex, mRemovedViewQueue.poll(), this);
      addAndMeasureChild(child, 0);
      leftEdge -= child.getMeasuredWidth();
      mLeftViewIndex--;
      mDisplayOffset -= child.getMeasuredWidth();
    }
  }

  private void removeNonVisibleItems(final int dx)
  {
    View child = getChildAt(0);
    while (child != null && child.getRight() + dx <= 0)
    {
      mDisplayOffset += child.getMeasuredWidth();
      mRemovedViewQueue.offer(child);
      removeViewInLayout(child);
      mLeftViewIndex++;
      child = getChildAt(0);
    }

    child = getChildAt(getChildCount() - 1);
    while (child != null && child.getLeft() + dx >= getWidth())
    {
      mRemovedViewQueue.offer(child);
      removeViewInLayout(child);
      mRightViewIndex--;
      child = getChildAt(getChildCount() - 1);
    }
  }

  private void positionItems(final int dx)
  {
    if (getChildCount() > 0)
    {
      mDisplayOffset += dx;
      int left = mDisplayOffset;
      for (int i = 0; i < getChildCount(); i++)
      {
        View child = getChildAt(i);
        int childWidth = child.getMeasuredWidth();
        child.layout(left, 0, left + childWidth, child.getMeasuredHeight());
        left += childWidth;
      }
    }
  }

  public synchronized void scrollTo(int x)
  {
    mScroller.startScroll(mNextX, 0, x - mNextX, 0);
    requestLayout();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev)
  {
    boolean handled = super.dispatchTouchEvent(ev);
    handled |= mGesture.onTouchEvent(ev);
    return handled;
  }

  protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                            float velocityY)
  {
    synchronized (HorizontalListView.this)
    {
      mScroller.fling(mNextX, 0, (int)-velocityX, 0, 0, mMaxX, 0, 0);
    }
    requestLayout();

    return true;
  }

  protected boolean onDown(MotionEvent e)
  {
    mScroller.forceFinished(true);
    return true;
  }

  // The following code has been added by Stonyx
  // Copyright (C) 2011 Stonyx

  private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener()
  {
    // Method called to make one of the child views pressed
    private void PressChild(int x, int y)
    {
      // Loop thru the children and press the correct child
      int count = getChildCount();
      for (int i = 0; i < count; ++i)
      {
        // Get the child
        View child = getChildAt(i);
        
        // Check if this was the child that was pressed
        if (x > child.getLeft() && x < child.getRight() &&
            y > child.getTop() && y < child.getBottom())
          child.setPressed(true);
      }
    }
    
    // Method called to release any child that is pressed
    private void ReleaseChild()
    {
      // Loop thru the children and release any that are pressed
      int count = getChildCount();
      for (int i = 0; i < count; ++i)
      {
        // Get the child
        View child = getChildAt(i);
        
        // Check if it's pressed
        if (child.isPressed() == true)
          child.setPressed(false);
      }          
    }
    
    @Override
    public boolean onDown(MotionEvent e)
    {
      // Press the child
      PressChild((int)e.getX(), (int)e.getY());
      
      return HorizontalListView.this.onDown(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY)
    {
      // Release the child
      ReleaseChild();

      return HorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY)
    {
      // Release the child
      ReleaseChild();

      synchronized (HorizontalListView.this)
      {
        mNextX += (int)distanceX;
      }
      requestLayout();

      return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
      // Check if any child was clicked
      if (getChildCount() > 0)
      {
        // Get the first and last child
        View first = getChildAt(0);
        View last = getChildAt(getChildCount() - 1);

        // Check if any child was clicked (only works if all children have the
        //    same height) and play the click sound
        int x = (int)e.getX();
        int y = (int)e.getY();
        if (x > first.getLeft() && x < last.getRight() &&
            y > first.getTop() && y < last.getBottom())
          playSoundEffect(SoundEffectConstants.CLICK);
      }

      Rect viewRect = new Rect();
      for (int i = 0; i < getChildCount(); i++)
      {
        View child = getChildAt(i);
        int left = child.getLeft();
        int right = child.getRight();
        int top = child.getTop();
        int bottom = child.getBottom();
        viewRect.set(left, top, right, bottom);
        if (viewRect.contains((int)e.getX(), (int)e.getY()))
        {
          if (mOnItemClicked != null)
          {
            mOnItemClicked.onItemClick(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          if (mOnItemSelected != null)
          {
            mOnItemSelected.onItemSelected(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          break;
        }
      }
      return true;
    }

    // Overridden onSingleTapUp method
    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
      // Release the child
      ReleaseChild();
      
      return false;
    }
    
    // Overridden onDoubleTapEvent method
    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
      // Release the child
      ReleaseChild();
      
      return false;
    }
    
    @Override
    public void onLongPress(MotionEvent e)
    {
      // Release the child
      ReleaseChild();

      Rect viewRect = new Rect();
      int childCount = getChildCount();
      for (int i = 0; i < childCount; i++)
      {
        View child = getChildAt(i);
        int left = child.getLeft();
        int right = child.getRight();
        int top = child.getTop();
        int bottom = child.getBottom();
        viewRect.set(left, top, right, bottom);
        if (viewRect.contains((int)e.getX(), (int)e.getY()))
        {
          if (mOnItemLongClicked != null)
          {
            mOnItemLongClicked.onItemLongClick(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          break;
        }
      }
    }
  };  
  
  // The preceding code has been added by Stonyx
  
  // The following code has been removed by Stonyx
  /*
  private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener()
  {
    @Override
    public boolean onDown(MotionEvent e)
    {
      return HorizontalListView.this.onDown(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY)
    {
      return HorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY)
    {
      synchronized (HorizontalListView.this)
      {
        mNextX += (int)distanceX;
      }
      requestLayout();

      return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
      Rect viewRect = new Rect();
      for (int i = 0; i < getChildCount(); i++)
      {
        View child = getChildAt(i);
        int left = child.getLeft();
        int right = child.getRight();
        int top = child.getTop();
        int bottom = child.getBottom();
        viewRect.set(left, top, right, bottom);
        if (viewRect.contains((int)e.getX(), (int)e.getY()))
        {
          if (mOnItemClicked != null)
          {
            mOnItemClicked.onItemClick(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          if (mOnItemSelected != null)
          {
            mOnItemSelected.onItemSelected(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          break;
        }
      }
      return true;
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
      Rect viewRect = new Rect();
      int childCount = getChildCount();
      for (int i = 0; i < childCount; i++)
      {
        View child = getChildAt(i);
        int left = child.getLeft();
        int right = child.getRight();
        int top = child.getTop();
        int bottom = child.getBottom();
        viewRect.set(left, top, right, bottom);
        if (viewRect.contains((int)e.getX(), (int)e.getY()))
        {
          if (mOnItemLongClicked != null)
          {
            mOnItemLongClicked.onItemLongClick(HorizontalListView.this, child,
                mLeftViewIndex + 1 + i, mAdapter.getItemId(mLeftViewIndex + 1 + i));
          }
          break;
        }
      }
    }
  };
  */
  // The preceding code has been removed by Stonyx
  
}