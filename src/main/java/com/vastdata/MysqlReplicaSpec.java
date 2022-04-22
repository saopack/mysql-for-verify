package com.vastdata;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class MysqlReplicaSpec {

    // Add Spec information here
    private Integer replicas;

    /**
     * 响应前置步骤中的Secret创建
     */
    private String secretName;

    /**
     * mysql镜像，采用这种格式 mysql:5.7.37
     */
    private String image;

    /**
     * 容器的资源限制，一般限制memory和cpu
     */
    private Map<String, String> resourceLimits;

    private Integer containerPort;

    /**
     * 容器的挂载点
     */
    private List<VolumeMount> volumeMounts;

    /**
     * mysql的配置，都是以键值对形式出现的，下面是一些示例，具体要配置什么请参考官方文档：
     * <p></p>
     * innodb-buffer-pool-size: 128M
     * <p></p>
     * transaction-isolation: READ-COMMITTED
     * <p>作为样例，我也会在sample目录下提供</p>
     */
    private Map<String, String> myConf;

    /**
     * 定义PVC的容量
     */
    private String pvcStorageSize;

    /**
     * 存储卷的访问模式，有三种，
     * <li>ReadWriteOnce: 可以被一个节点读写挂载</li>
     * <li>ReadOnlyMany: 可以被多个节点只读挂载</li>
     * <li>ReadWriteMany: 可以被多个节点读写挂载</li>
     */
    private String[] accessModes;

    private String storageClassName;

    /**
     * 要挂载的路径，这是挂载本地盘用的，作为演示就用本地盘了，不考虑nfs
     */
    private Map<String, String> hostPath;

    private String pvcName;

    private String configMapName;

    private String configData;

    /**
     * mysql无头服务的名称
     */
    private String HeadlessServiceName;

    private String mysqlPersistentStorageMountName;

    private String mysqlCnfMountName;

    private String mysqlPersistentStorageMountPath;

    private String mysqlCnfMountPath;

    private String mysqlCnfMountSubPath;

    private String volumeConfigName;

    private String statefulSetName;
}
