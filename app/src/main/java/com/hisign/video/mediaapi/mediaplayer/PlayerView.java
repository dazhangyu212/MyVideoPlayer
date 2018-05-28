package com.hisign.video.mediaapi.mediaplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/5/28
 */

public class PlayerView extends SurfaceView {
    private double aspectRatio;

    public PlayerView(Context context) {
        super(context);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public void setAspect(double aspect){
        if (aspect > 0){
            this.aspectRatio = aspect;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (aspectRatio > 0){
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
            final int horizPadding = getPaddingLeft() + getPaddingRight();
            final int verticalPadding = getPaddingTop()+getPaddingBottom();
            initialHeight -= verticalPadding;
            initialWidth -= horizPadding;
            final double viewAspectRatio = (double) initialWidth/initialHeight;
            final double aspectDiff = aspectRatio/viewAspectRatio -1;
            if (Math.abs(aspectDiff) > 0.01){
                if (aspectDiff > 0){
                    // 高度适配宽度
                    initialHeight = (int) (initialWidth/aspectRatio);
                }else {
                    //根据高度适配宽度
                    initialWidth = (int) (initialHeight*aspectRatio);
                }
                initialWidth += horizPadding;
                initialHeight += verticalPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth,MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight,MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }
}
