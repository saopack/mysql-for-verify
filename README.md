# mysql-for-verify

这个工程的主要目的是学习如何做一个CRD for MySQL。第一步的想法是实现一个MySQL单机服务的安装，暂时不考虑主从或者组复制集群。

## 1. 单实例MySQL的安装

要安装单机的MySQL，需要这些资源做支持：

* PV & PVC : 实现数据的持久化
* configMap : 实现配置的持久化
* StatefulSet : 这是一种特殊的Pod，一般有状态的服务都要使用StatefulSet
* headless Service : 无头服务
* Service : 提供对外访问的能力，如果我们只是验证安装，那么这个可以暂时省略掉

因此controller的代码中主要的逻辑就是去创建这些资源。

生成CR的sample文件在resources/sample下。