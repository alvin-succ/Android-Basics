package com.example.cameraalbumtest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private Button takePhoto;
    private Button shareToWeChat;
    private Uri imageUri;//原始图片Uri，用于显示
    private File sharedImageFile;//原始图片路径备份
    private ImageView imageView;

    private static final String TAG = "CameraAlbumTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imageView = (ImageView)findViewById(R.id.image_view);
        takePhoto = (Button) findViewById(R.id.take_photo);
        shareToWeChat = (Button) findViewById(R.id.share_to_wechat);

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result-> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        //5. length(): 4170693 bytes
                        Log.d("FileTest", "5. length(): " + sharedImageFile.length() + " bytes");
                        handlePhotoResult();
                    }else if(result.getResultCode() == Activity.RESULT_CANCELED){
                        Toast.makeText(this, "取消拍照", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "拍照失败", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted->{
                    if(isGranted){
                        takePhotoAction();
                    }else{
                        // 权限被拒绝
                        handlePermissionDenied();
                    }
                }
        );

        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(hasCameraPermission()){
                    takePhotoAction();
                }else{
                    //检查是否需要权限请求说明
                    if(shouldShowCameraPermissionRationale()){
                        //向用户解释为什么需要权限
                        showCameraPermissionExplanation();
                    }else{
                        //直接请求权限
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                }
            }
        });

        shareToWeChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareImageToWeChat();
            }
        });

    }

    private void takePhotoAction(){
        // 使用外部缓存目录，不需要存储权限
        //新建的File对象仅仅是一个指向该路径的引用，并没有在磁盘上创建文件。
        File outputImage = new File(getExternalCacheDir(),
                "photo" + ".jpg");
        try{
            if(outputImage.exists()){
                boolean deleted = outputImage.delete();
                if(!deleted){
                    Toast.makeText(this, "无法删除旧文件", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //在磁盘上创建一个空的文件（0字节），文件名为photo.jpg。
            boolean created = outputImage.createNewFile();
            if(!created){
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "创建文件异常", Toast.LENGTH_SHORT).show();
            return;
        }

        // 完整的调试代码
        Log.d("FileTest", "=== File 对象信息 ===");
        //1. outputImage: /storage/emulated/0/Android/data/com.example.cameraalbumtest/cache/photo.jpg
        Log.d("FileTest", "1. outputImage: " + outputImage);  // 调用 toString()
        Log.d("FileTest", "2. getPath(): " + outputImage.getPath());
        Log.d("FileTest", "3. getAbsolutePath(): " + outputImage.getAbsolutePath());

        //第一次打印，bytes是0
        Log.d("FileTest", "4. length(): " + outputImage.length() + " bytes");


        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
            imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.cameraalbumtest.fileprovider", outputImage);
        }else{
            imageUri = Uri.fromFile(outputImage);
        }
        //保存文件路径，等从相册返回再打印看下大小
        sharedImageFile = outputImage;
        Log.d("FileTest", "6.imageUri: " + imageUri.toString());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }

        if(intent.resolveActivity(getPackageManager()) != null){
            takePhotoLauncher.launch(intent);
        }else{
            Toast.makeText(this, "没有找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePhotoResult(){
        if(imageUri == null){
            Toast.makeText(this, "图片Uri为空", Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            long timestamp = System.currentTimeMillis();
            ObjectKey signature = new ObjectKey(timestamp);
//            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
//
//            imageView.setImageBitmap(bitmap);
            //这里尝试用下Glide库
            Glide.with(this)
                    .load(imageUri)
                    .apply(new RequestOptions()
                            .override(1080, 1920)  // 限制尺寸
                            .format(DecodeFormat.PREFER_RGB_565))  // 减少内存
                            .signature(signature)
                    .into(imageView);
            updateSharedButtonState(true);
            Toast.makeText(this, "照片拍摄成功，可以分享到微信了", Toast.LENGTH_SHORT).show();
        }catch(Exception e){
            e.printStackTrace();
            updateSharedButtonState(false);
            Toast.makeText(this, "图片解析失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分享图片到微信
     */
    private void shareImageToWeChat(){
        if(sharedImageFile == null || !sharedImageFile.exists()){
            Toast.makeText(this, "请先拍摄照片", Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            //获取图片的Uri
            Uri sharedUri;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                //Android 7.0+使用FileProvider
                sharedUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        "com.example.cameraalbumtest.fileprovider",
                        sharedImageFile);
            }else{
                sharedUri = Uri.fromFile(sharedImageFile);
            }
            //创建分享Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");//分享图片
            shareIntent.putExtra(Intent.EXTRA_STREAM, sharedUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "分享一张照片");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            //启动分享方式选择弹窗
            launchShareDialog(shareIntent, sharedUri);
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 展示不同分享方式选择弹窗
     * @param sharedIntent
     * @param sharedUri
     */
    private void launchShareDialog(Intent sharedIntent, Uri sharedUri){
        String[] options = {"分享给微信好友", "分享到朋友圈", "使用其他应用分享"};
        new AlertDialog.Builder(this)
                .setTitle("选择分享方式")
                .setItems(options, (dialog, which) -> {
                    switch(which){
                        case 0:
                            shareToWeChatSession(sharedIntent, sharedUri);
                            break;
                        case 1:
                            shareToWeChatTimeLine(sharedIntent, sharedUri);
                            break;
                        case 2:
                            showShareChooser(sharedIntent);
                            break;
                    }
                }).show();
    }

    private void shareToWeChatSession(Intent sharedIntent, Uri sharedUri){
        try{
            ComponentName componentName = new ComponentName(
                    "com.tencent.mm",
                    "com.tencent.mm.ui.tools.ShareImgUI");
            Intent weChatIntent = new Intent(Intent.ACTION_SEND);
            weChatIntent.setComponent(componentName);
            weChatIntent.setType("image/*");
            weChatIntent.putExtra(Intent.EXTRA_STREAM, sharedUri);
            weChatIntent.putExtra("Kdescription", "分享一张照片");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                weChatIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(weChatIntent);
            Toast.makeText(this, "正在打开微信...", Toast.LENGTH_SHORT).show();

        }catch(Exception e){
            e.printStackTrace();
            // 如果直接打开失败，可能是微信未安装或版本不支持，使用通用分享
            Toast.makeText(this, "打开微信失败，请确保已安装微信", Toast.LENGTH_SHORT).show();
            showShareChooser(sharedIntent);
        }
    }

    private void shareToWeChatTimeLine(Intent sharedIntent, Uri sharedUri){
        try{
            // 微信朋友圈的ComponentName
            ComponentName componentName = new ComponentName(
                    "com.tencent.mm",
                    "com.tencent.mm.ui.tools.ShareToTimeLineUI"
            );
            Intent timeLineIntent = new Intent(Intent.ACTION_SEND);
            timeLineIntent.setComponent(componentName);
            timeLineIntent.setType("image/*");
            timeLineIntent.putExtra(Intent.EXTRA_STREAM, sharedUri);
            timeLineIntent.putExtra("Kdescription", "分享一张照片");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                timeLineIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(timeLineIntent);
            Toast.makeText(this, "正在打开微信...", Toast.LENGTH_SHORT).show();

        }catch(Exception e){
            e.printStackTrace();
            // 如果直接打开失败，可能是微信未安装或版本不支持，使用通用分享
            Toast.makeText(this, "打开微信失败，请确保已安装微信", Toast.LENGTH_SHORT).show();
            showShareChooser(sharedIntent);
        }
    }

    private void showShareChooser(Intent sharedIntent){
        try{
            //创建选择器标题
            String title = "选择分享应用";
            //创建分享选择器
            Intent chooser = Intent.createChooser(sharedIntent, title);

            //验证是否有应用可以处理这个Intent
            if(sharedIntent.resolveActivity(getPackageManager()) != null){
                startActivity(chooser);
            }else{
                Toast.makeText(this, "没有找到可以分享的应用", Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新分享按钮状态
     */
    private void updateSharedButtonState(boolean enabled){
        shareToWeChat.setEnabled(enabled);
        if(enabled){
            shareToWeChat.setAlpha(1.0f);
        }else{
            shareToWeChat.setAlpha(0.5f);
        }
    }


    /**
     * 判断是否有相机权限
     */
    private boolean hasCameraPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }
    /**
     * 检查是否需要显示权限请求说明
     */
    private boolean shouldShowCameraPermissionRationale(){
        return ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA);
    }


    /**
     * 显示相机权限说明
     */
    private void showCameraPermissionExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("需要相机权限")
                .setMessage("拍照功能需要访问您的相机，请授予相机权限以继续使用")
                .setPositiveButton("确定", (dialog, which) -> {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 处理权限被拒绝的情况
     */
    private void handlePermissionDenied() {
        boolean shouldShowRationale = shouldShowCameraPermissionRationale();

        if (shouldShowRationale) {
            // 可以再次请求
            new AlertDialog.Builder(this)
                    .setTitle("权限被拒绝")
                    .setMessage("拍照功能需要相机权限，是否重新请求权限？")
                    .setPositiveButton("重新请求", (dialog, which) -> {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            // 永久拒绝，需要去设置
            new AlertDialog.Builder(this)
                    .setTitle("权限被永久拒绝")
                    .setTitle("相机权限被永久拒绝")
                    .setMessage("您已永久拒绝相机权限，需要到设置中手动开启")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        openAppSettings();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    /**
     * 打开应用设置页面
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}