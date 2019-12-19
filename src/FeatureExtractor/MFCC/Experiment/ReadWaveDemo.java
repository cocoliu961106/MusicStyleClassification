package FeatureExtractor.MFCC.Experiment;

import FeatureExtractor.MFCC.Util.WaveFileReader;

public class ReadWaveDemo {
    public static void main(String[] args) {
        String fileName = "F:\\BaiduNetdiskDownload\\林俊杰 - 可惜没如果.wav";
        WaveFileReader wfr = new WaveFileReader(fileName);
        System.out.println(wfr.isSuccess());
        System.out.println(wfr.getBitPerSample());
        System.out.println(wfr.getSampleRate());
        System.out.println(wfr.getNumChannels());
        System.out.println(wfr.getDataLen());
        System.out.println(wfr.getData()[0].length);
    }
}
