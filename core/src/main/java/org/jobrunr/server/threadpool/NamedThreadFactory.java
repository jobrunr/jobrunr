package org.jobrunr.server.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

final  class NamedThreadFactory implements ThreadFactory {

  private final String poolName;
  private final ThreadFactory factory;

  NamedThreadFactory(final String poolName) {
    super();
    this.poolName = requireNonNull(poolName);
    factory = Executors.defaultThreadFactory();
  }

  @Override
  public Thread newThread(final Runnable runnable) {
    final Thread thread = factory.newThread(runnable);
    thread.setName(thread.getName().replace("pool", poolName));
    thread.setDaemon(true);
    return thread;
  }
}