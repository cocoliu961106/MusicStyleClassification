package FeatureExtractor.MFCC.Model;

// 封装整个MFCC提取过程，无论是离线还是在线模块，特征提取调用这个类就行
// 步骤如果写在构造方法里，scala调用会报错，所以写在了普通方法里
public class MFCCProcecure {
    private double[] parameter = null;
    public MFCCProcecure processingData(double[] data) {
        // 1.预加重、分帧、加窗，得到分帧以后的信号
        BeforeFFT preSteps = new BeforeFFT();
        double[] psData = preSteps.preEnhance(data);
        double[][] frameData = preSteps.framing(psData, 512);
        preSteps.HammingWindow(frameData);

        // 2.进行FFT、通过三角滤波器并进行DCT得到MFCC系数
        MFCC MFCC = new MFCC(13, 16000, 22, 512, false, 0, false);
        double[][] MFCCParameters = new double[frameData.length][];
        for (int i = 0; i < frameData.length; i++) {
            MFCCParameters[i] = MFCC.getParameters(frameData[i]);
        }

        int numberOfParameters = MFCC.getNumberOfCoefficients();

        // 求一阶差分与二阶差分
        // int numberOfAllParameters = numberOfParameters * 1;
        // int numberOfAllParameters = numberOfParameters * 2;
        int numberOfAllParameters = numberOfParameters * 3;
        double[][] oneOrderDifference = differenceTransform(MFCCParameters, 2, numberOfParameters);
        double[][] twoOrderDifference = differenceTransform(oneOrderDifference, 2, numberOfParameters);
        // double[][] allParameters = MFCCParameters;
        double[][] allParameters = new double[MFCCParameters.length][numberOfAllParameters];
//        for (int i = 0; i< MFCCParameters.length; i++) {
//            System.arraycopy(MFCCParameters[i], 0, allParameters[i], 0, numberOfParameters);
//            System.arraycopy(oneOrderDifference[i], 0, allParameters[i], numberOfParameters, numberOfParameters);
//        }
        for (int i = 0; i< MFCCParameters.length; i++) {
            System.arraycopy(MFCCParameters[i], 0, allParameters[i], 0, numberOfParameters);
            System.arraycopy(oneOrderDifference[i], 0, allParameters[i], numberOfParameters, numberOfParameters);
            System.arraycopy(twoOrderDifference[i], 0, allParameters[i], numberOfParameters * 2, numberOfParameters);
        }

        // 3.求整首歌每一帧的每个参数的平均值和方差，以获得歌曲的总体时间特征
        double[] mean = new double[numberOfAllParameters];
        double[] variance = new double[numberOfAllParameters];
        for (int i = 0; i < numberOfAllParameters; i++) {
            double sum = 0;
            for (int j = 0; j < MFCCParameters.length; j++) {
                sum += allParameters[j][i];
            }
            mean[i] = sum / MFCCParameters.length;
        }
        for (int i = 0; i < numberOfAllParameters; i++) {
            double vari = 0;
            for (int j = 0; j < MFCCParameters.length; j++) {
                vari += Math.pow(allParameters[j][i] - mean[i], 2);
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

    // 求差分函数，n阶差分就递归调用n次
    public double[][] differenceTransform(double[][] parameters, int K, int numberOfParameters) {
        double[][] orderDifference = new double[parameters.length][numberOfParameters];
        int sumK = 0;
        for (int m = 1; m<=K; m++) {
            sumK += Math.pow(m, 2);
        }
        double sq = Math.sqrt(2 * sumK);
        for (int i = 0;i < parameters.length; i++) {
            if (i < K) {
                for (int j = 0; j<numberOfParameters; j++) {
                    orderDifference[i][j] = parameters[i+1][j] - parameters[i][j];
                }
            } else if (i >= parameters.length - K) {
                for (int j = 0; j<numberOfParameters; j++) {
                    orderDifference[i][j] = parameters[i][j] - parameters[i-1][j];
                }
            } else {
                for (int j = 0; j<numberOfParameters; j++) {
                    double delta = 0.0;
                    for (int k = 1; k<=K; k++) {
                        delta += k * (parameters[i+k][j] - parameters[i-k][j]);
                    }
                    orderDifference[i][j] = delta / sq;
                }
            }
        }
        return orderDifference;
    }

}
