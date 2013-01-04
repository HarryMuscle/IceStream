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
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import info.icefilms.icestream.R;
import java.net.URL;

public class HeadingItem extends Item
{
  // Variable
  private boolean mSelected;

  // Constructors
  protected HeadingItem(URL url)
  {
    this(null, url, false);
  }
  protected HeadingItem(String name, URL url, boolean selected)
  {
    super(name, url);
    mSelected = selected;
  }

  // Constructor used by the parcelable functionality of this class
  protected HeadingItem(Parcel in)
  {
    super(in);
    boolean[] array = new boolean[1];
    in.readBooleanArray(array);
    mSelected = array[0];
  }

  // Variable used by the parcelable functionality of this class
  public static final Parcelable.Creator<HeadingItem> CREATOR =
      new Parcelable.Creator<HeadingItem>()
      {
        public HeadingItem createFromParcel(Parcel in)
        {
          return new HeadingItem(in);
        }

        public HeadingItem[] newArray(int size)
        {
          return new HeadingItem[size];
        }
      };

  // Method used by the parcelable functionality of this class
  public void writeToParcel(Parcel dest, int flags)
  {
    super.writeToParcel(dest, flags);
    dest.writeBooleanArray(new boolean[] { mSelected });
  }

  // Getter method
  protected boolean IsSelected() { return mSelected; }

  // Method called to get a view object for this item
  @Override
  protected View GetView(Context context)
  {
    // Create the linear layout
    LinearLayout linearLayout = new LinearLayout(context);

    // Use scaled pixels for the padding since the height of the horizontal scroll list
    //    that contains these items is set in scaled pixels
    int sp5 = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 5,
              context.getResources().getDisplayMetrics()) + 0.5f);
    linearLayout.setPadding(sp5, sp5, sp5, sp5);

    // Add the text view
    TextView textView = new TextView(context);
    textView.setText(GetName());
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    textView.setTypeface(Typeface.DEFAULT_BOLD);
    if (mSelected == true)
      textView.setTextColor(context.getResources().getColor(R.color.heading_item_color_selected));
    else
      textView.setTextColor(context.getResources().getColor(R.color.heading_item_color));
    linearLayout.addView(textView);

    return linearLayout;
  }
}