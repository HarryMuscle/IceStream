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

package com.stonyx.megastream.open;

import android.content.Intent;
import android.net.Uri;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.stonyx.ads.AdLayout;
import java.io.File;
import java.net.URL;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

public class PutLockerAndSockShare extends Source
{
  // Define class used to open this source
  private class OpenTask extends Source.OpenTask
  {
    // Overridden method that runs in a separate thread
    @Override
    protected Boolean doInBackground(final URL... params)
    {
      // Create some of the needed variables
      String url = params[0].toExternalForm();
      StringBuilder page = new StringBuilder();
      StringBuilder error = new StringBuilder();
      
      // Create the http client
      DefaultHttpClient client = CreateHttpClient();
      
      // Download the page
      HttpGet get = CreateGetRequest(url);
      if (get == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      Boolean result = DownloadPage(client, get, 0,
                       GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 100 : 50,
                       page, error);
      if (result == null)
      {
        return true;        
      }
      else if (result == false)
      {
        ShowErrorDialog(error.toString());
        return false;
      }
      
      // TODO: grab the possibly redirected address from the last method call and use that
      //       address from here on
      
      // Convert the builder to a string
      String string = page.toString();

      // Check if this file is available
      if (string.contains("file doesn't exist") ||
          string.contains("404 Not Found") ||
          string.contains("file failed to convert"))
      {
        ShowErrorDialog(Strings.ErrorNotAvailable);
        return false;
      }
      
      // Find the hash
      String hash = GetGroup("value=\"([0-9a-fA-F]+?)\" name=\"hash\"", string);
      if (hash == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }

      // Download the stream/download details
      HttpPost post = CreatePostRequest(url, new String[] { "hash", hash,
                      "confirm", "Continue as Free User" });
      result = DownloadPage(client, post,
               GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 100 : 50,
               GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 200 : 100,
               page, error);
      if (result == null)
      {
        return true;
      }
      else if (result == false)
      {
        ShowErrorDialog(error.toString());
        return false;
      }
      
      // Convert the builder to a string
      string = page.toString();
      
      // Find the link information
      String link;
      String stream = GetGroup("\\?stream=(.+?)['\"]", string);
      if (stream != null)
      {
        // Download the url details
        if (url.toLowerCase().contains("putlocker.com"))
          get = CreateGetRequest("http://www.putlocker.com/get_file.php?stream=" + stream);
        else if (url.toLowerCase().contains("sockshare.com"))
          get = CreateGetRequest("http://www.sockshare.com/get_file.php?stream=" + stream);
        else
        {
          ShowErrorDialog(Strings.ErrorParseFailure);
          return false;
        }
        if (get == null)
        {
          ShowErrorDialog(Strings.ErrorParseFailure);
          return false;
        }
        result = DownloadPage(client, get,
                 GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 200 : 100,
                 GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 300 : 150,
                 page, error);
        if (result == null)
        {
          return true;        
        }
        else if (result == false)
        {
          ShowErrorDialog(error.toString());
          return false;
        }
        
        // Create the link
        link = GetGroup("url=\"(.+?)\"", page.toString());
        if (link == null || link.startsWith("http") == false)
        {
          ShowErrorDialog(Strings.ErrorParseFailure);
          return false;
        }
      }
      else
      {
        String download = GetGroup("\\?download=(.+?)['\"]", string);
        if (download != null)
        {
          // Download the url details
          if (url.toLowerCase().contains("putlocker.com"))
            get = CreateGetRequest("http://www.putlocker.com/get_file.php?download=" + 
                  download, false);
          else if (url.toLowerCase().contains("www.sockshare.com"))
            get = CreateGetRequest("http://www.sockshare.com/get_file.php?download=" +
                  download, false);
          else
          {
            ShowErrorDialog(Strings.ErrorParseFailure);
            return false;
          }
          if (get == null)
          {
            ShowErrorDialog(Strings.ErrorParseFailure);
            return false;
          }
          StringBuilder redirect = new StringBuilder();
          result = DownloadPage(client, get,
                   GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 200 : 100,
                   GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 300 : 150,
                   page, redirect, error);
          if (result == null)
          {
            return true;            
          }
          else if (result == false)
          {
            ShowErrorDialog(error.toString());
            return false;
          }          
          
          // Create the link
          link = redirect.toString();
        }
        else
        {
          ShowErrorDialog(Strings.ErrorParseFailure);
          return false;
        }
      }
      
      // Get the filename
      String filename = null;
      String[] parts = link.split("/");
      for (int i = parts.length - 1; i >= 0; --i)
      {
        if (parts[i].length() > 4 && 
            parts[i].substring(parts[i].length() - 4).matches("\\.[0-9a-zA-Z]{3}"))
        {
          filename = parts[i];
          break;
        }
      }
      if (filename == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      
      // Get the save location
      File saveLocation = null;
      if (GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_SAVE)
      {
        // Open the save dialog
        saveLocation = ShowSaveDialog(filename);
        
        // Check if it was cancelled
        if (saveLocation == null)
          return true;
      }      
      
      // Check if there's an activity available to open this file
      Intent intent = null;
      if (GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN)
      {
        // Create the intent
        if (GetOpenMode() == OpenActivity.Mode.STREAM_OPEN)
          intent = CreateIntent(Uri.parse(link), filename);
        else
          intent = CreateIntent(Uri.parse(saveLocation.getAbsolutePath()));

        // Check if the intent creation got cancelled
        if (intent == null)
          return true;
        
        // Check if there's an activity that can open this intent
        if (CanOpenIntent(intent, error) == false)
        {
          ShowErrorDialog(error.toString());
          return false;
        }
      }
      
      // Wait
      Boolean waitResult = Wait(0,
                           GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 300 : 150,
                           GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 1000 : 500,
                           error);
      if (waitResult == null)
      {
        return true;
      }
      else if (waitResult == false)
      {
        ShowErrorDialog(error.toString());
        return false;
      }
      
      // Download
      if (GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_SAVE)
      {
        get = CreateGetRequest(link);
        result = DownloadFile(client, get, saveLocation, 500, 1000, error);
        if (result == null)
        {
          return true;
        }
        else if (result == false)
        {
          ShowErrorDialog(error.toString());
          return false;
        }
      }
      
      // Destroy the http client
      DestroyHttpClient(client);
      
      // Open the intent
      if (GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN)
      {
        Boolean openResult = OpenIntent(intent, error);
        if (openResult == null)
        {
          return true;
        }
        else if (openResult == false)
        {
          ShowErrorDialog(error.toString());
          return false;          
        }
      }
      
      // Show download complete dialog
      if (GetOpenMode() == OpenActivity.Mode.DOWNLOAD_SAVE)
      {
        ShowErrorDialog(Strings.DialogComplete);
        return false;
      }      

      return true;
    }
  }
  
  // Constructor
  public PutLockerAndSockShare(OpenFragment fragment, TextView topText, ProgressBar progressBar,
                   TextView bottomText, AdLayout adLayout, URL url)
  {
    // Call base class constructor
    super(fragment, topText, progressBar, bottomText, adLayout, url);
  }  

  // Method called to open this source
  public void Open(OpenActivity.Mode mode)
  {
    // Call base class method
    super.Open(mode);
  
    // Create and start the open task
    OpenTask task = new OpenTask();
    task.execute(GetURL());
    
    // Save the task
    SaveOpenTask(task);
  }
}