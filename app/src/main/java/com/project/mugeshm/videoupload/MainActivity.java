package com.project.mugeshm.videoupload;
/**
 * Created by MugeshM on 5/9/2016.
 */
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    SQLHelper sqlhelper;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 0;
    //String filemanagerstring="";
    String selectedpath="";
    TextView tv;
    Button bt,bt2;
    private String filepath = null;
    ImageView imageView;
    FileInputStream fileInputStream = null;
    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt=(Button)findViewById(R.id.button);
        bt2=(Button)findViewById(R.id.button2);
        tv=(TextView)findViewById(R.id.textView);
        imageView=(ImageView)findViewById(R.id.imageView);

        sqlhelper=new SQLHelper(this);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);

            }
        });
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileInputStream fstrm;
                try {
                    fstrm = new FileInputStream(selectedpath);
                    Send_Now(fstrm);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedImageUri = data.getData();

                filepath = getPath(selectedImageUri);
                Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Video.Thumbnails.MINI_KIND);
                imageView.setImageBitmap(bitmap);
                Toast.makeText(getBaseContext(),"Bitmap",Toast.LENGTH_LONG).show();
                // OI FILE Manager
               // filemanagerstring = selectedImageUri.getPath();

                // MEDIA GALLERY
                selectedpath = getPath(selectedImageUri);
                if (selectedpath != null) {
                    Toast.makeText(getBaseContext(), selectedpath,Toast.LENGTH_SHORT).show();

                }
            }
        }
    }

    // UPDATED!
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    void Send_Now(FileInputStream fStream){
        fileInputStream = fStream;
        /*progress = ProgressDialog.show(this, "Upload","wait, Uploading video to server", true);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setIndeterminate(true);
        */
        progress = new ProgressDialog(MainActivity.this);
        progress.setMessage("Uploading Video");
        progress.setIndeterminate(false);
        progress.setMax(100);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
        uploadFile();
    }

    public void uploadFile() {

        File myFile = new File(selectedpath);
        //progress.setMax((int)(long)myFile.length());
        RequestParams params = new RequestParams();
        try {
            params.put("uservideo", myFile);
        } catch(FileNotFoundException e) {}

// send request
        AsyncHttpClient client = new AsyncHttpClient();
        client.post("http://192.168.0.123:3000/api/video", params, new AsyncHttpResponseHandler() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                Log.d("progress", "pos: " + bytesWritten + " len: " + totalSize);
                double percent=((double)bytesWritten/(double)totalSize)*100;
                Log.d("percent", "" +percent);
               // tv.setText(String.valueOf(percent));
                progress.setProgress((int) percent);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] bytes) {
                progress.dismiss();
                sqlhelper.insertrow(selectedpath);
                tv.setText("Successfully uploaded" + "\n Total number of videos uploaded : " + sqlhelper.numberOfRows());
                // Toast.makeText(getBaseContext(),"Successfully uploaded",Toast.LENGTH_SHORT).show();
                Log.d("success", String.valueOf(statusCode));

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                // handle failure response
                progress.dismiss();
                tv.setText("Unable to upload try again");
                // Toast.makeText(getBaseContext(),"Unable to upload try again",Toast.LENGTH_SHORT).show();
                Log.d("Failure", String.valueOf(statusCode));

            }

        });

    }
}


