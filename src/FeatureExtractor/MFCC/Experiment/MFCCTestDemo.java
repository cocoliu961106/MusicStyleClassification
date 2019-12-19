package FeatureExtractor.MFCC.Experiment;

import FeatureExtractor.MFCC.Model.BeforeFFT;
import FeatureExtractor.MFCC.Model.MFCC;
import FeatureExtractor.MFCC.Util.WaveFileReader;

import java.util.Arrays;

// 测试一首歌的提取效果，通过特征提取算法获得每一帧n维向量后（n为MFCC参数个数），
// 再对整首歌每一个参数求平均值和方差，最终结果一首歌对应一个2n维向量
public class MFCCTestDemo {
    public static void main(String args[]) throws Exception {
        String fileName  = "F:\\BaiduNetdiskDownload\\林俊杰 - 可惜没如果.wav";
        // 1.将原语音文件数字化表示
        WaveFileReader wfr = new WaveFileReader(fileName);
        System.out.println(wfr.isSuccess());
        System.out.println(wfr.getBitPerSample());
        System.out.println(wfr.getSampleRate());
        System.out.println(wfr.getNumChannels());
        System.out.println(wfr.getDataLen());
        System.out.println(wfr.getData()[0].length);

        // 获取真实数据部分，也就是我们用来特征提取的部分
        double[] data = new double[wfr.getDataLen()];
        for (int i = 0; i < wfr.getDataLen(); i++) {
            data[i] = wfr.getData()[0][i];
        }
        System.out.println(data[1000000]);

        // 2.预加重、分帧、加窗，得到分帧以后的信号
        BeforeFFT preSteps = new BeforeFFT();
        preSteps.preEnhance(data);
        System.out.println(data[1000000]);
        double[][] frameData = preSteps.framing(data, 512);
        System.out.println(frameData[3906][64]);
        preSteps.HammingWindow(frameData);
        System.out.println(frameData[3906][64]);

        // 3.进行FFT、通过三角滤波器并进行DCT得到MFCC系数
        MFCC MFCC = new MFCC(12, wfr.getSampleRate(), 19, 512, false, 0, false);
        double[][] MFCCParameters = new double[frameData.length][];
        for (int i = 0; i < frameData.length; i++) {
            MFCCParameters[i] = MFCC.getParameters(frameData[i]);
        }
        System.out.println(MFCCParameters.length);
        System.out.println(MFCC.getNumberOfCoefficients());
        System.out.println(Arrays.toString(MFCCParameters[3906]));

        // 求整首歌每一帧的每个参数的平均值和方差，以获得歌曲在时间上面的特征
        int numberOfParameters = MFCC.getNumberOfCoefficients();
        double[] mean = new double[numberOfParameters];
        double[] variance = new double[numberOfParameters];
        for (int i = 0; i < numberOfParameters; i++) {
            double sum = 0;
            for (int j = 0; j < MFCCParameters.length; j++) {
                sum += MFCCParameters[j][i];
            }
            mean[i] = sum / MFCCParameters.length;
        }
        for (int i = 0; i < numberOfParameters; i++) {
            double vari = 0;
            for (int j = 0; j < MFCCParameters.length; j++) {
                vari += Math.pow(MFCCParameters[j][i] - mean[i], 2);
            }
            variance[i] = vari / MFCCParameters.length;
        }
        double[] result = new double[mean.length + variance.length];
        System.arraycopy(mean, 0, result, 0, mean.length);
        System.arraycopy(variance, 0, result, mean.length, variance.length);
        System.out.println(Arrays.toString(result));
    }


//    public static void transform2SingleChannel(String sourceFilePath, String newFilePath, int bytes) throws Exception {
//        FileInputStream fis = new FileInputStream(sourceFilePath);
//        FileOutputStream fos = new FileOutputStream(newFilePath);
//        for (int i = 0; i < 44; i++) {
//            fos.write(fis.read());
//        }
//        int m = 0;
//        int data[] = new int[bytes];
//        while (fis.available() != 0) {
//            data[m] = fis.read();
//            fos.write(data[m]);
//            m++;
//            if (m == (bytes / 2 + 1)) {
//                for (int j = 0; j < (bytes/2 + 1);j++) {
//                    fis.read();
//                    fos.write(data[j]);
//                }
//                m = 0;
//            }
//        }
//        fos.flush();
//        fis.close();
//        fos.close();
//    }
}
