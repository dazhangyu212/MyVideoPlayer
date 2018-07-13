package com.hisign.video.record_video;

import android.app.Fragment;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hisign.video.record_video.encoder.MediaMuxerWrapper;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/6/21
 */

public class CameraFragment extends Fragment {

    private static final boolean DEBUG = false;

    private static final String TAG = "CameraFragment";

    private CameraGLView mCameraView;

    private TextView mScaleModeView;

    private ImageButton mRecordButton;

    private MediaMuxerWrapper mMuxer;

}
