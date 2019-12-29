# MusicStyleClassification
基于Spark的音乐风格分类系统
功能分为离线和在线两个部分
1.离线部分对训练样本进行音乐的特征提取，并在分类模型下进行训练并保存模型（用spark core实现）
2.在线部分实时获取用户上传的音乐，并进行在线的特征提取，用保存的模型进行分类的预测（用spark streaming实现）

具体每个包的含义：

Classifier: 分类器模型
    CNN：卷积神经网络模型
    NN： 神经网络模型

FeatureExtractor: 特征提取模型
    MFCC：梅尔倒谱系数

ClassificationModule: 整个音乐分类系统的流程
    OfflineTraining: 离线训练模块
    OnlinePredicting: 在线预测模块