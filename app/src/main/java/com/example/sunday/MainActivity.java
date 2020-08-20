package com.example.sunday;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    //카메라, 저장공간 권한
    String[] permission_list = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private CameraBridgeViewBase mOpenCvCameraView;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    boolean startYolo = false;
    boolean firstTimeYolo = false;
    Net tinyYolo;
    private Button capture;
    private int intConf;
    //카메라 촬영용 flag
    //flag_camera가 1일 경우 화면 캡쳐, 0일 경우 화면캡쳐 중지
    private int flag_camera = 0;
    //flag_rotate가 1일 경우 가로 모드, 0일 경우 가로모드
    private int flag_rotate = 0;

    //참고 https://devfarming.tistory.com/3
    //참고 https://itmining.tistory.com/16
    //MainThread가 아닌 다른 Thread에서 UI를 변경하려 하면 오류가 발생
    //그럼으로 Handler를 이용하여 두개의 서로 다른 쓰레드를 연결할 수 있도록 함
    //안드로이드에선 메인스레드와 서브스레드 간에 Handler를 통해 메시지를 전달하여 메시지 큐에 저장하는 방식의 통신을 사용
    //Handler는 해당 Handler를 호출한 스레드의 MessageQueue와 Looper에 자동 연결된다.
    //임시로 MainThread를 사용할 수 있도록 해준다.
    /*
    원래 있던 코드 주석 처리
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            //이곳에 변경할 UI행동을 넣어줌으로써 동작을 한다.

        }
    }; */


    public void YOLO(View Button) {

        //YOLO 버튼을 누르면 startYolo가 true가 되어 detection 진행
        //버튼을 누르면 sensorManager등록, 이때 Listener로 this가 아닌 선언한 변수 mAccLis를 넣어줘야 에러 발생 없다.
        //Yolov3와 무관하게 실험값을 확인하기 위해 다른 버튼으로 작동하도록 만들기

        if (startYolo == false) {

            startYolo = true;

            if (firstTimeYolo == false) {

                firstTimeYolo = true;
                //opencv DNN.readNetFromDarknet을 사용하기 위해 string 인자를 두개(cfg,weight) 넘겨줘야 합니다.
                //getpath라는 임의의 함수를 이용하여 filepath를 string으로 저장합니다.
                String tinyYoloCfg = getPath("yolov3-dongkeun.cfg",this) ;
                String tinyYoloWeights = getPath("yolov3-dongkeun_last.weights",this);



                //opencv에서 제공하는 Dnn모델(Deep Neural Network)을 이용
                tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);
                System.out.println(tinyYolo.getLayerNames());
            }

        } else {

            startYolo = false;
        }

    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    //가로 세로 변환시 해당 모드를 팝업 메시지로 띄워주고 flag변수에 값을 할당한다
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "세로모드", Toast.LENGTH_SHORT).show();
            flag_rotate=0;
        }

        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "가로모드", Toast.LENGTH_SHORT).show();
            flag_rotate=1;
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        //참고 https://recipes4dev.tistory.com/61
        //유저에게 입력을 받아 이를 string으로 변환하여 저장
        //label 이름 저장을 위해 textview를 활용
        /* QR코드에 담을 정보를 구체화함에 따라 이 부분 삭제합니다.
        editText2 = findViewById(R.id.editText2);
        enter2 = findViewById(R.id.enter2);
        enter2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //enter2 버튼(label) 클릭시 editText2(label 이름)의 값을 string으로 저장
                strText = editText2.getText().toString();
                System.out.println(strText);
            }
        });
        */

        //이미지 저장
        capture = findViewById(R.id.capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag_camera = 1;
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        if (startYolo == true) {

            String tinyYoloCfg = getPath("yolov3-dongkeun.cfg",this) ;
            String tinyYoloWeights = getPath("yolov3-dongkeun_last.weights",this);


            tinyYolo = Dnn.readNetFromDarknet(tinyYoloCfg, tinyYoloWeights);

        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //inputframe중 rgba format을 프레임변수에 할당
        Mat frame = inputFrame.gray();
        //Mat frame = inputFrame.rgba();
        Mat frame2 = frame.clone(); // 프레임을 저장하기위한 변수

        //세로모드에서 frame회전
        if (flag_rotate == 0) {
            Mat mRgabT = frame.t();
            Core.flip(frame.t(), mRgabT, 1);
            Imgproc.resize(mRgabT, mRgabT, frame.size());
            frame = mRgabT;
        }
        //Mat to Bitmap으로 변환
        Bitmap bmp = null;
        //화면 가로,세로 길이 받아오기
        int height_frame = frame.height();
        int width_frame = frame.width();
        System.out.println("가로" + width_frame + "세로" + height_frame);
        //Imgproc.rectangle(frame, new Point(width_frame * 0.15, height_frame * 0.15), new Point(width_frame * 0.85, height_frame * 0.85), new Scalar(0, 255, 255), 2);

        //이미지 저장위해 bitmap bmp 생성
        try {
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            bmp = Bitmap.createBitmap(frame2.cols(), frame2.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame2, bmp);
        } catch (CvException e) {
            Log.d("Exception", e.getMessage());
        }

        //YOLO 버튼 클릭으로 startYolo가 true로 바뀌었을 때
        if (startYolo == true) {
            //Imgproc을 이용해 이미지 프로세싱을 한다. rgba를 rgb로 컬러체계변환
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_GRAY2RGB);

            //출처 https://stackoverflow.com/questions/39411160/how-to-save-opencv-resultant-images-in-android-automatically
            //bmp파일을 이미지 폴더에 저장, 저장경로(내장메모리/Pictures)
            //MediaStore.Images.Media.insertImage(getContentResolver(),bmp,"t","hello");

            //blob이란 input image가  mean subtraction, normalizing, and channel swapping을 거치고 난 후를 말합니다.
            //Dnn.blobFromImage를 이용하여 이미지 픽셀의 평균값을 계산하여 제외하고 스케일링을 하고 또 채널 스왑(RED와 BLUE)을 진행합니다.
            //현재는 128 x 128로 스케일링하고 채널 스왑은 하지 않습니다. 생성된 4-dimensional blob 값을 imageBlob에 할당합니다.
            //www.pyimagesearch.com/2017/11/06/deep-learning-opencvs-blobfromimage-works 참고하였습니다.
            Mat imageBlob = Dnn.blobFromImage(frame, 0.00392, new Size(128, 128), new Scalar(0, 0, 0),/*swapRB*/false, /*crop*/false);


            tinyYolo.setInput(imageBlob);
            System.out.println("BLBO PASS");
            //cfg파일에서 yolo layer number를 확인하여 이를 순전파에 넣어줍니다.
            //yolov3의 경우 yolo layer가 3개임으로 initialCapacity를 3으로 줍니다.
            //java.util.List<Mat> result = new java.util.ArrayList<Mat>(3);
            List<Mat> result = new ArrayList<Mat>(3);

            List<String> outBlobNames = new ArrayList<>();

            //yolov3
            outBlobNames.add(0, "yolo_82");
            outBlobNames.add(1, "yolo_94");
            outBlobNames.add(2, "yolo_106");

            //yolov3-tiny
//            outBlobNames.add(0, "yolo_16");
//            outBlobNames.add(1, "yolo_23");

            //vlov4-tiny
            //outBlobNames.add(0, "yolo_30");
            //outBlobNames.add(1, "yolo_37");
            //위 코드는 각각의 yolo version에 맞춰 layer number를 준 값입니다.

            //순전파를 진행합니다.
            tinyYolo.forward(result, outBlobNames);
            //30%이상의 확률만 출력해주겠다.
            float confThreshold = 0.3f;

            List<Integer> clsIds = new ArrayList<>();
            List<Float> confs = new ArrayList<>();
            List<Rect> rects = new ArrayList<>();

            for (int i = 0; i < result.size(); ++i) {

                Mat level = result.get(i);

                for (int j = 0; j < level.rows(); ++j) {
                    Mat row = level.row(j);
                    Mat scores = row.colRange(5, level.cols());
                    Core.MinMaxLocResult mm = Core.minMaxLoc(scores);

                    float confidence = (float) mm.maxVal;
                    System.out.println(confidence);
                    Point classIdPoint = mm.maxLoc;

                    if (confidence > confThreshold) {
                        int centerX = (int) (row.get(0, 0)[0] * frame.cols());
                        int centerY = (int) (row.get(0, 1)[0] * frame.rows());
                        int width = (int) (row.get(0, 2)[0] * frame.cols());
                        int height = (int) (row.get(0, 3)[0] * frame.rows());


                        int left = centerX - width / 2;
                        int top = centerY - height / 2;

                        clsIds.add((int) classIdPoint.x);
                        confs.add(confidence);

                        rects.add(new Rect(left, top, width, height));
                    }
                }
            }
            int ArrayLength = confs.size();

            if (ArrayLength >= 1) {
                // Apply non-maximum suppression procedure.
                float nmsThresh = 0.2f;

                MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));

                Rect[] boxesArray = rects.toArray(new Rect[0]);

                MatOfRect boxes = new MatOfRect(boxesArray);

                MatOfInt indices = new MatOfInt();
                System.out.println(confidences.size()+ " " + boxes.size());
                System.out.println("에휴?");
                Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);
                System.out.println("오잉?");
                // Draw result boxes:
                int[] ind = indices.toArray();
                for (int i = 0; i < ind.length; ++i) {

                    int idx = ind[i];
                    Rect box = boxesArray[idx];

                    int idGuy = clsIds.get(idx);

                    float conf = confs.get(idx);


                    List<String> cocoNames = Arrays.asList("a person", "a bicycle", "a motorbike", "an airplane", "a bus", "a train", "a truck", "a boat", "a traffic light", "a fire hydrant", "a stop sign", "a parking meter", "a car", "a bench", "a bird", "a cat", "a dog", "a horse", "a sheep", "a cow", "an elephant", "a bear", "a zebra", "a giraffe", "a backpack", "an umbrella", "a handbag", "a tie", "a suitcase", "a frisbee", "skis", "a snowboard", "a sports ball", "a kite", "a baseball bat", "a baseball glove", "a skateboard", "a surfboard", "a tennis racket", "a bottle", "a wine glass", "a cup", "a fork", "a knife", "a spoon", "a bowl", "a banana", "an apple", "a sandwich", "an orange", "broccoli", "a carrot", "a hot dog", "a pizza", "a doughnut", "a cake", "a chair", "a sofa", "a potted plant", "a bed", "a dining table", "a toilet", "a TV monitor", "a laptop", "a computer mouse", "a remote control", "a keyboard", "a cell phone", "a microwave", "an oven", "a toaster", "a sink", "a refrigerator", "a book", "a clock", "a vase", "a pair of scissors", "a teddy bear", "a hair drier", "a toothbrush");


                    intConf = (int) (conf * 100);

                    //opencv의 이미지 프로세싱을 진행합니다.
                    //putText를 이용하여 label의 이름을 입력하여 줍니다.
                    Imgproc.putText(frame, cocoNames.get(idGuy) + " " + intConf + "%", box.tl(), Core.FONT_HERSHEY_COMPLEX, 2, new Scalar(255, 255, 0), 2);

                    //opencv의 이미지 프로세싱을 진행합니다.
                    //rectangle을 이용하여 사각형을 그려줍니다.
                    Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);

                }

            }
        }
        //Date객체를 활용한 현재 시간 데이터 활용
        SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");

        Date time = new Date();

        String time_Q = format.format(time);

        if (flag_camera == 1) {
                //메인쓰레드 밖에서 UI변경을 요구함으로 다음과 같이 runOnUiThread 메소드를 호출하여 출력합니다.
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "화면을 저장합니다", Toast.LENGTH_SHORT).show();
                    }
                });
                MediaStore.Images.Media.insertImage(getContentResolver(), bmp, "time_"+ time_Q, "IMAGE");
                flag_camera = 0;

                //두 번째 화면으로 전환
            boolean capture1 = capture.isClickable();
            if (startYolo == true && capture1 == true) {
                Intent intent = new Intent(MainActivity.this, user_texting.class);
                startActivity(intent);
            }
                //아래는 원래 있었던 코드.
                //메인쓰레드 밖에서 UI변경을 요구함으로 다음과 같이 runOnUiThread 메소드를 호출하여 출력합니다.
                /*
                 if (strText == null){
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "먼저 Label을 입력해주세요!", Toast.LENGTH_SHORT).show();
                    }
                });
                flag_camera = 0;
            } else {
                //메인쓰레드 밖에서 UI변경을 요구함으로 다음과 같이 runOnUiThread 메소드를 호출하여 출력합니다.
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "화면을 저장합니다", Toast.LENGTH_SHORT).show();
                    }
                });
                MediaStore.Images.Media.insertImage(getContentResolver(), bmp, strText + "_" + time_Q, "IMAGE");
                flag_camera = 0;
            }
            }*/

        }
        return frame;
    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //여기서부턴 퍼미션(권한) 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    protected void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                //cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permission_list,0);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //사용자가 권한을 허용하면,
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onCameraPermissionGranted();
        }else{
            //권한설정이 완료되면 안내 문구
            showDialogForPermission("마스크를 꼼꼼히 착용하고,\n얼굴감지 >> 촬영 터치!");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }
    // Upload file to storage and return a path.
    // YOLO의 cfg와 weight를 불러오기 위한 코드입니다.
    // asset 폴더를 읽어오는 과정에서 string이 아닌 inputstream으로 받아오기에 이를 다시 string으로 변환해줍니다.
    // https://recipes4dev.tistory.com/125을 참고하여 asset폴더를 생성하고 yolo 모델 파일을 저장하였습니다.
    // https://docs.opencv.org/3.4/d0/d6c/tutorial_dnn_android.html에서 getpath함수를 참고하였습니다.
    private static String getPath(String file, Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return "";
    }


}