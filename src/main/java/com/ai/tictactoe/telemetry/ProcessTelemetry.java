package com.ai.tictactoe.telemetry;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

public final class ProcessTelemetry {

    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();
    private static final OperatingSystemMXBean OS_BEAN =
            ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private ProcessTelemetry() {
    }

    public static ProcessSnapshot capture() {
        MemoryUsage heapUsage = MEMORY_BEAN.getHeapMemoryUsage();
        return new ProcessSnapshot(
                System.currentTimeMillis(),
                readProcessCpuLoadPercent(),
                readProcessCpuTimeNanos(),
                heapUsage.getUsed(),
                heapUsage.getCommitted(),
                heapUsage.getMax(),
                THREAD_BEAN.getThreadCount(),
                readGcCount(),
                readGcTimeMillis());
    }

    private static double readProcessCpuLoadPercent() {
        if (OS_BEAN == null) {
            return 0.0;
        }
        double value = OS_BEAN.getProcessCpuLoad();
        if (value < 0) {
            return 0.0;
        }
        return value * 100.0;
    }

    private static long readProcessCpuTimeNanos() {
        return OS_BEAN == null ? 0L : OS_BEAN.getProcessCpuTime();
    }

    private static long readGcCount() {
        long total = 0L;
        for (GarbageCollectorMXBean gcBean : GC_BEANS) {
            long count = gcBean.getCollectionCount();
            if (count >= 0) {
                total += count;
            }
        }
        return total;
    }

    private static long readGcTimeMillis() {
        long total = 0L;
        for (GarbageCollectorMXBean gcBean : GC_BEANS) {
            long time = gcBean.getCollectionTime();
            if (time >= 0) {
                total += time;
            }
        }
        return total;
    }
}
