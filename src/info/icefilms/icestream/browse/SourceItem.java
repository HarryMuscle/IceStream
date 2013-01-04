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

public class SourceItem extends ListItem
{
  // Variables
  private String mCookie;
  private String mSecret;
  private String mToken;
  private String mIdent;

  // Constructor
  protected SourceItem(String name, String cookie, String secret,
                       String token, String ident)
  {
    super(name, null, -1);
    mCookie = cookie;
    mSecret = secret;
    mToken = token;
    mIdent = ident;
  }

  // Constructor used by the parcelable functionality of this class
  protected SourceItem(Parcel in)
  {
    super(in);
    mCookie = in.readString();
    mSecret = in.readString();
    mToken = in.readString();
    mIdent = in.readString();
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<SourceItem> CREATOR =
      new Parcelable.Creator<SourceItem>()
      {
        public SourceItem createFromParcel(Parcel in)
        {
          return new SourceItem(in);
        }

        public SourceItem[] newArray(int size)
        {
          return new SourceItem[size];
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

  // Getter methods
  protected String GetCookie() { return mCookie; }
  protected String GetSecret() { return mSecret; }
  protected String GetToken() { return mToken; }
  protected String GetIdent() { return mIdent; }
}