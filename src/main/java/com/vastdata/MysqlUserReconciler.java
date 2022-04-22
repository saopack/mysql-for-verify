package com.vastdata;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class MysqlUserReconciler implements Reconciler<MysqlUser> { 
  private final KubernetesClient client;

  public MysqlUserReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<MysqlUser> reconcile(MysqlUser resource, Context context) {
    // TODO: fill in logic
    return UpdateControl.noUpdate();
  }
}

