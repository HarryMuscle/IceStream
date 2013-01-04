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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.stonyx.ads.AdLayout;
import java.io.File;
import java.net.URL;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

// This class uses the RapidShare API as described at
//    http://images.rapidshare.com/apidoc.txt

public class RapidShare extends Source
{
  // Define class used to open this source
  private class OpenTask extends Source.OpenTask
  {
    // Overridden method that runs in a separate thread
    @Override
    protected Boolean doInBackground(URL... params)
    {
      // Create some of the needed variables
      HttpPost post;
      Boolean result;
      String string;
      StringBuilder page = new StringBuilder();
      StringBuilder redirect = new StringBuilder();
      StringBuilder error = new StringBuilder();

      // Split the URL into parts
      String[] parts = params[0].getPath().split("/");
      
      // Make sure we have at least 2 parts
      if (parts.length < 2)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      
      // Get the file ID and filename
      String id = parts[parts.length - 2];
      String filename = parts[parts.length - 1];

      // Get the username and password for RapidShare
      String username = PreferenceManager.getDefaultSharedPreferences(GetFragment().
                        GetActivity()).getString("RapidShareUsername", "");
      String password = PreferenceManager.getDefaultSharedPreferences(GetFragment().
                        GetActivity()).getString("RapidSharePassword", "");
      
      // Create the URL for communicating with the RapidShare API
      String url;
      if (username.length() == 0 || password.length() == 0)
        url = "http://api.rapidshare.com/cgi-bin/rsapi.cgi";
      else
        url = "https://api.rapidshare.com/cgi-bin/rsapi.cgi";

      // Create the http client
      DefaultHttpClient client = CreateHttpClient(false);
      
      // Download the file details
      do
      {
        post = CreatePostRequest(url, new String[] { "sub", "checkfiles", "files",
               id, "filenames", filename }, false);
        result = DownloadPage(client, post, 0, 
                 GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 100 : 50,
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
        
        // Check for redirect (needs to be handled manually since we are redirecting
        //    a post request to a post request which goes against the http protocol
        //    and isn't handled automatically by the http client)
        string = redirect.toString();
        if (string.length() > 0)
          url = string;
      }
      while (string.length() > 0); 
      
      // Check for errors
      string = page.toString();
      if (string.startsWith("ERROR: "))
      {
        int end = string.lastIndexOf('\n');
        if (end == -1)
          end = string.length();
        ShowErrorDialog(Strings.RapidShareError +
            string.substring(string.indexOf(' '), end));
        return false;
      }

      // Split the downloaded page into parts
      parts = string.split(",");
      
      // Make sure we have at least 5 parts
      if (parts.length < 5)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      
      // Get the status
      String status = parts[4];
            
      // Check if the file is temporarily unavailable
      if (status.equals("3"))
      {
        ShowErrorDialog(Strings.ErrorTemporarilyNotAvailable);
        return false;
      }
      
      // Check if the file is unavailable
      if (status.equals("0") || status.equals("4"))
      {
        ShowErrorDialog(Strings.ErrorNotAvailable);
        return false;
      }

      // Get the login cookie if we have login information
      String cookie = null;
      if (username.length() != 0 && password.length() != 0)
      {
        do
        {
          // Download the login details
          post = CreatePostRequest(url, new String[] { "sub", "getaccountdetails",
              "login", username, "password", password, "withcookie", "1" }, false);
          result = DownloadPage(client, post, 
                   GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 100 : 50,
                   GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 200 : 100,
                   page, redirect, error);
          if (result == null)
          {
            return true;
          }
          else if (result == false)
          {
            ShowWarningToast(Strings.WarningLoginFailure);
          }
          
          // Check for redirect (needs to be handled manually since we are redirecting
          //    a post request to a post request which goes against the http protocol
          //    and isn't handled automatically by the http client)
          string = redirect.toString();
          if (string.length() > 0)
            url = string;
        }
        while (string.length() > 0);
        
        // Check for error
        string = page.toString();
        if (string.startsWith("ERROR: "))
        {
          ShowWarningToast(Strings.WarningLoginFailure);
        }
        else
        {        
          // Split the downloaded page into parts
          parts = string.split("\n");
          
          // Find the cookie
          for (int i = 0; i < parts.length; ++i)
          {
            if (parts[i].startsWith("cookie=") && parts[i].length() > 7)
            {
              cookie = parts[i].substring(7);
              break;
            }
          }
          
          // Free memory
          parts = null;
          
          // Check if we didn't find the cookie
          if (cookie == null)
          {
            // Show warning
            ShowWarningToast(Strings.WarningLoginFailure);
            
            // Change the URL for communicating with the RapidShare API
            url = "http://api.rapidshare.com/cgi-bin/rsapi.cgi";
          }
        }
      }
      
      // Download the download authentication code
      do
      {
        if (cookie == null)
          post = CreatePostRequest(url, new String[] { "sub", "download", "fileid", id,
                 "filename", filename, "try", "1" }, false);
        else
          post = CreatePostRequest(url, new String[] { "sub", "download", "fileid", id,
                 "filename", filename, "try", "1", "cookie", cookie }, false);
        result = DownloadPage(client, post,
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
      
        // Check for redirect (needs to be handled manually since we are redirecting
        //    a post request to a post request which goes against the http protocol
        //    and isn't handled automatically by the http client)
        string = redirect.toString();
        if (string.length() > 0)
          url = string;
      }
      while (string.length() > 0);        
      
      // Check for errors
      string = page.toString();
      if (string.startsWith("ERROR: "))
      {
        int end = string.lastIndexOf('\n');
        if (end == -1)
          end = string.length();
        ShowErrorDialog(Strings.RapidShareError + 
            string.substring(string.indexOf(' '), end));
        return false;
      }

      // Set the wait start
      long waitStart = SystemClock.uptimeMillis();

      // Clean and split the downloaded page into parts
      string = string.replaceFirst("DL:", "");
      parts = string.split(",");
      
      // Make sure we have at least 3 parts
      if (parts.length < 3)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      
      // Get the server name, authentication code, and wait time and free memory
      String server = parts[0];
      String code = parts[1];
      String count = parts[2];
      parts = null;
      
      // Create the link
      String link;
      if (cookie == null)
        link = "http://";
      else
        link = "https://";
      link += server + "/cgi-bin/rsapi.cgi?" + URLEncodeData(new String[] { "sub",
              "download", "fileid", id, "filename", filename });
      if (cookie == null)
        link += "&" + URLEncodeData(new String[] { "dlauth", code });
      else
        link += "&" + URLEncodeData(new String[] { "cookie", cookie });
      
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
          intent = CreateIntent(Uri.parse(saveLocation.getAbsolutePath()), filename);

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
      
      // Figure out how much time already elapsed taking into account wrap around of the clock
      long timeWaited = SystemClock.uptimeMillis() - waitStart;
      if (timeWaited < 0)
        timeWaited = Long.MAX_VALUE - timeWaited;
      
      // Figure out how long we need to wait
      int wait = 0;
      try
      {
        wait = Integer.parseInt(count);
      }
      catch (NumberFormatException exception)
      {
        wait = 0;
      }
      wait -= timeWaited / 1000;
      if (wait < 0)
        wait = 0;
      
      // Wait
      Boolean waitResult = Wait(wait,
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
        HttpGet get = CreateGetRequest(link);
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
  public RapidShare(OpenFragment fragment, TextView topText, ProgressBar progressBar,
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