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
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.icefilms.icestream.R;

public class InfoItem extends ListItem
{
  // Variable
  private Bitmap mImage;

  // Constructor
  protected InfoItem(String name, Bitmap image)
  {
    super(name, null, -1);
    mImage = image;
  }

  // Constructor used by the parcelable functionality of this class
  protected InfoItem(Parcel in)
  {
    super(in);
    byte exists = in.readByte();
    if (exists == (byte)1)
      mImage = (Bitmap)in.readParcelable(null);
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<InfoItem> CREATOR =
      new Parcelable.Creator<InfoItem>()
      {
        public InfoItem createFromParcel(Parcel in)
        {
          return new InfoItem(in);
        }

        public InfoItem[] newArray(int size)
        {
          return new InfoItem[size];
        }
      };

  // Method used by the parcelable functionality of this class
  public void writeToParcel(Parcel dest, int flags)
  {
    super.writeToParcel(dest, flags);
    if (mImage == null)
    {
      dest.writeByte((byte)0);
    }
    else
    {
      dest.writeByte((byte)1);
      dest.writeParcelable(mImage, flags);
    }
  }

  // Method called to get a view object for this item
  @Override
  protected View GetView(Context context)
  {
    // Calculate things
    float density = context.getResources().getDisplayMetrics().density;
    int dp5 = (int)(5f * density + 0.5f);
    int dp10 = (int)(10f * density + 0.5f);
    int dp150 = (int)(150f * density + 0.5f);
    int dp200 = (int)(200f * density + 0.5f);
    int dp225 = (int)(225f * density + 0.5f);

    // Check which type of layout we are creating
    boolean sideBySide = PreferenceManager.getDefaultSharedPreferences(context).
                         getBoolean("SideBySide", true);

    // Create the linear layout
    LinearLayout linearLayout = new LinearLayout(context);
    if (sideBySide == true)
      linearLayout.setOrientation(LinearLayout.HORIZONTAL);
    else
      linearLayout.setOrientation(LinearLayout.VERTICAL);
    linearLayout.setPadding(dp5, dp5, dp5, dp5);

    // Add the image view
    if (mImage != null)
    {
      if (sideBySide == true)
      {
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams imageViewLP =
          new LinearLayout.LayoutParams(dp150, dp225);
        if (GetName() != null)
          imageViewLP.setMargins(0, 0, dp10, 0);
        imageView.setImageBitmap(mImage);
        linearLayout.addView(imageView, imageViewLP);
      }
      else
      {
        ImageView imageView = new ImageView(context);
        LinearLayout.LayoutParams imageViewLP =
            new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp200);
        imageViewLP.gravity = Gravity.CENTER_HORIZONTAL;
        if (GetName() != null)
          imageViewLP.setMargins(0, 0, 0, dp5);
        imageView.setImageBitmap(mImage);
        linearLayout.addView(imageView, imageViewLP);
      }
    }

    // Add the text view
    if (GetName() != null)
    {
      // Add the text view
      TextView textView = new TextView(context);
      LinearLayout.LayoutParams textViewLP =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
          LayoutParams.WRAP_CONTENT);
      textView.setText(GetName());
      textView.setTextColor(context.getResources().getColor(R.color.info_item_color));
      linearLayout.addView(textView, textViewLP);
    }

    return linearLayout;
  }
}