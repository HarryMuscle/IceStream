/*
 * IceStream - The Official IceFilms Android Application
 * Copyright (C) 2011-2013 Stonyx
 *
 * The IceStream application (and any "covered work" as defined by the GNU General Public
 * License Version 3) is licensed under the GNU General Public License Version 3 (henceforth
 * referred to as "GNU GPL V3") with the following amendments that supersede any relevant wording
 * in the GNU GPL V3:
 *
 * 1. The IceStream application (and any "covered work" as defined by the GNU GPL V3) can be
 *    statically and/or dynamically linked to any source code, library, or application developed
 *    or released by Stonyx (the original authors of the IceStream application), regardless of the
 *    type of license that such source code, library, or application is licensed under.
 *
 * 2. The IceStream application (and any "covered work" as defined by the GNU GPL V3) can not be
 *    distributed for a fee without the prior written consent provided by Stonyx (the original
 *    authors of the IceStream application).
 *
 * The preceding amendments make up part of the license that the IceStream application is licensed
 * under.  They apply to and need to be included (along with the GNU GPL V3) with any derivative
 * work as outlined in the GNU GPL V3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU GPL V3 for more details.
 *
 * You should have received a copy of the GNU GPL V3 along with this program.  If not, see
 * http://www.gnu.org/licenses/.
 */

