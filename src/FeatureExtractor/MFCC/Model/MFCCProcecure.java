package FeatureExtractor.MFCC.Model;

// 封装整个MFCC提取过程，无论是离线还是在线模块，特征提取调用这个类就行
// 步骤如果写在构造方法里，scala调用会报错，所以写在了普通方法里
public class MFCCProcecure {
    private double[] parameter = null;
    public MFCCProcecure processingData(double[] data,long sampleRate) {
        // 1.预加重、分帧、加窗，得到分帧以后的信号
        BeforeFFT preSteps = new BeforeFFT();
        preSteps.preEnhance(data);
        double[][] frameData = preSteps.framing(data, 512);
        preSteps.HammingWindow(frameData);

        // 2.进行FFT、通过三角滤波器并进行DCT得到MFCC系数
        MFCC MFCC = new MFCC(12, sampleRate, 19, 512, false, 0, false);
        double[][] MFCCParameters = new double[frameData.length][];
        for (int i = 0; i < frameData.length; i++) {
            MFCCParameters[i] = MFCC.getParameters(frameData[i]);
        }

        // 3.求整首歌每一帧的每个参数的平均值和方差，以获得歌曲的总体特征
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
        this.parameter = result;
        return this;
    }

    public double[] getParameter() {
        return this.parameter;
    }
}
