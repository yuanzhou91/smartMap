package util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Helper {
	public static byte[] bitMapToByteArray(Bitmap thumbnailBitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] thumbnailBitmapBytes = stream.toByteArray();
		return thumbnailBitmapBytes;
	}
	
	public static Bitmap byteArrayToBitmap(byte[] bytes) {
		Bitmap bitmap = null;
		if (bytes != null) {
            try {
            	bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } catch (Exception e) {
                Log.e("TAG", "Exception", e);
            }
        } else {
            Log.e("TAG", "IMAGE NOT FOUND");
        }
		return bitmap;
	}
	
}
