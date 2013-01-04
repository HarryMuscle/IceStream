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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import java.net.URL;

public abstract class Item implements Parcelable
{
  // Variables
  private String mName;
  private URL mURL;

  // Constructor
  protected Item(String name, URL url)
  {
    mName = name;
    mURL = url;
  }

  // Constructor used by the parcelable functionality of this class
  protected Item(Parcel in)
  {
    byte exists = in.readByte();
    if (exists == (byte)1)
      mName = in.readString();
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
    if (mName == null)
    {
      dest.writeByte((byte)0);
    }
    else
    {
      dest.writeByte((byte)1);
      dest.writeString(mName);
    }
    dest.writeValue(mURL);
  }

  // Getter methods
  protected String GetName() { return mName; }
  protected URL GetURL() { return mURL; }

  // Method called to get a view object for this item
  protected abstract View GetView(Context context);
}