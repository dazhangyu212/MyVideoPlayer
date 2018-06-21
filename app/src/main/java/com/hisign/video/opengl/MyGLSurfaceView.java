package com.hisign.video.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 描述：
 * 公司：北京海鑫科金高科技股份有限公司
 * 作者：zhangyu
 * 创建时间 2018/6/7
 */

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer mRender;

    public MyGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        mRender = new MyGLRenderer();
        setRenderer(mRender);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    private static class MyGLRenderer implements Renderer {

        private Triangle triangle;

        private Square mSquare;
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            GLES20.glClearColor(0,0,0,1);
            triangle = new Triangle();
            mSquare = new Square();
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {

            GLES20.glViewport(0,0,i,i1);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        public  int loadShader(int type,String shaderCode){
            int shader = GLES20.glCreateShader(type);

            GLES20.glShaderSource(shader,shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        };
    }
}
