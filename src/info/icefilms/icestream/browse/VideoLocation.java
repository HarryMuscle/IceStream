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
import android.util.Log;
import info.icefilms.icestream.R;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoLocation extends Location
{
  // Constructor
  protected VideoLocation(URL url)
  {
    super(url);
  }

  // Constructor used by the parcelable functionality of this class
  protected VideoLocation(Parcel in)
  {
    super(in);
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<VideoLocation> CREATOR =
      new Parcelable.Creator<VideoLocation>()
      {
        public VideoLocation createFromParcel(Parcel in)
        {
          return new VideoLocation(in);
        }

        public VideoLocation[] newArray(int size)
        {
          return new VideoLocation[size];
        }
      };

  // Method called to get a list of list items from the passed page
  // Returns null if the Callback IsCancelled method returns true or
  //    if an error occurred or returns an empty list if no items were
  //    found otherwise returns a list of list items
  public ArrayList<Item> OnGetListItems(String page, Callback callback)
  {
    // Create the list
    ArrayList<Item> list = new ArrayList<Item>();

    // Add an info item with the image and description
    InfoItem info = FindImageAndDescriptionInfo(page, callback);
    if (info != null)
      list.add(info);

    // Check if we got cancelled while finding the info item
    if (callback.IsCancelled())
      return null;

    // Get the frame page URL
    URL url;
    try
    {
      url = new URL(GetIceFilmsURL(),
            GetGroup("(/membersonly/components/com_iceplayer/.+?)\" width=", page).
            replace("%28", "(").replace("%29", ")"));
    }
    catch (MalformedURLException exception)
    {
      Log.e("Ice Stream", "Parsing frame page URL failed.", exception);
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_parse_error);
      }
      return null;
    }

    // Get the frame page
    StringBuilder cookieBuilder = new StringBuilder();
    page = DownloadPage(url, null, null, cookieBuilder, callback);
    if (page == null)
    {
      return null;
    }
    else if (page.length() == 0)
    {
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_page_download_error);
      }
      return null;
    }

    // Check if IceFilms is down
    if (page.contains("Database Error") || page.contains("Yer off the edge of the map."))
    {
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_database_error);
      }
      return null;
    }

    // Build the cookie
    String cookie = cookieBuilder.toString();

    // Get the secret
    String secret = GetGroup("f\\.lastChild\\.value=\"([^']+)\",a", page);

    // Get the token
    String token = GetGroup("\"&t=([^\"]+)\",", page);

    // Get the HD links section and add all the links
    String section = GetGroup("<div class=ripdiv><b>HD 720p</b>(.+?)</div>", page);
    if (section != null)
    {
      // Get the sources
      ArrayList<Item> sources = FindSources(section, cookie, secret, token, callback);
      if (sources == null)
        return null;

      // Add an info item and the sources
      if (sources.isEmpty() == false)
      {
        list.add(new InfoItem("HD 720p", null));
        list.addAll(sources);
      }
    }

    // Get the DVD links section and add all the links
    section = GetGroup("<div class=ripdiv><b>DVDRip / Standard Def</b>(.+?)</div>", page);
    if (section != null)
    {
      // Get the sources
      ArrayList<Item> sources = FindSources(section, cookie, secret, token, callback);
      if (sources == null)
        return null;

      // Add an info item and the sources
      if (sources.isEmpty() == false)
      {
        list.add(new InfoItem("DVDRip / Standard Def", null));
        list.addAll(sources);
      }
    }

    // Get the DVD Screener links section and add all the links
    section = GetGroup("<div class=ripdiv><b>DVD Screener</b>(.+?)</div>", page);
    if (section != null)
    {
      // Get the sources
      ArrayList<Item> sources = FindSources(section, cookie, secret, token, callback);
      if (sources == null)
        return null;

      // Add an info item and the sources
      if (sources.isEmpty() == false)
      {
        list.add(new InfoItem("DVD Screener", null));
        list.addAll(sources);
      }
    }

    // Get the R5/R6 links section and add all the links
    section = GetGroup("<div class=ripdiv><b>R5/R6 DVDRip</b>(.+?)</div>", page);
    if (section != null)
    {
      // Get the sources
      ArrayList<Item> sources = FindSources(section, cookie, secret, token, callback);
      if (sources == null)
        return null;

      // Add an info item and the sources
      if (sources.isEmpty() == false)
      {
        list.add(new InfoItem("R5/R6 DVDRip", null));
        list.addAll(sources);
      }
    }

    return list;
  }

  // Method called to find a list of sources
  // Returns null if the Callback IsCancelled method returns true or returns an
  //    empty list if no sources were found otherwise returns a list of source items
  private ArrayList<Item> FindSources(String section, String cookie, String secret,
                                      String token, Callback callback)
  {
    // Create the list
    ArrayList<Item> list = new ArrayList<Item>();

    // Look for up to 20 sources
    for (int i = 1; i < 21; ++i)
    {
      // Check if we got cancelled
      if (callback.IsCancelled())
        return null;

      // Check if this source exists
      if (section.contains("Source #" + i) == false)
        continue;

      // Check if this is source has multiple parts
      if (section.contains("<p>Source #" + i))
      {
        // Get the source string
        String subsection = GetGroup("<p>Source #" + i + ": (.+?PART \\d.+?)</i><p>", section);

        // Get the next source if we didn't find a subsection
        if (subsection == null)
          continue;

        // Get the host
        String host = GetGroup("title=['\"]Hosted by (.+?)['\"]", subsection);
        
        // Define our matcher
        Matcher matcher = Pattern.compile("onclick='go\\((\\d+)\\)'>PART\\s+(\\d+)").matcher(subsection);

        // Loop thru our matches
        while (matcher.find())
        {
          // Check if we got cancelled
          if (callback.IsCancelled() == true)
            return null;

          // Get the info
          String ident = matcher.group(1);
          String part = matcher.group(2);

          // Create the item title using the host name if we found it
          String title;
          if (host != null)
            title = "Source #" + i + " - Part #" + part + " (" + host + ")";
          else
            title = "Source #" + i + " - Part #" + part;
          
          // Add a source item to the list
          SourceItem source = new SourceItem(title, cookie, secret, token, ident);
          list.add(source);
        }
      }
      else
      {
        // Define our matcher
        Matcher matcher = Pattern.compile("<a\\s+rel=" + i + ".+?onclick='go\\((\\d+)\\)'>Source\\s+#" +
                          i + ":(.+?title=['\"]Hosted by (.+?)['\"])?").matcher(section);

        // Get the next source if we didn't find a match
        if (matcher.find() == false)
          continue;
        
        // Get the identification number
        String ident = matcher.group(1);

        // Create the item title using the host name if we found it
        String title;
        if (matcher.group(2) != null)
          title = "Source #" + i + " (" + matcher.group(3) + ")";
        else
          title = "Source #" + i;

        // Add a source item to the list
        SourceItem source = new SourceItem(title, cookie, secret, token, ident);
        list.add(source);
      }
    }

    return list;
  }
}