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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import info.icefilms.icestream.HelpActivity;
import info.icefilms.icestream.PreferenceHeadersActivity;
import info.icefilms.icestream.PreferencesActivity;
import info.icefilms.icestream.R;

public class BrowseActivity extends FragmentActivity
{
  // Overridden onCreate method
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    // Call base class method
    super.onCreate(savedInstanceState);

    // Set the content view
    setContentView(R.layout.browse_activity);

    // Add the fragment
    if (savedInstanceState == null)
    {
      BrowseFragment fragment = new BrowseFragment();
      fragment.setArguments(getIntent().getExtras());
      fragment.setRetainInstance(true);
      getSupportFragmentManager().beginTransaction().add(R.id.browse_fragment_container,
          fragment, "Browse").commit();
    }
  }

  // Overridden method responsible for creating the menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Call base class method
    super.onCreateOptionsMenu(menu);

    // Inflate the menu
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);

    return true;
  }

  // Overridden method responsible for responding to the menu
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
    case R.id.preferences:
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
        startActivity(new Intent(this, PreferencesActivity.class));
      else
        startActivity(new Intent(this, PreferenceHeadersActivity.class));
      return true;
    case R.id.help:
      startActivity(new Intent(this, HelpActivity.class));
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  // Overridden onBackPressed method
  @Override
  public void onBackPressed()
  {
    // Call the fragment's back button pressed method
    ((BrowseFragment)getSupportFragmentManager().findFragmentByTag("Browse")).OnBackPressed();
  }
}