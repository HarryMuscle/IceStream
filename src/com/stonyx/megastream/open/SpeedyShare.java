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
import org.apache.http.impl.client.DefaultHttpClient;

public class SpeedyShare extends Source
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
      HttpGet get = CreateGetRequest(params[0].toExternalForm());
      if (get == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }      
      Boolean result = DownloadPage(client, get, 0,
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
      String string = page.toString();
      
      // Check if the link is available
      if (string.contains("You are already downloading another file"))
      {
        ShowErrorDialog(Strings.ErrorNotAllowed);
        return false;
      }      
      if (string.contains("File not found"))
      {
        ShowErrorDialog(Strings.ErrorNotAvailable);
        return false;
      }
      
      // Create the link
      String link = GetGroup("<a class=downloadfilename href=['\"](.+?)['\"]>",
                    page.toString());
      if (link == null)
      {
        ShowErrorDialog(Strings.ErrorParseFailure);
        return false;
      }
      link = "http://speedy.sh" + link;
      
      // Get the save location
      File saveLocation = null;
      if (GetOpenMode() == OpenActivity.Mode.DOWNLOAD_OPEN ||
          GetOpenMode() == OpenActivity.Mode.DOWNLOAD_SAVE)
      {
        // Open the save dialog
        saveLocation = ShowSaveDialog(GetFilenameFromLink(link));
        
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
  }
  
  // Constructor
  public SpeedyShare(OpenFragment fragment, TextView topText, ProgressBar progressBar,
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