package info.icefilms.icestream.browse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import info.icefilms.icestream.R;
import com.stonyx.megastream.open.OpenActivity;
import com.stonyx.ads.AdLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BrowseFragment extends Fragment
{
  // Variables
  private LinearLayout[] mHeadingLayout;
  private HorizontalListView[] mHeading;
  private ArrayList<Item>[] mHeadingItems;
  private ItemArrayAdapter[] mHeadingAdapter;
  private ImageView mDivider;
  private ListView mList;
  private ArrayList<Item> mListItems;
  private ItemArrayAdapter mListAdapter;
  private AdLayout mAdLayout;
  private Boolean mInitialConnection;
  private LinkedList<State> mCurrentState;
  private LinearLayout mRetryLayout;
  private Button mRetryButton;
  private LoadStateTask mLoadStateTask;
  private OpenSourceLocationTask mOpenSourceLocationTask;
  private ProgressDialog mProgressDialog;
  private AsyncTask<?, ?, ?> mProgressDialogTask;
  private Boolean mProgressDialogShowing;
  private AlertDialog mActionDialog;
  private URL mActionDialogURL;
  private StopParcel mStopParcel;

  // Class used to store details about our current state
  private static class State implements Parcelable
  {
    // Variables
    private Location mLocation;
    private ArrayList<Item>[] mHeadingItems;
    private ArrayList<Item> mListItems;
    private int mListScrollPosition;
    private int mListScrollOffset;

    // Constructor
    protected State(Location location)
    {
      mLocation = location;
    }

    // Constructor used by the parcelable functionality of this class
    @SuppressWarnings("unchecked")
    protected State(Parcel in)
    {
      mLocation = in.readParcelable(null);

      int size = in.readInt();
      if (size != 0)
      {
        mHeadingItems = new ArrayList[size];
        Bundle bundle = in.readBundle();
        for (int i = 0; i < mHeadingItems.length; ++i)
          mHeadingItems[i] = bundle.getParcelableArrayList("HeadingItems" + i);
      }

      size = in.readInt();
      if (size != 0)
      {
        Bundle bundle = in.readBundle();
        mListItems = bundle.getParcelableArrayList("ListItems");
      }

      mListScrollPosition = in.readInt();
      mListScrollOffset = in.readInt();
    }

    // Variable used by the parcelable functionality of this class
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<State> CREATOR =
        new Parcelable.Creator<State>()
        {
          public State createFromParcel(Parcel in)
          {
            return new State(in);
          }

          public State[] newArray(int size)
          {
            return new State[size];
          }
        };

    // Method used by the parcelable functionality of this class
    public int describeContents()
    {
      return 0;
    }

    // Method used by the parcelable functionality of this class
    public void writeToParcel(Parcel dest, int flags)
    {
      dest.writeParcelable(mLocation, flags);

      if (mHeadingItems == null)
      {
        dest.writeInt(0);
      }
      else
      {
        dest.writeInt(mHeadingItems.length);
        Bundle bundle = new Bundle();
        for (int i = 0; i < mHeadingItems.length; ++i)
          bundle.putParcelableArrayList("HeadingItems" + i, mHeadingItems[i]);
        dest.writeBundle(bundle);
      }

      if (mListItems == null)
      {
        dest.writeInt(0);
      }
      else
      {
        dest.writeInt(1);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("ListItems", mListItems);
        dest.writeBundle(bundle);
      }

      dest.writeInt(mListScrollPosition);
      dest.writeInt(mListScrollOffset);
    }

    // Setter and getter methods
    protected Location GetLocation() { return mLocation; }
    protected void SetHeadingItems(ArrayList<Item>[] items) { mHeadingItems = items; }
    protected ArrayList<Item>[] GetHeadingItems() { return mHeadingItems; }
    protected void SetListItems(ArrayList<Item> items) { mListItems = items; }
    protected ArrayList<Item> GetListItems() { return mListItems; }
    protected void SetListScrollPosition(int position) { mListScrollPosition = position; }
    protected int GetListScrollPosition() { return mListScrollPosition; }
    protected void SetListScrollOffset(int offset) { mListScrollOffset = offset; }
    protected int GetListScrollOffset() { return mListScrollOffset; }
  }

  // Method called to start a new OpenLocationTask task if one isn't already running
  private void StartLoadStateTask(final State state)
  {
    // Check if we should add this state to the current state list
    if (mCurrentState.isEmpty() || mCurrentState.getLast() != state)
    {
      // Check if we should save the previous state scroll position
      if (mCurrentState.isEmpty() == false)
      {
        // Calculate the scroll position
        int position = mList.getFirstVisiblePosition();
        View top = mList.getChildAt(0);
        int offset = (top == null) ? 0 : top.getTop();

        // Save the position
        State last = mCurrentState.getLast();
        last.SetListScrollPosition(position);
        last.SetListScrollOffset(offset);
      }

      // Add the state to the current state list
      mCurrentState.add(state);
      Log.d("Ice Stream", "New state added to current state list.");

      // Keep the list to less than 20
      if (mCurrentState.size() > 20)
        mCurrentState.removeFirst();
      Log.d("Ice Stream", "Current state list contains " + mCurrentState.size() +
          " state(s).");
    }

    // Check if we already have heading and list items
    if (state.GetHeadingItems() != null && state.GetListItems() != null)
    {
      // Load the items
      LoadStateItems(state);

      // Restore the scroll position
      new Handler().postDelayed(new Runnable()
          {
            public void run()
            {
              mList.setSelectionFromTop(state.GetListScrollPosition(),
                  state.GetListScrollOffset());
              Log.d("Ice Stream", "Set list to scroll position " +
                  state.GetListScrollPosition() + " & offset " +
                  state.GetListScrollOffset() + ".");
            }
          }, 100);
    }
    else if (mLoadStateTask == null || (mLoadStateTask != null &&
             (mLoadStateTask.isCancelled() ||
             mLoadStateTask.getStatus() == AsyncTask.Status.FINISHED)))
    {
      // Create and run the new task
      mLoadStateTask = new LoadStateTask();
      mLoadStateTask.execute(state);
    }
  }

  // Class responsible for retrieving information on a separate thread
  private class LoadStateTask extends AsyncTask<State, Void, State>
  {
    // Overridden method that runs before the onInBackground method
    @Override
    protected void onPreExecute()
    {
      // Show the progress dialog
      ShowProgressDialog(this);
    }

    // Overridden method that does all the work
    @Override
    protected State doInBackground(State... params)
    {
      // Get the activity
      Activity activity = GetActivity();
      if (activity == null)
        return null;

      // Create the callback object
      final Location.Callback callback = new Location.Callback(activity, this);

      // Get the heading items
      Log.d("Ice Stream", "Attempting to retrieve headings.");
      ArrayList<Item>[] headingItems = params[0].GetLocation().GetHeadingItems(callback);
      if (headingItems == null)
      {
        if (callback.IsCancelled())
        {
          Log.d("Ice Stream", "Async Task cancelled, no headings retrieved.");
          return null;
        }
      }
      else
      {
        Log.d("Ice Stream", "Retrieved " + headingItems.length + " heading(s).");
      }

      // Get a list of the list items
      ArrayList<Item> listItems = null;
      if (headingItems != null)
      {
        Log.d("Ice Stream", "Attempting to retrieve list items.");
        listItems = params[0].GetLocation().GetListItems(callback);
        if (listItems == null)
        {
          if (callback.IsCancelled())
          {
            Log.d("Ice Stream", "Async Task cancelled, no list items retrieved.");
            return null;
          }
        }
        else
        {
          Log.d("Ice Stream", "Retieved " + listItems.size() + " list item(s).");
        }
      }

      // Check for warnings and errors
      if (callback.GetWarningBoolean() == true || callback.GetErrorBoolean() == true)
      {
        if (callback.GetWarningBoolean() == true)
        {
          Log.w("Ice Stream", "Warning occurred while retrieving headings or list items. " +
              "Warning toast message: " + getString(callback.GetWarningStringID()));
        }

        if (callback.GetErrorBoolean() == true)
        {
          if (headingItems == null)
            Log.e("Ice Stream", "Error retrieving headings. Error dialog message: " +
                getString(callback.GetErrorStringID()));
          else if (listItems == null)
            Log.e("Ice Stream", "Error retrieving list items. Error dialog message: " +
                getString(callback.GetErrorStringID()));
        }

        // Display the warning toast and/or error dialog
        getActivity().runOnUiThread(new Runnable()
            {
              public void run()
              {
                Activity activity = GetActivity();
                if (callback.IsCancelled() == false && activity != null &&
                    activity.isFinishing() == false)
                {
                  if (callback.GetWarningBoolean() == true)
                  {
                    // Create and display a toast
                    Toast toast = Toast.makeText(getActivity(), callback.GetWarningStringID(),
                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 96);
                    toast.show();
                  }

                  if (callback.GetErrorBoolean() == true)
                  {
                    // Create and display an error dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(callback.GetErrorStringID());
                    builder.setPositiveButton("OK", null);
                    builder.setCancelable(true);
                    builder.create().show();
                  }
                }
              }
            });

        if (callback.GetErrorBoolean() == true)
          return null;
      }

      // Save the items in the state and return it
      params[0].SetHeadingItems(headingItems);
      params[0].SetListItems(listItems);
      return params[0];
    }

    // Overridden method called when this task gets cancelled
    @Override
    protected void onCancelled()
    {
      // Remove the last state added to the current state list
      // TODO: One error received from this line of code - NoSuchElementException
      mCurrentState.removeLast();
      Log.d("Ice Stream", "Last state removed from current state list.");

      // Check if this is the initial connection that was cancelled and show
      //    the retry button
      if (mInitialConnection == true)
      {
        mInitialConnection = false;
        mRetryLayout.setVisibility(View.VISIBLE);
      }
    }

    // Overridden method that runs after the onInBackground method
    @Override
    protected void onPostExecute(State result)
    {
      // Check for any errors
      if (result == null)
      {
        // Run the onCancelled method
        onCancelled();

        // Dismiss the progress dialog
        DismissProgressDialog();

        return;
      }

      // Load the state items
      LoadStateItems(result);

      // Scroll back to the top
      new Handler().postDelayed(new Runnable()
          {
            public void run()
            {
              mList.setSelection(0);
              Log.d("Ice Stream", "Set list to scroll position 0.");
            }
          }, 100);

      // Clear the initial connection boolean
      if (mInitialConnection == true)
        mInitialConnection = false;

      // Hide the retry button
      if (mRetryLayout.getVisibility() == View.VISIBLE)
        mRetryLayout.setVisibility(View.GONE);

      // Dismiss the progress dialog
      DismissProgressDialog();
    }
  }

  // Method called to load the actual state items
  private void LoadStateItems(State state)
  {
    // Loop thru the heading items
    for (int i = 0; i < mHeadingItems.length; ++i)
    {
      // Clear the heading items
      mHeadingItems[i].clear();

      if (i < state.GetHeadingItems().length)
      {
        // Add the heading items from the state
        mHeadingItems[i].addAll(state.GetHeadingItems()[i]);

        // Show this heading
        if (mHeadingLayout[i].getVisibility() == View.GONE)
          mHeadingLayout[i].setVisibility(View.VISIBLE);
      }
      else
      {
        // Hide this heading
        if (mHeadingLayout[i].getVisibility() == View.VISIBLE)
          mHeadingLayout[i].setVisibility(View.GONE);
      }

      // Notify the adapter that we changed the list
      mHeadingAdapter[i].notifyDataSetChanged();
    }

    // Show the divider
    if (mDivider.getVisibility() == View.GONE)
      mDivider.setVisibility(View.VISIBLE);

    // Clear the list items
    mListItems.clear();

    // Add the list items from the result array
    mListItems.addAll(state.GetListItems());

    // Notify the adapter that we changed the list
    mListAdapter.notifyDataSetChanged();
  }

  // Method called to start a new OpenSourceLocationTask task if one isn't already running
  private void StartOpenSourceLocationTask(SourceLocation location)
  {
    // Create and run the new task
    if (mOpenSourceLocationTask == null || (mOpenSourceLocationTask != null &&
        (mOpenSourceLocationTask.isCancelled() ||
        mOpenSourceLocationTask.getStatus() == AsyncTask.Status.FINISHED)))
    {
      mOpenSourceLocationTask = new OpenSourceLocationTask();
      mOpenSourceLocationTask.execute(location);
    }
  }

  // Class responsible for open a source location on a separate thread
  private class OpenSourceLocationTask extends AsyncTask<SourceLocation, Void, URL>
  {
    // Overridden method that runs before the onInBackground method
    @Override
    protected void onPreExecute()
    {
      // Show the progress dialog
      ShowProgressDialog(this);
    }

    // Overridden method that does all the work
    @Override
    protected URL doInBackground(SourceLocation... params)
    {
      // Get the activity
      Activity activity = GetActivity();
      if (activity == null)
        return null;

      // Create the callback object
      final Location.Callback callback = new Location.Callback(activity, this);

      // Get the actual URL for this source
      Log.d("Ice Stream", "Attempting to retrieve source URL.");
      URL url = params[0].GetURL(callback);
      if (url == null)
      {
        if (callback.IsCancelled())
        {
          Log.d("Ice Stream", "Async Task cancelled, no source URL retrieved.");
          return null;
        }
      }
      else
      {
        Log.d("Ice Stream", "Retrieved source URL: " + url.toString());
      }

      // Check for warnings and errors
      if (callback.GetWarningBoolean() == true || callback.GetErrorBoolean() == true)
      {
        if (callback.GetWarningBoolean() == true)
          Log.w("Ice Stream", "Warning occurred while retrieving source URL. " +
              "Warning toast message: " + getString(callback.GetWarningStringID()));

        if (callback.GetErrorBoolean() == true)
          Log.e("Ice Stream", "Error retrieving source URL. Error dialog message: " +
              getString(callback.GetErrorStringID()));

        // Display the warning toast and/or error dialog
        getActivity().runOnUiThread(new Runnable()
            {
              public void run()
              {
                Activity activity = GetActivity();
                if (callback.IsCancelled() == false && activity != null &&
                    activity.isFinishing() == false)
                {
                  if (callback.GetWarningBoolean() == true)
                  {
                    // Create and display a toast
                    Toast toast = Toast.makeText(getActivity(), callback.GetWarningStringID(),
                        Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 96);
                    toast.show();
                  }

                  if (callback.GetErrorBoolean() == true)
                  {
                    // Create and display an error dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(callback.GetErrorStringID());
                    builder.setPositiveButton("OK", null);
                    builder.setCancelable(true);
                    builder.create().show();
                  }
                }
              }
            });

        if (callback.GetErrorBoolean() == true)
          return null;
      }

      return url;
    }

    // Overridden method that runs after the onInBackground method
    @Override
    protected void onPostExecute(URL result)
    {
      // Dismiss the progress dialog
      mProgressDialog.dismiss();

      // Show a dialog with action choices
      if (result != null)
        ShowActionDialog(result);
    }
  }

  // Method called to get the activity
  // Returns null if an interrupt exception is encountered otherwise returns
  //    the activity, waiting for it if necessary during a screen rotation
  private Activity GetActivity()
  {
    Activity activity;
    while ((activity = getActivity()) == null)
    {
      try
      {
        Log.w("Ice Stream", "getActivity method returned null. Waiting 100ms before " +
            "calling getActivity method again.");
        Thread.sleep(100);
      }
      catch (InterruptedException exception)
      {
        Log.d("Ice Stream", "Interrupted Exception encountered.", exception);
        return null;
      }
    }

    return activity;
  }

  // Method called to show the progress dialog
  private void ShowProgressDialog(final AsyncTask<?, ?, ?> task)
  {
    synchronized (mProgressDialogShowing)
    {
      // Save the passed task
      mProgressDialogTask = task;

      // Show the progress dialog
      mProgressDialog = ProgressDialog.show(getActivity(), "",
                        getString(R.string.browse_progress), true, true,
                        new OnCancelListener()
                        {
                          public void onCancel(DialogInterface dialog)
                          {
                            synchronized (mProgressDialogShowing)
                            {
                              // Cancel the task
                              task.cancel(true);

                              // Signal that the progress dialog will not longer be
                              //    showing
                              mProgressDialogShowing = false;
                            }
                          }
                        });
      mProgressDialogShowing = true;
    }
  }

  // Method called to restore the progress dialog
  private void RestoreProgressDialog()
  {
    synchronized (mProgressDialogShowing)
    {
      if (mProgressDialog != null && mProgressDialog.isShowing() == false &&
          mProgressDialogTask != null && mProgressDialogShowing == true)
        ShowProgressDialog(mProgressDialogTask);
    }
  }

  // Method called to dismiss the progress dialog
  private void DismissProgressDialog()
  {
    synchronized (mProgressDialogShowing)
    {
      if (mProgressDialog != null && mProgressDialog.isShowing() == true)
      {
        mProgressDialog.dismiss();
        mProgressDialogShowing = false;
      }
    }
  }

  // Method called to show the action dialog
  private void ShowActionDialog(final URL url)
  {
    // Save the passed URL
    mActionDialogURL = url;

    // Create and show the action dialog
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setItems(new String[] {"Open (Stream)", "Open (Download)", "Save"},
        new OnClickListener()
        {
          public void onClick(DialogInterface dialog, int item)
          {
            // Create the intent
            Intent intent = new Intent(getActivity(), OpenActivity.class);

            // Add the mode
            switch (item)
            {
            case 0:
              intent.putExtra("Mode", OpenActivity.Mode.STREAM_OPEN.GetValue());
              break;
            case 1:
              intent.putExtra("Mode", OpenActivity.Mode.DOWNLOAD_OPEN.GetValue());
              break;
            case 2:
              intent.putExtra("Mode", OpenActivity.Mode.DOWNLOAD_SAVE.GetValue());
              break;
            }

            // Add the link to the extras and open the activity
            intent.putExtra("Link", url);
            startActivity(intent);
          }
        });
    builder.setCancelable(true);
    mActionDialog = builder.create();
    mActionDialog.show();
  }

  // Method called to restore the action dialog
  private void RestoreActionDialog()
  {
    if (((mActionDialog != null && mActionDialog.isShowing() == false) ||
        mActionDialog == null) && mActionDialogURL != null)
      ShowActionDialog(mActionDialogURL);
  }

  // The item array adapter class
  private class ItemArrayAdapter extends ArrayAdapter<Item>
  {
    // Variable
    List<Item> mItems;

    // Constructor
    public ItemArrayAdapter(Context context, int textViewResourceId,
                            ArrayList<Item> objects)
    {
      // Call the base class constructor and store the passed info
      super(context, textViewResourceId, objects);
      mItems = objects;
    }

    // Overridden method called to create the view in the list
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      return mItems.get(position).GetView(getActivity());
    }
  }

  // The heading list listener
  private OnItemClickListener mHeadingListener = new OnItemClickListener()
  {
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
      // Check which heading was clicked
      int i;
      for (i = 0; i < mHeading.length; ++i)
        if (parent == mHeading[i])
          break;

      // Load the location if it's not already selected
      if (((HeadingItem)mHeadingItems[i].get(position)).IsSelected() == false)
        // TODO: The line below (or possibly above) received an error once - ArrayIndexOutOfBoundsException
        StartLoadStateTask(new State(Location.CreateLocationForItem(mHeadingItems[i].
            get(position))));
    }
  };

  // The list listener
  private OnItemClickListener mListListener = new OnItemClickListener()
  {
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
      // Open the source or load the location
      if (mListItems.get(position) instanceof SourceItem)
      {
        // Open the source location
        StartOpenSourceLocationTask((SourceLocation)Location.
            CreateLocationForItem(mListItems.get(position)));
      }
      else if (!(mListItems.get(position) instanceof InfoItem))
      {
        // Open the location
        StartLoadStateTask(new State(Location.CreateLocationForItem(mListItems.
            get(position))));
      }
    }
  };

  // Method called when the back button is pressed
  public void OnBackPressed()
  {
    // Check if we should exit the activity
    if (mCurrentState.size() < 2)
    {
      getActivity().finish();
      return;
    }

    // Remove the last location from the current location list
    mCurrentState.removeLast();
    Log.d("Ice Stream", "Last state removed from current state list.");

    // Remove and open the now current location from the current location list
    StartLoadStateTask(mCurrentState.getLast());
    Log.d("Ice Stream", "New last state loaded as current state.");
  }

  // Class used to save information when pausing or saving the instance of the fragment
  private static class StopParcel implements Parcelable
  {
    // Variables
    private int[] mHeadingLayoutVisibility;
    private int mDividerVisibility;
    private int mRetryLayoutVisibility;
    private int mListScrollPosition;
    private int mListScrollOffset;
    private boolean mRestoreProgressDialog;
    private boolean mRestoreActionDialog;
    private URL mActionDialogURL;

    // Constructor
    protected StopParcel(BrowseFragment fragment)
    {
      // Create the heading layout visibility array
      mHeadingLayoutVisibility = new int[fragment.mHeadingLayout.length];

      // Save the heading layout visibility
      for (int i = 0; i < mHeadingLayoutVisibility.length; ++i)
        mHeadingLayoutVisibility[i] = fragment.mHeadingLayout[i].getVisibility();

      // Save the divider and retry layout visibility
      mDividerVisibility = fragment.mDivider.getVisibility();
      mRetryLayoutVisibility = fragment.mRetryLayout.getVisibility();

      // Save the list scroll position and offset
      mListScrollPosition = fragment.mList.getFirstVisiblePosition();
      View top = fragment.mList.getChildAt(0);
      mListScrollOffset = (top == null) ? 0 : top.getTop();

      // Save the progress dialog show state
      if (fragment.mProgressDialog != null && fragment.mProgressDialog.isShowing())
        mRestoreProgressDialog = true;
      else
        mRestoreProgressDialog = false;

      // Save the action dialog show state
      if (fragment.mActionDialog != null && fragment.mActionDialog.isShowing())
        mRestoreActionDialog = true;
      else
        mRestoreActionDialog = false;

      // Save the action dialog URL
      mActionDialogURL = fragment.mActionDialogURL;
    }

    // Constructor used by the parcelable functionality of this class
    protected StopParcel(Parcel in)
    {
      mHeadingLayoutVisibility = in.createIntArray();
      mDividerVisibility = in.readInt();
      mRetryLayoutVisibility = in.readInt();
      mListScrollPosition = in.readInt();
      mListScrollOffset = in.readInt();
      boolean[] array = new boolean[2];
      in.readBooleanArray(array);
      mRestoreProgressDialog = array[0];
      mRestoreActionDialog = array[1];
      String string = in.readString();
      if (string.length() > 0)
      {
        try
        {
          mActionDialogURL = new URL(string);
        }
        catch (MalformedURLException exception)
        {
          mActionDialogURL = null;
        }
      }
      else
      {
        mActionDialogURL = null;
      }
    }

    // Variable used by the parcelable functionality of this class
    @SuppressWarnings("unused")
    public static final Parcelable.Creator<StopParcel> CREATOR =
        new Parcelable.Creator<StopParcel>()
        {
          public StopParcel createFromParcel(Parcel in)
          {
            return new StopParcel(in);
          }

          public StopParcel[] newArray(int size)
          {
            return new StopParcel[size];
          }
        };

    // Method used by the parcelable functionality of this class
    public int describeContents()
    {
      return 0;
    }

    // Method used by the parcelable functionality of this class
    public void writeToParcel(Parcel out, int flags)
    {
      out.writeIntArray(mHeadingLayoutVisibility);
      out.writeInt(mDividerVisibility);
      out.writeInt(mRetryLayoutVisibility);
      out.writeInt(mListScrollPosition);
      out.writeInt(mListScrollOffset);
      out.writeBooleanArray(new boolean[] { mRestoreProgressDialog, mRestoreActionDialog });
      if (mActionDialogURL != null)
        out.writeString(mActionDialogURL.toString());
      else
        out.writeString("");
    }
  }

  // Overridden onCreate method
  @SuppressWarnings("unchecked")
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    // Call base class method
    super.onCreate(savedInstanceState);

    // Create the heading layout and heading arrays
    mHeadingLayout = new LinearLayout[3];
    mHeading = new HorizontalListView[3];

    // Create the heading items and list items arrays
    mHeadingItems = new ArrayList[3];
    for (int i = 0; i < mHeadingItems.length; ++i)
      mHeadingItems[i] = new ArrayList<Item>();
    mListItems = new ArrayList<Item>();

    // Create the boolean object representing the progress dialog state
    mProgressDialogShowing = false;
  }

  // Overridden onCreateView method
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    // Inflate our xml layout
    LinearLayout layout = (LinearLayout)inflater.inflate(R.layout.browse_fragment,
                          container, false);

    // Add the ad layout
    mAdLayout = new AdLayout(getActivity(), 30000, true);
    LinearLayout.LayoutParams adLayoutLP =
        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
        (int)(50f * getResources().getDisplayMetrics().density + 0.5f));
    adLayoutLP.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    layout.addView(mAdLayout, adLayoutLP);

    return layout;
  }

  // Overridden onActivityCreated method
  @SuppressWarnings("unchecked")
  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    // Call base class method
    super.onActivityCreated(savedInstanceState);

    // Save the views
    mHeadingLayout[0] = (LinearLayout)getActivity().findViewById(R.id.heading1_layout);
    mHeading[0] = (HorizontalListView)getActivity().findViewById(R.id.heading1);
    mHeadingLayout[1] = (LinearLayout)getActivity().findViewById(R.id.heading2_layout);
    mHeading[1] = (HorizontalListView)getActivity().findViewById(R.id.heading2);
    mHeadingLayout[2] = (LinearLayout)getActivity().findViewById(R.id.heading3_layout);
    mHeading[2] = (HorizontalListView)getActivity().findViewById(R.id.heading3);
    mDivider = (ImageView)getActivity().findViewById(R.id.divider);
    mList = (ListView)getActivity().findViewById(R.id.list);
    mRetryLayout = (LinearLayout)getActivity().findViewById(R.id.retry_layout);
    mRetryButton = (Button)getActivity().findViewById(R.id.retry_button);

    // Create the array adapters
    mHeadingAdapter = new ItemArrayAdapter[3];
    for (int i = 0; i < mHeadingAdapter.length; ++i)
      mHeadingAdapter[i] = new ItemArrayAdapter(getActivity(), 0, mHeadingItems[i]);
    mListAdapter = new ItemArrayAdapter(getActivity(), 0, mListItems);

    // Set the list adapters
    for (int i = 0; i < mHeading.length; ++i)
      mHeading[i].setAdapter(mHeadingAdapter[i]);
    mList.setAdapter(mListAdapter);

    // Set the heading click listeners
    for (int i = 0; i < mHeading.length; ++i)
      mHeading[i].setOnItemClickListener(mHeadingListener);
    mList.setOnItemClickListener(mListListener);

    // Create or load certain items
    if (savedInstanceState == null && mStopParcel == null)
    {
      // Set the initial connection boolean and create the current location and
      //    list scroll position lists
      mInitialConnection = true;
      mCurrentState = new LinkedList<State>();
      Log.d("Ice Stream", "New current state list created.");
    }
    else if (savedInstanceState != null)
    {
      // Get the initial connection boolean and the current location list
      mInitialConnection = savedInstanceState.getBoolean("InitialConnection");
      mCurrentState = new LinkedList<State>((Collection<? extends State>)Arrays.
                      asList(savedInstanceState.getParcelableArray("CurrentState")));
      Log.d("Ice Stream", "Loaded current state list from saved instance state bundle.");
      Log.d("Ice Stream", "Current state list contains " + mCurrentState.size() +
          " state(s).");

      // Get the heading items lists
      for (int i = 0; i < mHeadingItems.length; ++i)
      {
        mHeadingItems[i].addAll((Collection<? extends Item>)savedInstanceState.
            getParcelableArrayList("Heading" + (i + 1) + "Items"));
        Log.d("Ice Stream", "Loaded " + mHeadingItems[i].size() + " heading items " +
            "from saved instance state bundle into heading " + (i + 1) + " items list.");
        mHeadingAdapter[i].notifyDataSetChanged();
      }

      // Get the list items list
      mListItems.addAll((Collection<? extends Item>)savedInstanceState.
          getParcelableArrayList("ListItems"));
      Log.d("Ice Stream", "Loaded " + mListItems.size() + " list items from saved instance " +
          "state bundle into list items list.");
      mListAdapter.notifyDataSetChanged();

      // Get the stop parcel from the saved instance state bundle
      mStopParcel = savedInstanceState.getParcelable("StopParcel");
    }

    // Get the current state
    State state;
    if (mCurrentState.isEmpty())
    {
      // Create a new location based on the passed URL
      URL url = (URL)getArguments().get("URL");
      state = new State(Location.CreateLocationForItem(new HeadingItem(url)));
    }
    else
    {
      state = mCurrentState.getLast();
    }

    // Load the state
    StartLoadStateTask(state);

    // Set the retry button click listener
    final State retryState = state;
    mRetryButton.setOnClickListener(new View.OnClickListener()
    {
      public void onClick(View view)
      {
        StartLoadStateTask(retryState);
      }
    });
  }

  // Overridden onStart method
  @Override
  public void onStart()
  {
    // Call base class method
    super.onStart();

    if (mStopParcel != null)
    {
      // Restore the heading, divider, and retry button visibility
      for (int i = 0; i < mHeadingLayout.length; ++i)
        mHeadingLayout[i].setVisibility(mStopParcel.mHeadingLayoutVisibility[i]);
      mDivider.setVisibility(mStopParcel.mDividerVisibility);
      mRetryLayout.setVisibility(mStopParcel.mRetryLayoutVisibility);

      // Restore the list scroll position
      new Handler().postDelayed(new Runnable()
      {
        public void run()
        {
          mList.setSelectionFromTop(mStopParcel.mListScrollPosition,
              mStopParcel.mListScrollOffset);
        }
      }, 100);

      // Restore the progress dialog
      if (mStopParcel.mRestoreProgressDialog)
        RestoreProgressDialog();

      // Restore the action dialog
      if (mStopParcel.mRestoreActionDialog)
      {
        mActionDialogURL = mStopParcel.mActionDialogURL;
        RestoreActionDialog();
      }
    }
  }

  // Overridden onResume method
  @Override
  public void onResume()
  {
    // Call base class method
    super.onResume();

    // Resume the ad layout refresh cycle
    if (mAdLayout != null)
      mAdLayout.Resume();
  }

  // Overridden onSaveInstanceState method
  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    // Call base class method
    super.onSaveInstanceState(outState);

    // Save the stop parcel and other settings and lists
    outState.putParcelable("StopParcel", new StopParcel(this));
    outState.putBoolean("InitialConnection", mInitialConnection);
    outState.putParcelableArrayList("Heading1Items", mHeadingItems[0]);
    outState.putParcelableArrayList("Heading2Items", mHeadingItems[1]);
    outState.putParcelableArrayList("Heading3Items", mHeadingItems[2]);
    outState.putParcelableArrayList("ListItems", mListItems);
    outState.putParcelableArray("CurrentState", mCurrentState.toArray(new State[0]));
  }

  // Overridden onPause method
  public void onPause()
  {
    // Call base class method
    super.onPause();

    // Pause the ad layout refresh cycle
    if (mAdLayout != null)
      mAdLayout.Pause();
  }

  // Overridden onStop method
  @Override
  public void onStop()
  {
    // Call base class method
    super.onStop();

    // Create a stop parcel
    mStopParcel = new StopParcel(this);

    // Dismiss the progress dialog
    if (mProgressDialog != null && mProgressDialog.isShowing())
      mProgressDialog.dismiss();

    // Dismiss the action dialog
    if (mActionDialog != null && mActionDialog.isShowing())
      mActionDialog.dismiss();
  }

  // Overridden onDestroy method
  @Override
  public void onDestroy()
  {
    // Call base class onStop method
    super.onDestroy();

    // Cancel any running tasks
    if (mLoadStateTask != null &&
        mLoadStateTask.getStatus() != AsyncTask.Status.FINISHED)
      mLoadStateTask.cancel(true);
    if (mOpenSourceLocationTask != null &&
        mOpenSourceLocationTask.getStatus() != AsyncTask.Status.FINISHED)
      mOpenSourceLocationTask.cancel(true);

    // Cancel the ad layout refresh cycle
    if (mAdLayout != null)
      mAdLayout.Cancel();
  }
}