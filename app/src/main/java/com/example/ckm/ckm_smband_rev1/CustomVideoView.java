package com.example.ckm.ckm_smband_rev1;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by CKM on 11/10/2016.
 */

public class CustomVideoView extends VideoView {
    private PlayPauseListener mListener;

    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPlayPauseListener(PlayPauseListener listener) {
        mListener = listener;
    }

    @Override
    public void pause() {
        super.pause();
        if (mListener != null) {
            mListener.onPause();
        }
    }

    @Override
    public void start() {
        super.start();
        if (mListener != null) {
            mListener.onPlay();
        }
    }
    @Override
    public void seekTo(int msec)
    {
        super.seekTo(msec);

        if (mListener != null)
        {
            mListener.onTimeBarSeekChanged(msec);
        }
    }

    public static interface PlayPauseListener {
        void onPlay();
        void onPause();
        void onTimeBarSeekChanged(int currentTime);
    }
}

