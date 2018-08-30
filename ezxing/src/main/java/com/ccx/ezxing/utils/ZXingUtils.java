package com.ccx.ezxing.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Display;
import android.view.WindowManager;

import com.ccx.ezxing.DecodeType;
import com.ccx.ezxing.decode.DecodeFormatManager;
import com.ccx.ezxing.decode.DecodeResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import static android.content.Context.WINDOW_SERVICE;

public class ZXingUtils {
    private ZXingUtils() {

    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public static Bitmap encodeAsBitmap(Context context, String contents) {
        WindowManager manager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        assert manager != null;
        Display display     = manager.getDefaultDisplay();
        Point   displaySize = new Point();
        display.getSize(displaySize);
        int screenWdith      = displaySize.x;
        int screenHeight     = displaySize.y;
        int smallerDimension = screenWdith < screenHeight ? screenWdith : screenHeight;
        smallerDimension = smallerDimension * 7 / 8;
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints    = null;
        String                      encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        BitMatrix result = null;
        try {
            BarcodeFormat format = BarcodeFormat.QR_CODE;
            result = new MultiFormatWriter().encode(contents, format, smallerDimension, smallerDimension, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            iae.printStackTrace();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        assert result != null;
        int   width  = result.getWidth();
        int   height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    public static DecodeType regexText(String text) {
        String email  = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";
        String URL    = "[a-zA-z]+://[^\\s]*";
        String number = "^[0-9]*$";
        if (regex(email, text)) {
            return DecodeType.EMAIL;
        } else if (regex(URL, text)) {
            return DecodeType.URL;
        } else if (regex(number, text)) {
            return DecodeType.NUMBER;
        } else {
            return DecodeType.TEXT;
        }

    }

    private static boolean regex(String regex, String text) {
        return Pattern.matches(regex, text);
    }


    public static DecodeResult encodeImage(Bitmap bitmap) {
        long start = System.currentTimeMillis();
        bitmap = getSmallerBitmap(bitmap);
        int   width  = bitmap.getWidth();
        int   height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // 查看源码发现还有这个子类
        RGBLuminanceSource source            = new RGBLuminanceSource(width, height, pixels);
        Result             result            = null;
        MultiFormatReader  multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(getHints());

        BinaryBitmap neoBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            result = multiFormatReader.decodeWithState(neoBitmap);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
        long         end          = System.currentTimeMillis();
        DecodeResult decodeResult = new DecodeResult();
        if (result != null) {
            decodeResult.rawResult = result.getText();
        } else {
            decodeResult.rawResult = "解析失败";
        }
        decodeResult.handingTime = (end - start + 0f) / 1000 + "";

        return decodeResult;
    }


    private static Bitmap getSmallerBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() / 160000;
        if (size <= 1) {
            return bitmap; // 如果小于
        } else {
            Matrix matrix = new Matrix();
            matrix.postScale((float) (1 / Math.sqrt(size)), (float) (1 / Math.sqrt(size)));
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    }


    private static Map<DecodeHintType, Vector<BarcodeFormat>> getHints() {
        EnumMap<DecodeHintType, Vector<BarcodeFormat>> hints         = new EnumMap<>(DecodeHintType.class);
        Vector<BarcodeFormat>                          decodeFormats = new Vector<>(EnumSet.noneOf(BarcodeFormat.class));
        decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        return hints;
    }
}
