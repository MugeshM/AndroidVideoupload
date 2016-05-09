package com.project.mugeshm.videoupload;
/**
 * Created by MugeshM on 5/9/2016.
 */
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.PersistableBundle;
import android.widget.MediaController;
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
import android.widget.VideoView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

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
    ProgressDialog progress,videoprogress;
    String filenameinserver;
    int position=0;
    VideoView videoview;
    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt=(Button)findViewById(R.id.button);
        bt2=(Button)findViewById(R.id.button2);
        tv=(TextView)findViewById(R.id.textView);
        imageView=(ImageView)findViewById(R.id.imageView);
        videoview=(VideoView)findViewById(R.id.videoview);
        if (mediaController == null) {
            mediaController = new MediaController(MainActivity.this);
        }

        videoview.setMediaController(mediaController);
        videoview.requestFocus();
        mediaController.setAnchorView(findViewById(R.id.videoview));

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

        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoprogress.dismiss();
                videoview.seekTo(position);
                if (position == 0) {
                    //videoview.start();
                } else {
                    videoview.pause();
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
           /* if(selectedpath!=null) {
                videoview.setVideoURI(Uri.parse(selectedpath));
                videoview.setVisibility(View.GONE);
            }*/
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
                double percent = ((double) bytesWritten / (double) totalSize) * 100;
                Log.d("percent", "" + percent);
                // tv.setText(String.valueOf(percent));
                progress.setProgress((int) percent);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] bytes) {
                progress.dismiss();

                tv.setText("Successfully uploaded" + "\n Total number of videos uploaded : " + sqlhelper.numberOfRows());

                String str="";
                try {
                    str = new String(bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                filenameinserver=str;
                sqlhelper.insertrow(str);
                loadvideo(filenameinserver);
                //Toast.makeText(getBaseContext(),str,Toast.LENGTH_LONG).show();
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

    private void loadvideo(String fname) {
        videoprogress = new ProgressDialog(MainActivity.this);
        videoprogress.setTitle("Video from Server");
        videoprogress.setMessage("Buffering...");
        videoprogress.setIndeterminate(false);
        videoprogress.setCancelable(false);
        videoprogress.show();

        /*String date=String.valueOf(System.currentTimeMillis());
        File userFile = new File(selectedpath);
        String filename = userFile.getName();*/
        String url="http://192.168.0.123:3000/videos?name="+fname;
        //String url="http://clips.vorwaerts-gmbh.de/VfE_html5.mp4";
        Toast.makeText(getBaseContext(), url, Toast.LENGTH_SHORT).show();
        videoview.setVideoURI(Uri.parse(url));
        //videoview.start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt("Position", position);
        videoview.pause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt("Position");
        videoview.seekTo(position);
    }


}


