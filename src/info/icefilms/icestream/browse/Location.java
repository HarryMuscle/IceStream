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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import info.icefilms.icestream.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.util.ByteArrayBuffer;

public abstract class Location implements Parcelable
{
  // Class used to pass callback methods
  public static class Callback
  {
    // Variable
    private int mConnectionTimeout;
    private boolean mError;
    private int mErrorStringID;
    private boolean mWarning;
    private int mWarningStringID;
    private AsyncTask<?, ?, ?> mTask;

    // Constructor
    public Callback(Activity activity, AsyncTask<?, ?, ?> task)
    {
      try
      {
        mConnectionTimeout = Integer.parseInt(PreferenceManager.
                             getDefaultSharedPreferences(activity).
                             getString("ConnectionTimeout", "20")) * 1000;
      }
      catch (NumberFormatException exception)
      {
        mConnectionTimeout = 20000;
      }
      mError = false;
      mErrorStringID = 0;
      mWarning = false;
      mWarningStringID = 0;
      mTask = task;
    }

    // Setter and getter methods
    public int GetConnectionTimeout() { return mConnectionTimeout; }
    public void SetErrorBoolean(boolean error) { mError = error; }
    public boolean GetErrorBoolean() { return mError; }
    public void SetErrorStringID(int id) { mErrorStringID = id; }
    public int GetErrorStringID() { return mErrorStringID; }
    public void SetWarningBoolean(boolean warning) { mWarning = warning; }
    public boolean GetWarningBoolean() { return mWarning; }
    public void SetWarningStringID(int id) { mWarningStringID = id; }
    public int GetWarningStringID() { return mWarningStringID; }

    // Method called to see if the async task associated with this callback class
    //    has been cancelled
    public boolean IsCancelled()
    {
      return mTask.isCancelled();
    }
  }

  // Variables
  private static URL mIceFilmsURL;
  private URL mURL;

  // Static method called to create a location for a passed item
  public static Location CreateLocationForItem(Item item)
  {
    Location location = null;
    if (item instanceof HeadingItem)
      location = new HeadingLocation(item.GetURL());
    else if (item instanceof SeriesItem)
      location = new SeriesLocation(item.GetURL());
    else if (item instanceof VideoItem)
      location = new VideoLocation(item.GetURL());
    else if (item instanceof SourceItem)
      location = new SourceLocation(((SourceItem)item).GetCookie(),
                 ((SourceItem)item).GetSecret(), ((SourceItem)item).GetToken(),
                 ((SourceItem)item).GetIdent());

    return location;
  }

  // Constructor
  protected Location(URL url)
  {
    // Save the URL
    mURL = url;

    // Make sure we have an IceFilms URL
    if (mIceFilmsURL == null)
    {
      try
      {
        mIceFilmsURL = new URL(mURL.getProtocol(), mURL.getHost(), mURL.getPort(), "");
      }
      catch (MalformedURLException exception) {}
    }
  }

  // Constructor used by the parcelable functionality of this class
  protected Location(Parcel in)
  {
    mIceFilmsURL = (URL)in.readValue(null);
    mURL = (URL)in.readValue(null);
  }

  // Method used by the parcelable functionality of this class
  public int describeContents()
  {
    return 0;
  }

  // Method used by the parcelable functionality of this class
  public void writeToParcel(Parcel dest, int flags)
  {
    dest.writeValue(mIceFilmsURL);
    dest.writeValue(mURL);
  }

  // Getter methods
  public URL GetIceFilmsURL() { return mIceFilmsURL; }
  public URL GetURL() { return mURL; }

