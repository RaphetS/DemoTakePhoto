package org.raphets.takephotoandcrop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnTakePhoto;
    private Button btnCropPhoto;
    private Button btnCamera;
    private ImageView iv;

    private Uri imgUri;
    private static final int REQUEST_CODE_CAMERA_SECOND = 104;
    private static final int REQUEST_CODE_CAMERA = 101;
    private static final int REQUEST_CODE_PHOTO_ALBUM = 102;
    private static final int REQUEST_CODE_ZOOM = 103;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * type=0 拍照、从相册选取照片
     * type=1 拍照、从相册选取照片，并进行裁剪
     */
    private int type=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iv = (ImageView) findViewById(R.id.imageView);
        btnCamera= (Button) findViewById(R.id.btn_camera);
        btnCropPhoto = (Button) findViewById(R.id.btn_takephoto_and_crop);
        btnTakePhoto = (Button) findViewById(R.id.btn_takephoto);

        btnCropPhoto.setOnClickListener(this);
        btnTakePhoto.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_takephoto:
                 showBottomDialog();
                type=0;
                break;
            case R.id.btn_takephoto_and_crop:
                showBottomDialog();
                type=1;
                break;
            case R.id.btn_camera:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Intent getImageByREQUEST_CODE_CAMERA = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(getImageByREQUEST_CODE_CAMERA, REQUEST_CODE_CAMERA_SECOND);
                } else {
                    Toast.makeText(getApplicationContext(), "请确认已经插入SD卡", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    private void showBottomDialog() {
        final BottomSheetDialog dialog = new BottomSheetDialog(MainActivity.this);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_dialog, null);
        TextView tvTakePhoto = (TextView) view.findViewById(R.id.tv_takePhoto);
        TextView tvPhotoAlbum = (TextView) view.findViewById(R.id.tv_photoalbum);
        TextView tvCancel = (TextView) view.findViewById(R.id.tv_cancel);
        tvTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                takePhoto();
            }
        });
        tvPhotoAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                selectPhotoFromAlbum();
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    /**
     * 从相册选取
     */
    private void selectPhotoFromAlbum() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, REQUEST_CODE_PHOTO_ALBUM);
    }


    /**
     * 拍照
     */
    private void takePhoto() {
        verifyStoragePermissions(MainActivity.this);
        //创建File对象，用于存储拍照后的图片
        //将此图片存储于SD卡的根目录下
        File outputImage = new File(Environment.getExternalStorageDirectory(),
                "tem.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将File对象转换成Uri对象
        //Uri表标识着图片的地址
        imgUri = Uri.fromFile(outputImage);

        PreferenceUtil.write(MainActivity.this,"image",imgUri.getPath());

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Intent getImageByREQUEST_CODE_CAMERA = new Intent("android.media.action.IMAGE_CAPTURE");
            getImageByREQUEST_CODE_CAMERA.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
            startActivityForResult(getImageByREQUEST_CODE_CAMERA, REQUEST_CODE_CAMERA);
        } else {
            Toast.makeText(getApplicationContext(), "请确认已经插入SD卡", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 图片裁剪
     */
    private void startPhotoREQUEST_CODE_ZOOM(Uri uri, int i) {
        if (uri == null) {
            Toast.makeText(getApplicationContext(), "选择图片出错！", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 600);
        intent.putExtra("outputY", 600);
        //如果为true,则通过 Bitmap bmap = data.getParcelableExtra("data")取出数据
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, i);
    }


    /**
     * 通过uri获取bitmap
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // 读取uri所在的图片
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Android 6.0之后需要申请权限
     */
    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK){
            switch (requestCode){
                case REQUEST_CODE_CAMERA_SECOND://拍照返回缩略图
                    Bitmap bm=data.getParcelableExtra("data");
                    iv.setImageBitmap(bm);
                    break;

                case REQUEST_CODE_CAMERA: //拍照结果处理

                    //解决部分三星手机imgUri为空的bug
                    String path=PreferenceUtil.readString(MainActivity.this,"image");
                    imgUri=Uri.fromFile(new File(path));
                    if (type==0){
                        Bitmap bitmap=getBitmapFromUri(imgUri);
                        /**
                         * 这里有个bug,第一次getBitmapFromUri得到的bitmap为空，
                         * 再次调用getBitmapFromUri，就不为空。
                         * 原因未知。
                         */
                        if (bitmap==null){
                            bitmap=getBitmapFromUri(imgUri);
                        }
                        iv.setImageBitmap(bitmap);

                    }else {
                        startPhotoREQUEST_CODE_ZOOM(imgUri,REQUEST_CODE_ZOOM);
                    }
                    break;

                case REQUEST_CODE_PHOTO_ALBUM: //相册结果处理
                    imgUri=data.getData();
                    if (type==0){
                        Bitmap bitmap=getBitmapFromUri(imgUri);
                       iv.setImageBitmap(bitmap);
                    }else {
                        startPhotoREQUEST_CODE_ZOOM(imgUri, REQUEST_CODE_ZOOM);
                    }
                    break;

                case REQUEST_CODE_ZOOM:   //裁剪结果处理
                    Bitmap bitmap = getBitmapFromUri(imgUri);
                    /**
                     * 这里有个bug,第一次getBitmapFromUri得到的bitmap为空，
                     * 再次调用getBitmapFromUri，就不为空。
                     * 原因未知。
                     */
                    if (bitmap==null){
                        bitmap=getBitmapFromUri(imgUri);
                    }
                    iv.setImageBitmap(bitmap);
                    break;
            }
        }
    }


}
