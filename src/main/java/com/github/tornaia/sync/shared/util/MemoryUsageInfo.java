package com.github.tornaia.sync.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Component
public class MemoryUsageInfo {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryUsageInfo.class);

    @Scheduled(fixedDelay = 60000)
    public void logUsage() {
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memUsage = memoryMxBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMxBean.getNonHeapMemoryUsage();
        LOG.debug("HeapMemoryUsage: " + memUsage);
        LOG.debug("NonHeapMemoryUsage: " + nonHeapMemoryUsage);
    }
}
