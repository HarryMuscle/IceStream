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

import android.os.Parcel;
import android.os.Parcelable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadingLocation extends Location
{
  // Constructor
  protected HeadingLocation(URL url)
  {
    super(url);
  }

  // Constructor used by the parcelable functionality of this class
  protected HeadingLocation(Parcel in)
  {
    super(in);
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<HeadingLocation> CREATOR =
      new Parcelable.Creator<HeadingLocation>()
      {
        public HeadingLocation createFromParcel(Parcel in)
        {
          return new HeadingLocation(in);
        }

        public HeadingLocation[] newArray(int size)
        {
          return new HeadingLocation[size];
        }
      };

  // Method called to get a list of list items from the passed page
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items
  public ArrayList<Item> OnGetListItems(String page, Callback callback)
  {
    // Determine what to get based on the URL
    if (GetURL().getPath().length() == 0 || GetURL().getPath().equals("/"))
      return GetHomeVideos(page, callback);
    else if (GetURL().getPath().contains("tv"))
      return GetSeries(page, callback);
    else
      return GetVideos(page, callback);
  }

  // Method called to get a list of videos from the home page
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items
  private ArrayList<Item> GetHomeVideos(String page, Callback callback)
  {
    // Create the list
    ArrayList<Item> list = new ArrayList<Item>();

    // Get the section of the page that contains the info we want
    page = GetGroup("(<h1>Recently Added</h1>.+?)<h1>Statistics</h1>", Pattern.DOTALL, page);
    if (page == null)
      return list;

    // Define our matcher
    Matcher matcher = Pattern.compile("(href=(.+?)>(.+?)</a>)|(<h1>(.+?)</h1>)").matcher(page);

    // Loop thru the found items
    while (matcher.find())
    {
      // Check if we got cancelled
      if (callback.IsCancelled() == true)
        return null;

      // Figure out what we found
      if (matcher.group(1) != null)
      {
        // Safely get all the info
        String name = matcher.group(3);
        URL url;
        try
        {
          url = new URL(GetIceFilmsURL(), matcher.group(2));
        }
        catch (MalformedURLException exception)
        {
          continue;
        }

        // Clean the string
        name = CleanString(name);

        // Add a video item to the list
        list.add(new VideoItem(name, url, -1));
      }
      else if (matcher.group(4) != null)
      {
        // Get the info
        String name = matcher.group(5);

        // Clean the string
        name = CleanString(name);

        // Add an info item to the list
        list.add(new InfoItem(name, null));
      }
    }

    return list;
  }

  // Method called to get a list of series
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items
  private ArrayList<Item> GetSeries(String page, Callback callback)
  {
    // Create the list
    ArrayList<Item> list = new ArrayList<Item>();

    // Define our matcher
    Matcher matcher = Pattern.compile("((<a name=i id=(.+?)></a>){0,1}<img class=star>" +
                      "<a href=/(.+?)>(.+?)</a>([0-9]*).*?<br>)|(<h3>(.+?)</h3>)").matcher(page);

    // Loop thru the found items
    while (matcher.find())
    {
      // Check if we got cancelled
      if (callback.IsCancelled() == true)
        return null;

      // Figure out what we found
      if (matcher.group(1) != null)
      {
        // Safely get all the info
        String name = matcher.group(5);
        URL url;
        try
        {
          url = new URL(GetIceFilmsURL(), matcher.group(4));
        }
        catch (MalformedURLException exception)
        {
          continue;
        }

        int id;
        try
        {
          id = Integer.parseInt(matcher.group(3));
        }
        catch (NumberFormatException exception)
        {
          id = -1;
        }

        int episodes;
        try
        {
          episodes = Integer.parseInt(matcher.group(6));
        }
        catch (NumberFormatException exception)
        {
          episodes = -1;
        }

        // Clean the string
        name = CleanString(name);

        // Add a series item to the list
        list.add(new SeriesItem(name, url, id, episodes));
      }
      else if (matcher.group(7) != null)
      {
        // Get the info
        String name = matcher.group(8);

        // Clean the string
        name = CleanString(name);

        // Add an info item to the list
        list.add(new InfoItem(name, null));
      }
    }

    return list;
  }

  // Method called by derived classes to get a list of video items
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items
  protected ArrayList<Item> GetVideos(String page, Callback callback)
  {
    // Create the list
    ArrayList<Item> list = new ArrayList<Item>();

    // Add an info item with the image and description if they exist
    InfoItem info = FindImageAndDescriptionInfo(page, callback);
    if (info != null)
      list.add(info);

    // Define our matcher
    Matcher matcher = Pattern.compile("((<a name=i id=(.+?)></a>){0,1}<img class=star>" +
                      "<a href=/(.+?)>(.+?)<br>)|(<h3>(.+?)</h3>)").matcher(page);

    // Loop thru the found items
    while (matcher.find())
    {
      // Check if we got cancelled
      if (callback.IsCancelled())
        return null;

      // Figure out what we found
      if (matcher.group(1) != null)
      {
        // Safely get all the info
        String name = matcher.group(5);
        URL url;
        try
        {
          url = new URL(GetIceFilmsURL(), matcher.group(4));
        }
        catch (MalformedURLException exception)
        {
          continue;
        }

        int id;
        try
        {
          id = Integer.parseInt(matcher.group(3));
        }
        catch (NumberFormatException exception)
        {
          id = -1;
        }

        // Format the HD label
        name = name.replace("<b>HD</b>", " - HD");

        // Clean the string
        name = CleanString(name);

        // Add a video item to the list
        list.add(new VideoItem(name, url, id));
      }
      else if (matcher.group(6) != null)
      {
        // Get the info
        String name = matcher.group(7);

        // Remove the top link
        name = name.replace("Top ▲", "");

        // Clean the string
        name = CleanString(name);

        // Add an info item to the list
        list.add(new InfoItem(name, null));
      }
      else
      {
        continue;
      }
    }

    return list;
  }
}