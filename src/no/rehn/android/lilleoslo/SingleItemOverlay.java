package no.rehn.android.lilleoslo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

class SingleItemOverlay extends ItemizedOverlay<OverlayItem> {
    final OverlayItem mItem;
    final Context mContext;

    public SingleItemOverlay(Context context, Drawable marker, OverlayItem item) {
        super(boundCenterBottom(marker));
        mItem = item;
        mContext = context;
        populate();
    }

    @Override
    protected boolean onTap(int i) {
        Toast.makeText(mContext, mItem.getTitle(), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    protected OverlayItem createItem(int i) {
        return mItem;
    }

    @Override
    public int size() {
        return 1;
    }
}