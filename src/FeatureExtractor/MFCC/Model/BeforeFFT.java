package FeatureExtractor.MFCC.Model;


import java.util.ArrayList;
import java.util.Arrays;

public class BeforeFFT {
    // 预加重
    public static void preEnhance(double data[]) {
        // 将信号值域先置于-1到1之间
        double max = 0;
        for(double i : data) {
            if (Math.abs(i) > max)
                max = Math.abs(i);
        }
        data[0] = data[0] / max;
        for(int i = 1;i < data.length; i++) {
            data[i] = data[i] / max;
            data[i] = data[i] - 0.97 * data[i - 1];
        }
    }

    // 分帧
    public double[][] framing(double[] data, int fLength) {
        ArrayList<double[]> frameData = new ArrayList<>();
        int start = 0, step = fLength / 2;
        // 最后一帧时间不够直接舍弃
        double[] currentFrameData;
        while (start < data.length) {
            if (start + fLength > data.length)
                break;
            currentFrameData = Arrays.copyOfRange(data, start, start + fLength);
            frameData.add(currentFrameData);
            start = start + step;
        }
        double[][] result = new double[frameData.size()][];
        return frameData.toArray(result);
    }

    // 加窗
    public void HammingWindow(double[][] frameData) {
        for (int i = 0; i< frameData.length; i++) {
            for (int n = 0;n < frameData[0].length; n++) {
                double currentWindowValue = 0.5 - 0.5 * Math.cos((2 * Math.PI * n) / (frameData[0].length - 1));
                frameData[i][n] = frameData[i][n] * currentWindowValue;
            }
        }
    }
}
