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
import info.icefilms.icestream.R;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SourceLocation extends Location
{
  // Variables
  private String mCookie;
  private String mSecret;
  private String mToken;
  private String mIdent;

  // Constructor
  protected SourceLocation(String cookie, String secret,
                           String token, String ident)
  {
    super((URL)null);
    mCookie = cookie;
    mSecret = secret;
    mToken = token;
    mIdent = ident;
  }

  // Constructor used by the parcelable functionality of this class
  protected SourceLocation(Parcel in)
  {
    super(in);
    mCookie = in.readString();
    mSecret = in.readString();
    mToken = in.readString();
    mIdent = in.readString();
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<SourceLocation> CREATOR =
      new Parcelable.Creator<SourceLocation>()
      {
        public SourceLocation createFromParcel(Parcel in)
        {
          return new SourceLocation(in);
        }

        public SourceLocation[] newArray(int size)
        {
          return new SourceLocation[size];
        }
      };

  // Method used by the parcelable functionality of this class
  public void writeToParcel(Parcel dest, int flags)
  {
    super.writeToParcel(dest, flags);
    dest.writeString(mCookie);
    dest.writeString(mSecret);
    dest.writeString(mToken);
    dest.writeString(mIdent);
  }

  // Method called to get the actual URL for this source
  // Returns null if the Callback IsCancelled method return true or if an error
  //    occurred otherwise returns a URL for this source
  public URL GetURL(Callback callback)
  {
    // Create the mouse and seconds fields
    String mouse = new Integer((100 + (int)(Math.random() * (200) + 0.5)) * -1).toString();
    String seconds = new Integer(5 + (int)(Math.random() * (45) + 0.5)).toString();

    // Encode the data
    String data = null;
    try
    {
      data = "iqs=&url=&cap=&sec=" + URLEncoder.encode(mSecret, "UTF-8") +
             "&t=" + URLEncoder.encode(mToken, "UTF-8") +
             "&m=" + URLEncoder.encode(mouse, "UTF-8") +
             "&s=" + URLEncoder.encode(seconds, "UTF-8") +
             "&id=" + URLEncoder.encode(mIdent, "UTF-8");
    }
    catch (UnsupportedEncodingException exception) {}

    // Get the source URL
    URL url;
    try
    {
      // Get the page with the URL information
      String page = DownloadPage(new URL(GetIceFilmsURL(),
                    "/membersonly/components/com_iceplayer/video.phpAjaxResp.php"),
                    mCookie, data, null, callback);
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

      // Create the URL
      url = new URL(GetGroup("url=(http[^&]+)", URLDecoder.decode(page, "UTF-8")));
    }
    catch (MalformedURLException exception)
    {
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_parse_error);
      }
      return null;
    }
    catch (UnsupportedEncodingException exception)
    {
      if (callback.GetErrorBoolean() == false)
      {
        callback.SetErrorBoolean(true);
        callback.SetErrorStringID(R.string.browse_parse_error);
      }
      return null;
    }

    return url;
  }

  // Method called to get a list of list items from the passed page
  // Returns null if the Callback IsCancelled method returns true or
  //    returns an empty list if no items were found otherwise
  //    returns a list of list items however this method should
  //    never be called for a SourceLocation object
  @Override
  protected ArrayList<Item> OnGetListItems(String page, Callback callback)
  {
    return null;
  }
}