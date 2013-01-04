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

package info.icefilms.icestream;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import info.icefilms.icestream.browse.BrowseActivity;
import java.net.MalformedURLException;
import java.net.URL;

public class MainFragment extends Fragment
{
  // Variables
  private Button mHomeButton;
  private Button mTVShowsButton;
  private Button mMoviesButton;
  private Button mMusicButton;
  private Button mStandUpButton;
  private Button mOtherButton;

  // The button listener
  private OnClickListener mButtonListener = new OnClickListener()
  {
    public void onClick(View view)
    {
      // Create the intent
      Intent intent = new Intent(getActivity(), BrowseActivity.class);

      // Get the base URL string
      String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).
                        getString("IceFilmsURL", "http://www.icefilms.info/");

      // Clean up the URL string
      if (location.endsWith("/") == false)
        location += "/";
      if (location.startsWith("http://") == false)
        location = "http://" + location;

      // Check which button was clicked and add the correct path to the URL string
      if (view == mTVShowsButton)
        location += "tv/popular/1";
      else if (view == mMoviesButton)
        location += "movies/popular/1";
      else if (view == mMusicButton)
        location += "music/popular/1";
      else if (view == mStandUpButton)
        location += "standup/popular/1";
      else if (view == mOtherButton)
        location += "other/popular/1";

      // Create a URL object from the location string
      URL url;
      try
      {
        url = new URL(location);
      }
      catch (MalformedURLException exception)
      {
        // Build and show an error dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.main_invalid_url_error);
        builder.setPositiveButton("OK", null);
        builder.setCancelable(true);
        builder.create().show();

        return;
      }

      // Add the URL to the intent
      intent.putExtra("URL", url);

      // Start the activity
      startActivity(intent);
    }
  };

  // Overridden onCreateView method
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.main_fragment, container, false);
  }

  // Overridden onActivityCreated method
  @Override
  public void onActivityCreated(Bundle savedInstanceState)
  {
    // Call base class method
    super.onActivityCreated(savedInstanceState);

    // Get all the button views
    mHomeButton = (Button)getActivity().findViewById(R.id.home);
    mTVShowsButton = (Button)getActivity().findViewById(R.id.tvshows);
    mMoviesButton = (Button)getActivity().findViewById(R.id.movies);
    mMusicButton = (Button)getActivity().findViewById(R.id.music);
    mStandUpButton = (Button)getActivity().findViewById(R.id.standup);
    mOtherButton = (Button)getActivity().findViewById(R.id.other);

    // Add the button listeners
    mHomeButton.setOnClickListener(mButtonListener);
    mTVShowsButton.setOnClickListener(mButtonListener);
    mMoviesButton.setOnClickListener(mButtonListener);
    mMusicButton.setOnClickListener(mButtonListener);
    mStandUpButton.setOnClickListener(mButtonListener);
    mOtherButton.setOnClickListener(mButtonListener);
  }
}