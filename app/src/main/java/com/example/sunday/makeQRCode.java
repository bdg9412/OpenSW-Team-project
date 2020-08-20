package com.example.sunday;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class makeQRCode extends AppCompatActivity {

    ImageView qrImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.make_qrcode);
        //뷰의 주소값을 얻어온다.
        qrImage = findViewById(R.id.imageView);
        //QR 코드 생성 후 보여주기
        CreateQR(qrImage, user_texting.all);
    }
    //QR코드를 생성하는 함수
    public void CreateQR(ImageView img, String text) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            //한글 호환 맞춰주기
            user_texting.all = new String(user_texting.all.getBytes("UTF-8"), "ISO-8859-1");
            //QR코드 형성
            BitMatrix bitMatrix = multiFormatWriter.encode(user_texting.all, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrImage.setImageBitmap(bitmap);
        } catch (Exception e) {
        }
    }
}