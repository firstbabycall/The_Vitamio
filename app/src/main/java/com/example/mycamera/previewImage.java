package com.example.mycamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class previewImage extends AppCompatImageView {
    private Paint paint;
    private Path path;
    public previewImage(@NonNull Context context) {
        super(context);
    }

    public previewImage(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        paint = new Paint();

        paint.setAntiAlias(true);

        paint.setFilterBitmap(true);

        paint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas cns) {
        float h = getMeasuredHeight()- 3.0f;

        float w = getMeasuredWidth()- 3.0f;

        if (path == null) {
            path = new Path();

            path.addCircle(

                    w/2.0f

                    , h/2.0f

                    , (float) Math.min(w/2.0f, (h / 2.0))-18

                    , Path.Direction.CCW);

            path.close();

        }

        cns.drawCircle(w/2.0f, h/2.0f, Math.min(w/2.0f, h / 2.0f)-12, paint);

        int saveCount = cns.getSaveCount();

        cns.save();

        cns.clipPath(path, Region.Op.INTERSECT);

        cns.drawColor(Color.WHITE);

        super.onDraw(cns);

        cns.restoreToCount(saveCount);
    }


}
