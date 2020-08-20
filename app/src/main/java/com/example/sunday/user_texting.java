package com.example.sunday;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;


public class user_texting extends Activity {
    //뷰의 주소값을 담을 참조변수 선언
    EditText name, phone, tempr;
    CheckBox sym, aboard;
    Button enter;
    //사용자 정보를 저장할 변수
    public static String Name, Phone, Tempr, Sym, Aboard, time;
    public static String all;

    @Override
    public void onCreate(Bundle saveIncstanceState) {
        super.onCreate(saveIncstanceState);
        setContentView(R.layout.user_texting);

        //뷰의 주소 값을 얻어온다.
        name = (EditText) findViewById(R.id.editTextPersonName);
        phone = (EditText) findViewById(R.id.editTextPhone);
        tempr = (EditText) findViewById(R.id.editTextTempr);
        sym = (CheckBox) findViewById(R.id.symCheck);
        aboard = (CheckBox) findViewById(R.id.aboardCheck);
        enter = (Button) findViewById(R.id.enterQR);
    }

    public void btnMethod1(View view) {
        //now라는 문자열 변수에 시간을 저장한다.
        SimpleDateFormat format = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분");
        Date date = new Date();
        time = format.format(date);
        //사용자가 입력한 정보를 저장한다.
        Name = name.getText().toString();
        Phone = phone.getText().toString();
        Tempr = tempr.getText().toString();
        //체크박스 체크 여부를 저장한다.
        boolean a1 = sym.isChecked();
        boolean a2 = aboard.isChecked();

        //모든 정보가 잘 입력되면,
        if ((!Name.isEmpty()) && (Phone.length() == 11) && (!Tempr.isEmpty()) && (a1==true) && (a2 == true)) {
            //여기서부터 이제 QR코드 소환!
            //all은 QR코드에 삽입될 데이터.
            all = time + "\n이름: " + Name + "\n전화번호: " + Phone + "\n체온: " + Tempr + "\n" + Sym + "\n" + Aboard;
            Intent intent = new Intent(user_texting.this, makeQRCode.class);
            startActivity(intent);
        } else {
            //정보가 잘 입력되지 않으면 경고메세지를 띄우고 화면을 넘기지 않는다.
            user_texting.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(user_texting.this, "입력하신 정보를 다시 확인해주십시오.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}