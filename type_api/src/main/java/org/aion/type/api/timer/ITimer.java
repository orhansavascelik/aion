package org.aion.type.api.timer;

/**
 * Interface so we can create a mock
 *
 * @author yao
 */
public interface ITimer {
    void shutdown();

    void sched(TimerTask timer);
}
