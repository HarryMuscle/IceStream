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

public class ShareBees extends Source
{
  // Define class used to open this source
  private class OpenTask extends Source.OpenTask
  {
    // Overridden method that runs in a separate thread
    @Override
    protected Boolean doInBackground(URL... params)
    {
      // Create the string builders
      StringBuilder page = new StringBuilder();
      StringBuilder error = new StringBuilder();
      
      // Create the http client
      DefaultHttpClient client = CreateHttpClient();
      
      // Download the page
      String url = params[0].toExternalForm();
      HttpGet get = CreateGetRequest(url);
      if (get == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }      
      Boolean result = DownloadPage(client, get, 0,
                       GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 150 : 75,
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

      // Convert the builder into a string
      String string = page.toString();
      
      // Check if the link is available
      if (string.contains("File Not Found"))
      {
        ShowErrorDialog(Strings.ErrorNotAvailable);
        return false;
      }
      
      // Find the post data
      String op = GetGroup("<input type=\"hidden\" name=\"op\" value=\"(.+?)\">", string);
      String id = GetGroup("<input type=\"hidden\" name=\"id\" value=\"(.+?)\">", string);
      String fname = GetGroup("<input type=\"hidden\" name=\"fname\" value=\"(.+?)\">", string);
      if (op == null || id == null || fname == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      
      // Send the post data
      HttpPost post = CreatePostRequest(url,
                      new String[] { "op", op, "id", id, "fname", fname,
                      "method_free", "Free Download" });
      if (post == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      result = DownloadPage(client, post,
               GetOpenMode() == OpenActivity.Mode.STREAM_OPEN ? 150 : 75,
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

      // Convert the builder into a string
      string = page.toString();

      // Check for multiple concurrent downloads
      if (string.contains("seconds till next download"))
      {
        ShowErrorDialog(Strings.ErrorNotAllowed);
        return false;
      }
      
      // Check if the file is too large for the free download method
      if (string.contains("You can download files up to"))
      {
        ShowErrorDialog(Strings.ErrorTooLarge);
        return false;
      }
      
      // Get the variables needed to unpack the JavaScript we need to see
      String[] groups = GetGroups("(?s)id=\"player_code\".+?\\}\\('(.+?)',(\\d+?),(\\d+?),'(.+?)'\\.split",
                        string);
      if (groups == null || groups.length < 3)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }

      // Unpack the JavaScript
      string = UnpackDeanEdwardsPackedJS(groups[0], Integer.parseInt(groups[1]),
               Integer.parseInt(groups[2]), groups[3].split("\\|"));
 
      // Create the link
      String link = GetGroup("name=\"src\".*?value=\"(.+?)\"", string);
      if (link == null || link.startsWith("http") == false)
      {
        // Try alternate search string for creating the link
        link = GetGroup("file.+?(http.+?)\\\\", string);
        if (link == null || link.startsWith("http") == false)
        {
          ShowErrorDialog(Strings.ErrorParseFailure);
          return false;
        }
      }
      
      // Get the save location
      File saveLocation = null;
      if (GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_SAVE)
      {
        // Open the save dialog
        saveLocation = ShowSaveDialog(fname);
        
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
          intent = CreateIntent(Uri.parse(link));
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
    
    // Method called to unpack Dean Edwards packed JavaScript which begins with the following
    //    unpacking JavaScript algorithm eval(function(p,a,c,k,e,d){while(c--)if(k[c])
    //    p=p.replace(new RegExp('\\b'+c.toString(a)+'\\b','g'),k[c]);return p} followed by
    //    the data, numerical base, key count, and keys
    String UnpackDeanEdwardsPackedJS(String data, int base, int count, String[] keys)
    {
      while(count-- != 0)
      {
        if (keys.length > count && keys[count].length() > 0)
          data = data.replaceAll("\\b" + Integer.toString(count, base) + "\\b", keys[count]);
      }  
      
      return data;
    }
  }
  
  // Constructor
  public ShareBees(OpenFragment fragment, TextView topText, ProgressBar progressBar,
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