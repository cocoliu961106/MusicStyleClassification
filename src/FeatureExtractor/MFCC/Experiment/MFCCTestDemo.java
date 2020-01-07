package FeatureExtractor.MFCC.Experiment;

import FeatureExtractor.MFCC.Model.BeforeFFT;
import FeatureExtractor.MFCC.Model.MFCC;
import FeatureExtractor.MFCC.Model.MFCCProcecure;
import FeatureExtractor.MFCC.Util.WaveFileReader;

import java.util.Arrays;

// 测试一首歌的提取效果，通过特征提取算法获得每一帧n维向量后（n为MFCC参数个数），
// 再对整首歌每一个参数求平均值和方差，最终结果一首歌对应一个2n维向量
public class MFCCTestDemo {
    public static void main(String args[]) throws Exception {
        String fileName = "src/data/genres/rock/rock.00094.wav";
        // 1.将原语音文件数字化表示
        WaveFileReader wfr = new WaveFileReader(fileName);
        // 获取真实数据部分，也就是我们拿来特征提取的部分
        double[] data = new double[wfr.getDataLen()];
        for (int i = 0; i < wfr.getDataLen(); i++) {
            data[i] = wfr.getData()[0][i];
        }

        // 2.进行特征提取
        double[] result = new MFCCProcecure().processingData(data, wfr.getSampleRate()).getParameter();
        for (int i = 0; i < result.length; i++) {
            System.out.println(result[i]);
        }
    }
}
