package com.vastdata;

import com.vastdata.constants.AccessModeEnum;
import com.vastdata.constants.ApiVersionEnum;
import com.vastdata.constants.ResourceKindEnum;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlReplicaReconciler implements Reconciler<MysqlReplica> { 
  private final KubernetesClient client;

  public MysqlReplicaReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<MysqlReplica> reconcile(MysqlReplica resource, Context context) {
    var spec = resource.getSpec();
    // TODO: fill in logic
    var secretResource = client.resources(Secret.class).withName(resource.getSpec().getSecretName());
    final var secret = secretResource.get().getData().get("ROOT_PASSWORD");
    // Step 1 新建PV
    final var persistentVolume = buildPersistentVolume(resource);
    // Step 2 新建PVC
    final var persistentVolumeClaim = buildPersistentVolumeClaim(resource);

    // Step 3 新建configMap
    final var configMap = buildConfigMap(resource);

    // Step 4 新建Pod-StatefulSet
    final var statefulSet = buildStatefulSet(resource, secret);
    // Step 5 新建Headless Service
    final var headlessService = buildHeadlessService(resource);

    // 判断这些资源是不是存在，可以用断言，代码好看一些
    var pvcExisting
            = client.resources(PersistentVolumeClaim.class).withName(spec.getPvcName());
    var pvExisting
            = client.resources(PersistentVolume.class).withName(spec.getPvcName());
    var cmExisting
            = client.resources(ConfigMap.class).withName(spec.getConfigMapName());
    var ssExisting
            = client.resources(StatefulSet.class).withName(spec.getStatefulSetName());
    var headlessServiceExisting
            = client.resources(Service.class).withName(spec.getHeadlessServiceName());
    // 资源不存在就创建

    return UpdateControl.noUpdate();
  }

  private Service buildHeadlessService(MysqlReplica mysqlReplica) {
    var mysqlReplicaSpec = mysqlReplica.getSpec();
    var objectMeta = new ObjectMeta();
    objectMeta.setName(mysqlReplicaSpec.getHeadlessServiceName());

    var servicePort = new ServicePort();
    servicePort.setName("mysql");
    servicePort.setPort(3306);
    var servicePorts = new ArrayList<ServicePort>();
    servicePorts.add(servicePort);

    var label = new HashMap<String, String>();
    label.put("app", "mysql");
    var serviceSpec = new ServiceSpec();
    serviceSpec.setSelector(label);
    serviceSpec.setPorts(servicePorts);

    var service = new Service();
    service.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    service.setKind(ResourceKindEnum.SERVICE.getKind());
    service.setMetadata(objectMeta);
    service.setSpec(serviceSpec);

    return service;
  }

  private StatefulSet buildStatefulSet(MysqlReplica mysqlReplica, String secret) {
    MysqlReplicaSpec mysqlReplicaSpec = mysqlReplica.getSpec();

    // 元信息。主要是statefulSet的名称
    var objectMeta = new ObjectMeta();
    objectMeta.setName(mysqlReplicaSpec.getStatefulSetName());

    // spec.selector信息
    Map<String, String> label = new HashMap<>();
    label.put("app", "mysql");
    var labelSelector = new LabelSelector();
    labelSelector.setMatchLabels(label);

    // spec.template的信息
    var podMeta = new ObjectMeta();
    podMeta.setLabels(label);

    // spec.container.env信息
    var envVar = new EnvVar();
    envVar.setName("MYSQL_ROOT_PASSWORD");
    envVar.setValue(secret);
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(envVar);

    // spec.container.resource信息
    Map<String, Quantity> memLimits = new HashMap<>();
    Map<String, Quantity> cpuLimits = new HashMap<>();
    memLimits.put("memory", new Quantity("256Mi"));
    cpuLimits.put("cpu", new Quantity("500m"));
    var resourceRequirements = new ResourceRequirements();
    resourceRequirements.setLimits(memLimits);
    resourceRequirements.setLimits(cpuLimits);

    // spec.container.ports信息
    var containerPort = new ContainerPort();
    containerPort.setContainerPort(mysqlReplicaSpec.getContainerPort());
    var containerPorts = new ArrayList<ContainerPort>();
    containerPorts.add(containerPort);

    // spec.container.volumeMounts信息
    var volumeMounts = new ArrayList<VolumeMount>();

    // 数据目录的挂载
    var storageVolumeMount = new VolumeMount();
    storageVolumeMount.setName(mysqlReplicaSpec.getMysqlPersistentStorageMountName());
    storageVolumeMount.setMountPath(mysqlReplicaSpec.getMysqlPersistentStorageMountPath());

    // 配置文件的挂载
    var cnfVolumeMount = new VolumeMount();
    cnfVolumeMount.setName(mysqlReplicaSpec.getMysqlCnfMountName());
    cnfVolumeMount.setMountPath(mysqlReplicaSpec.getMysqlCnfMountPath());
    cnfVolumeMount.setSubPath(mysqlReplicaSpec.getMysqlCnfMountSubPath());

    volumeMounts.add(storageVolumeMount);
    volumeMounts.add(cnfVolumeMount);

    // spec.container信息
    var container = new Container();
    container.setName(mysqlReplicaSpec.getImage());
    container.setImage(mysqlReplicaSpec.getImage());
    container.setEnv(envVars);
    container.setResources(resourceRequirements);
    container.setPorts(containerPorts);
    container.setVolumeMounts(volumeMounts);

    var containers = new ArrayList<Container>();
    containers.add(container);

    // spec.volumes信息
    // PVC的挂载
    var persistentVolumeClaimVolumeSource = new PersistentVolumeClaimVolumeSource();
    persistentVolumeClaimVolumeSource.setClaimName(mysqlReplicaSpec.getPvcName());
    var pvcVolume = new Volume();
    pvcVolume.setName(mysqlReplicaSpec.getMysqlPersistentStorageMountName());
    pvcVolume.setPersistentVolumeClaim(persistentVolumeClaimVolumeSource);

    // cnf文件的挂载
    var keyToPath = new KeyToPath();
    keyToPath.setKey("my.cnf");
    keyToPath.setPath(mysqlReplicaSpec.getMysqlCnfMountSubPath());
    var keyToPaths = new ArrayList<KeyToPath>();
    keyToPaths.add(keyToPath);

    var configMapVolumeSource = new ConfigMapVolumeSource();
    configMapVolumeSource.setName(mysqlReplicaSpec.getVolumeConfigName());
    configMapVolumeSource.setItems(keyToPaths);

    var mycnfVolume = new Volume();
    mycnfVolume.setConfigMap(configMapVolumeSource);

    var volumes = new ArrayList<Volume>();
    volumes.add(mycnfVolume);
    volumes.add(pvcVolume);

    // template.spec的配置
    var podSpec = new PodSpec();
    podSpec.setContainers(containers);
    podSpec.setVolumes(volumes);

    // template配置
    var podTemplate = new PodTemplateSpec();
    podTemplate.setMetadata(podMeta);
    podTemplate.setSpec(podSpec);

    // spec配置
    var spec = new StatefulSetSpec();
    spec.setReplicas(mysqlReplicaSpec.getReplicas());
    spec.setServiceName(mysqlReplicaSpec.getHeadlessServiceName());
    spec.setSelector(labelSelector);
    spec.setTemplate(podTemplate);

    var statefulSet = new StatefulSet();
    statefulSet.setApiVersion(ApiVersionEnum.APPS_V1.getApiVersion());
    statefulSet.setKind(ResourceKindEnum.STATEFUL_SET.getKind());
    statefulSet.setMetadata(objectMeta);
    statefulSet.setSpec(spec);

    return statefulSet;
  }

  private ConfigMap buildConfigMap(MysqlReplica mysqlReplica) {
    MysqlReplicaSpec mysqlReplicaSpec = mysqlReplica.getSpec();

    Map<String, String> labels = new HashMap<>();
    labels.put("app", mysqlReplicaSpec.getConfigMapName());
    var objectMeta = new ObjectMeta();
    objectMeta.setName(mysqlReplicaSpec.getConfigMapName());
    objectMeta.setLabels(labels);

    Map<String, String> configData = new HashMap<>();
    configData.put("my.cnf", mysqlReplicaSpec.getConfigData());

    var configMap = new ConfigMap();
    configMap.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    configMap.setMetadata(objectMeta);
    configMap.setKind(ResourceKindEnum.CONFIG_MAP.getKind());
    configMap.setData(configData);

    return configMap;
  }

  private PersistentVolumeClaim buildPersistentVolumeClaim(MysqlReplica mysqlReplica) {
    var mysqlReplicaSpec = mysqlReplica.getSpec();

    // resource定义部分
    var resourceRequirements = new ResourceRequirements();
    var quantity = new Quantity();
    quantity.setAmount(mysqlReplicaSpec.getPvcStorageSize());
    Map<String, Quantity> capacity = new HashMap<>();
    capacity.put("storage", quantity);
    resourceRequirements.setRequests(capacity);

    // accessMode部分
    List<String> accessModes = new ArrayList<>();
    accessModes.add(AccessModeEnum.RWO.getAccessMode());

    // spec部分
    var spec = new PersistentVolumeClaimSpec();
    spec.setStorageClassName(mysqlReplicaSpec.getStorageClassName());
    spec.setAccessModes(accessModes);
    spec.setResources(resourceRequirements);

    // meta部分
    var objectMeta = new ObjectMeta();
    objectMeta.setName(mysqlReplicaSpec.getPvcName());

    var persistentVolumeClaim = new PersistentVolumeClaim();
    persistentVolumeClaim.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    persistentVolumeClaim.setKind(ResourceKindEnum.PVC.getKind());
    persistentVolumeClaim.setMetadata(objectMeta);
    persistentVolumeClaim.setSpec(spec);

    return persistentVolumeClaim;
  }

  private PersistentVolume buildPersistentVolume(MysqlReplica replica) {
    var mysqlReplicaSpec = replica.getSpec();

    // 容量信息
    var quantity = new Quantity();
    quantity.setAmount(mysqlReplicaSpec.getPvcStorageSize());
    Map<String, Quantity> capacity = new HashMap<>();
    capacity.put("storage", quantity);

    // 访问方式
    List<String> accessModes = new ArrayList<>();
    accessModes.add(AccessModeEnum.RWO.getAccessMode());

    // 路径信息
    var hostPathVolumeSource = new HostPathVolumeSource();
    hostPathVolumeSource.setPath(mysqlReplicaSpec.getHostPath().get("path"));

    // PV的spec信息
    var spec = new PersistentVolumeSpec();
    spec.setStorageClassName(mysqlReplicaSpec.getStorageClassName());
    spec.setHostPath(hostPathVolumeSource);
    spec.setAccessModes(accessModes);
    spec.setCapacity(capacity);

    var objectMeta = new ObjectMeta();
    objectMeta.setName(mysqlReplicaSpec.getPvcName());
    // PV声明头
    var persistentVolume = new PersistentVolume();
    persistentVolume.setApiVersion(ApiVersionEnum.V1.getApiVersion());
    persistentVolume.setKind(ResourceKindEnum.PV.getKind());
    persistentVolume.setSpec(spec);
    persistentVolume.setMetadata(objectMeta);

    return persistentVolume;
  }
}

