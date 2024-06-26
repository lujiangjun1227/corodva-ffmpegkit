package com.iknow.android.features.trim;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.iknow.android.MResource;
import com.iknow.android.TrimmerCordovaPlugin;
import com.iknow.android.interfaces.TrimVideoListener;
import com.iknow.android.widget.VideoTrimmerView;

public class VideoTrimmerActivity extends Activity implements TrimVideoListener {

    private static final String TAG = "jason";
    private static final String VIDEO_PATH_KEY = "path";
    private static final String VIDEO_OUT_PATH_KEY = "savePath";
    public static final int VIDEO_TRIM_REQUEST_CODE = 0x001;
    private ProgressDialog mProgressDialog;
    private VideoTrimmerView trimmerView;
    private String saveVideoPath;

    public static void call(Activity from, String videoPath, String savePath) {
        if (!TextUtils.isEmpty(videoPath)) {
            Intent intent = new Intent(from, VideoTrimmerActivity.class);
            intent.putExtra(VIDEO_PATH_KEY, videoPath);
            intent.putExtra(VIDEO_OUT_PATH_KEY, savePath);
            from.startActivityForResult(intent, VIDEO_TRIM_REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(MResource.getIdByName(this, "layout", "activity_trimmer_layout"));
        String path = getIntent().getStringExtra(VIDEO_PATH_KEY);
        this.saveVideoPath = getIntent().getStringExtra(VIDEO_OUT_PATH_KEY);
        trimmerView = (VideoTrimmerView) findViewById(MResource.getIdByName(this, "id", "trimmer_view"));
        if (trimmerView != null) {
            trimmerView.setOnTrimVideoListener(this);
            trimmerView.initVideoByURI(Uri.parse(path));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        trimmerView.onVideoPause();
        trimmerView.setRestoreState(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trimmerView.onDestroy();
    }

    @Override
    public void onStartTrim() {
        buildDialog(getResources().getString(MResource.getIdByName(this, "string", "trimming"))).show();
    }

    @Override
    public void onFinishTrim(String in) {
        //TODO: please handle your trimmed video url here!!!
        String out = saveVideoPath; //.replace("file:///", "").replace("file://", "");
        buildDialog(getResources().getString(MResource.getIdByName(this, "string", "compressing"))).show();

        String cmd = "-threads 2 -y -i " + in + " -strict -2 -vcodec libx264 -preset ultrafast -crf 28 -acodec copy -ac 2 " + out;
        //String cmd = "-threads 2 -y -i " + inputFile + " -strict -2 -vcodec libx264 -vf scale=720:-1 -crf 28 -acodec copy -ac 2 " + outputFile;

        FFmpegKit.executeAsync(cmd, session -> {
            ReturnCode returnCode = session.getReturnCode();
            if (ReturnCode.isSuccess(returnCode)) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                TrimmerCordovaPlugin.cdvCallbackContetxt.success(saveVideoPath);
                finish();
            } else {
                TrimmerCordovaPlugin.cdvCallbackContetxt.error("Compress video failed!");
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                TrimmerCordovaPlugin.cdvCallbackContetxt.success(saveVideoPath);
                finish();
            }
        });
    }

    @Override
    public void onCancel() {
        trimmerView.onDestroy();
        finish();
    }

    private ProgressDialog buildDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "", msg);
        }
        mProgressDialog.setMessage(msg);
        return mProgressDialog;
    }
}