  // Method called to get a list of heading items
  // Returns null if the async task calling this method got cancelled or if an
  //    error occurred otherwise returns an array of lists of heading items
  @SuppressWarnings("unchecked")
  public ArrayList<Item>[] GetHeadingItems(Callback callback)
  {
    // Download the page
    String page = DownloadPage(mURL, callback);
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

    // Create the matcher
    Matcher matcher = Pattern.compile("<div class='menu ((indent)|(submenu.*?))'>" +
                      "(.+?)</div>").matcher(page);

    // Get all the heading strings
    Vector<String> headings = new Vector<String>();
    while (matcher.find())
      headings.add(matcher.group(4));

    // Create the array of heading items lists
    ArrayList<Item>[] headingItems = new ArrayList[headings.size()];
    for (int i = 0; i < headingItems.length; ++i)
      headingItems[i] = new ArrayList<Item>();

    // Loop thru the heading strings
    int size = headings.size();
    for (int i = 0; i < size; ++i)
    {
      // Redefine the matcher
      matcher = Pattern.compile("(<a href=(.+?)>(.+?)</a>)|(<b>(.+?)</b>)").
                  matcher(headings.get(i));

        // Loop thru all our matches
      while (matcher.find() == true)
      {
        // Check if we got cancelled
        if (callback.IsCancelled())
          return null;

        // Safely get all the info
        String name;
        URL url;
        boolean selected;
        if (matcher.group(1) != null)
        {
          name = matcher.group(3);
          try
          {
            url = new URL(mIceFilmsURL, matcher.group(2));
          }
          catch (MalformedURLException exception)
          {
            continue;
          }
          selected = false;
        }
        else if (matcher.group(4) != null)
        {
          name = matcher.group(5);
          url = null;
          selected = true;
        }
        else
        {
          continue;
        }

        // Filter out the forum, donate, and random items
        if (name.contains("Forum") || name.contains("Donate") || name.contains("Random"))
          continue;

        // Clean the string
        name = CleanString(name);

        // Add a heading item to the list
        headingItems[i].add(new HeadingItem(name, url, selected));
      }
    }

    // Add a link to home to the first heading
    if (headingItems.length > 0 && headingItems[0].isEmpty() == false)
      headingItems[0].add(0, new HeadingItem("Home", mIceFilmsURL,
          mURL.getPath().length() == 0 || mURL.getPath().equals("/")));

    // Check for any empty lists
    for (int i = 0; i < headingItems.length; ++i)
    {
      if (headingItems[i].isEmpty())
      {
        if (callback.GetErrorBoolean() == false)
        {
          callback.SetErrorBoolean(true);
          callback.SetErrorStringID(R.string.browse_parse_error);
        }
        return null;
      }
    }

    return headingItems;
  }

