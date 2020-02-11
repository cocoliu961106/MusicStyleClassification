package FeatureExtractor.MFCC.Experiment;

import FeatureExtractor.MFCC.Util.WaveFileReader;

//测试读取wav文件的类
public class ReadWaveDemo {
    public static void main(String[] args) {
        String fileName = "src/data/genres/hiphop/hiphop.00098.wav";
        WaveFileReader wfr = new WaveFileReader(fileName);
        System.out.println(wfr.isSuccess());
        System.out.println(wfr.getBitPerSample());
        System.out.println(wfr.getSampleRate());
        System.out.println(wfr.getNumChannels());
        System.out.println(wfr.getDataLen());
        System.out.println(wfr.getData()[0].length);
    }
}
