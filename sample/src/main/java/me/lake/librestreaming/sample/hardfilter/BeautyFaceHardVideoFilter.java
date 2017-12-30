package me.lake.librestreaming.sample.hardfilter;

import android.content.Context;
import android.opengl.GLES20;

import me.lake.librestreaming.filter.hardvideofilter.OriginalHardVideoFilter;
import me.lake.librestreaming.sample.R;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by hukanli on 2017_09_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class BeautyFaceHardVideoFilter extends OriginalHardVideoFilter {
    private int singleStepOffsetLoc;
    private int paramsLoc;
    private int brightnessLoc;


    private float stepScale;
    private float beautyLevel = 0.9f;
    private float pinkLevel = 0.1f;
    private float brightLevel = 0.4f;

    public void setBeautyParams(int beauty, int pink, int bright) {
        beautyLevel = ((float)beauty / 100.0f);
        pinkLevel = ((float)pink / 100.0f);
        brightLevel = ((float)bright / 100.0f);
    }

    public BeautyFaceHardVideoFilter(Context context, int stepScale) {
        super(null, GLESTools.readTextFile(context.getResources(), R.raw.beautyface));
        this.stepScale = (float) stepScale;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);

        singleStepOffsetLoc = GLES20.glGetUniformLocation(glProgram, "singleStepOffset");
        paramsLoc = GLES20.glGetUniformLocation(glProgram, "params");
        brightnessLoc = GLES20.glGetUniformLocation(glProgram, "brightness");
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        GLES20.glUniform2f(singleStepOffsetLoc, (float) (stepScale / SIZE_WIDTH), (float) (stepScale / SIZE_HEIGHT));
        setParams(beautyLevel, pinkLevel);
        setBrightness(brightLevel);
    }



    private void setParams(float beautyLevel, float pinkLevel) {
        float vec4[] = new float[4];
        vec4[0] = 1.0f - 0.6f * beautyLevel; //r
        vec4[1] = 1.0f - 0.3f * beautyLevel; //g
        vec4[2] = 0.1f + 0.3f * pinkLevel;   //b
        vec4[3] = 0.1f + 0.3f * pinkLevel;   //a

        GLES20.glUniform4fv(paramsLoc, 1, vec4, 0);
    }

    private void setBrightness(float brightLevel) {
        GLES20.glUniform1f(brightnessLoc, 0.6f * (-0.5f + brightLevel));
    }
}