  // Method called to get a list of list items
  // Returns null if the async task calling this method got cancelled or if an
  //    error occurred otherwise returns a list of list items
  public ArrayList<Item> GetListItems(Callback callback)
  {
    // Download the page
    String page = DownloadPage(mURL, callback);
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

    // Get the list of items
    ArrayList<Item> listItems = OnGetListItems(page, callback);

    // Check for any errors
    if (listItems == null)
    {
      return null;
    }
    else if (listItems.isEmpty())
    {
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_parse_error);
      }
      return null;
    }

    return listItems;
  }

  // Method called by the GetListItems method to get a list of list items
  //    from the passed page
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items
  protected abstract ArrayList<Item> OnGetListItems(String page, Callback callback);

  // Methods called by this and derived classes to download a website page
  // Return null if it was cancelled or if an error occurred otherwise
  //    return a string containing the website page
  protected static String DownloadPage(URL url, Callback callback)
  {
    return DownloadPage(url, null, null, null, callback);
  }
  protected static String DownloadPage(URL url, String sendCookie, String sendData,
                                       StringBuilder getCookie, Callback callback)
  {
    // Declare our buffer
    ByteArrayBuffer byteArrayBuffer;

    try
    {
      // Setup the connection
      HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
      urlConnection.setRequestProperty("User-Agent",
          "Mozilla/5.0 (X11; Linux i686; rv:2.0) Gecko/20100101 Firefox/4.0");
      urlConnection.setRequestProperty("Referer", mIceFilmsURL.toString());
      if (sendCookie != null)
        urlConnection.setRequestProperty("Cookie", sendCookie);
      if (sendData != null)
      {
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setDoOutput(true);
      }
      urlConnection.setConnectTimeout(callback.GetConnectionTimeout());
      urlConnection.setReadTimeout(callback.GetConnectionTimeout());
      urlConnection.connect();

      // Get the output stream and send the data
      if (sendData != null)
      {
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(sendData.getBytes());
        outputStream.flush ();
        outputStream.close ();
      }

      // Get the cookie
      if (getCookie != null)
      {
        // Clear the string builder
        getCookie.delete(0, getCookie.length());

        // Loop thru the header fields
        String headerName;
        String cookie;
        for (int i = 1; (headerName = urlConnection.getHeaderFieldKey(i)) != null; ++i)
        {
          if (headerName.equalsIgnoreCase("Set-Cookie"))
          {
            // Get the cookie
            cookie = GetGroup("([^=]+=[^=;]+)", urlConnection.getHeaderField(i));

            // Add it to the string builder
            if (cookie != null)
              getCookie.append(cookie);

            break;
          }
        }
      }

      // Get the input stream
      InputStream inputStream = urlConnection.getInputStream();

      // For some reason we can actually get a null InputStream instead of an exception
      if (inputStream == null)
      {
        Log.e("Ice Stream", "Page download failed. Unable to create Input Stream.");
        if (callback.GetErrorBoolean() == false)
        {
          callback.SetErrorBoolean(true);
          callback.SetErrorStringID(R.string.browse_page_download_error);
        }
        urlConnection.disconnect();
        return null;
      }

      // Get the file size
      final int fileSize = urlConnection.getContentLength();

      // Create our buffers
      byte[] byteBuffer = new byte[2048];
      byteArrayBuffer = new ByteArrayBuffer(2048);

      // Download the page
      int amountDownloaded = 0;
      int count;
      while ((count = inputStream.read(byteBuffer, 0, 2048)) != -1)
      {
        // Check if we got canceled
        if (callback.IsCancelled())
        {
          inputStream.close();
          urlConnection.disconnect();
          return null;
        }

        // Add data to the buffer
        byteArrayBuffer.append(byteBuffer, 0, count);

        // Update the downloaded amount
        amountDownloaded += count;
      }

      // Close the connection
      inputStream.close();
      urlConnection.disconnect();

      // Check for amount downloaded calculation error
      if (fileSize != -1 && amountDownloaded != fileSize)
      {
        Log.w("Ice Stream", "Total amount downloaded (" + amountDownloaded + " bytes) does not " +
            "match reported content length (" + fileSize + " bytes).");
      }
    }
    catch (SocketTimeoutException exception)
    {
      Log.e("Ice Stream", "Page download failed.", exception);
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_page_timeout_error);
      }
      return null;
    }
    catch (IOException exception)
    {
      Log.e("Ice Stream", "Page download failed.", exception);
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_page_download_error);
      }
      return null;
    }

    // Convert things to a string
    return new String(byteArrayBuffer.toByteArray());
  }

  // Method called by this and derived classes to find an image and description
  //    and create an info item from them
  // Returns null if neither was found otherwise returns an info item
  protected static InfoItem FindImageAndDescriptionInfo(String page, Callback callback)
  {
    // See if there's an image
    Bitmap image = null;
    String imageURL = GetGroup(new String[] {"<a class=img target=_blank href=(.+?)>",
                      "<iframe src=/noref\\.php\\?url=(.+?) "}, page);
    if (imageURL != null)
    {
      // Create the image URL
      URL url;
      try
      {
        url = new URL(mIceFilmsURL, imageURL);
      }
      catch (MalformedURLException exception)
      {
        url = null;
      }

      // Get the image
      if (url != null)
      {
        image = DownloadImage(url, callback);

        // Getting the image is optional so change any errors into warnings
        if (callback.GetErrorBoolean() == true)
        {
          callback.SetWarningBoolean(callback.GetErrorBoolean());
          callback.SetWarningStringID(callback.GetErrorStringID());
          callback.SetErrorBoolean(false);
          callback.SetErrorStringID(0);
        }
      }
    }

    // See if there's a description
    String description = GetGroup(new String[] {"<p>.*?</p>\\s*?<p>.*?</p>\\s*?<p>(.*?)</p>",
                         "<th>Description:</th><td>(.+?) {0,1}<"}, page);
    if (description != null)
      description = CleanString(description);

    // Create and return the info item
    if (image != null || description != null)
      return new InfoItem(description, image);
    else
      return null;
  }

  // Method called by the FindImageAndDescriptionInfo method to download an image
  // Returns null if an error occurred otherwise returns a Bitmap object
  private static Bitmap DownloadImage(URL url, Callback callback)
  {
    try
    {
      // Open the connection
      HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
      urlConnection.setRequestProperty("User-Agent",
          "Mozilla/5.0 (X11; Linux i686; rv:2.0) Gecko/20100101 Firefox/4.0");
      urlConnection.setConnectTimeout(callback.GetConnectionTimeout());
      urlConnection.setReadTimeout(callback.GetConnectionTimeout());
      urlConnection.connect();

      // Get the input stream
      InputStream inputStream = urlConnection.getInputStream();

      // For some reason we can actually get a null InputStream instead of an exception
      if (inputStream == null)
      {
        Log.e("Ice Stream", "Image download failed. Unable to create Input Stream.");
        if (callback.GetErrorBoolean() == false)
        {
          callback.SetErrorBoolean(true);
          callback.SetErrorStringID(R.string.browse_image_download_error);
        }
        urlConnection.disconnect();
        return null;
      }

      // Download the image and create the bitmap
      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

      // Close the connection
      inputStream.close();
      urlConnection.disconnect();

      // Check for errors
      if (bitmap == null)
      {
        Log.e("Ice Stream", "Image data decoding failed.");
        if (callback.GetErrorBoolean() == false)
        {
          callback.SetErrorBoolean(true);
          callback.SetErrorStringID(R.string.browse_image_decode_error);
        }
        return null;
      }

      return bitmap;
    }
    catch (SocketTimeoutException exception)
    {
      Log.e("Ice Stream", "Image download failed.", exception);
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_image_timeout_error);
      }
      return null;
    }
    catch (IOException exception)
    {
      Log.e("Ice Stream", "Image download failed.", exception);
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_image_download_error);
      }
      return null;
    }
  }

  // Methods called by this and derived classes to get a single group using
  //    a single or multiple regular expressions
  // Return null if a match wasn't found otherwise return the group match
  protected static String GetGroup(String regex, String source)
  {
    return GetGroup(new String[] {regex}, -1, source);
  }
  protected static String GetGroup(String regex, int flags, String source)
  {
    return GetGroup(new String[] {regex}, flags, source);
  }
  protected static String GetGroup(String[] regex, String source)
  {
    return GetGroup(regex, -1, source);
  }
  protected static String GetGroup(String[] regex, int flags, String source)
  {
    // Combine all the regular expressions
    String expression = "(" + regex[0] + ")";
    for (int i = 1; i < regex.length; ++i)
      expression += "|(" + regex[i] + ")";

    // Define the matcher
    Matcher matcher;
    if (flags == -1)
      matcher = Pattern.compile(expression).matcher(source);
    else
      matcher = Pattern.compile(expression, flags).matcher(source);

    // Check if we found anything
    if (matcher.find() == false)
    {
      return null;
    }
    else
    {
      // Return the group we found
      for (int i = 2; i <= matcher.groupCount(); i += 2)
        if (matcher.group(i) != null)
          return matcher.group(i);

      return null;
    }
  }

  // Method called by this and derived classes to remove HTML codes and
  //    tags from a string
  protected static String CleanString(String string)
  {
    // Remove any html tags
    string = Pattern.compile("<.*?>", Pattern.DOTALL).matcher(string).replaceAll("");

    // Replace any special HTML characters
    string = StringEscapeUtils.unescapeHtml4(string);

    return string;
  }
}