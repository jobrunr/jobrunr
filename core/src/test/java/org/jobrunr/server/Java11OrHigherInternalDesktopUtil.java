package org.jobrunr.server;

import java.awt.*;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.time.Instant;

public class Java11OrHigherInternalDesktopUtil implements DesktopUtils.Internal, SystemSleepListener {

    private boolean isSystemSleepDetectionSupported = false;
    private Instant lastSystemAwake = Instant.MIN;

    public Java11OrHigherInternalDesktopUtil() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_EVENT_SYSTEM_SLEEP)) {
                desktop.addAppEventListener(this);
                isSystemSleepDetectionSupported = true;
            }
        } catch (Throwable e) {
            // being carefull here - what if SDK does not support java.awt?
        }
    }

    @Override
    public boolean supportsSystemSleepDetection() {
        return isSystemSleepDetectionSupported;
    }

    @Override
    public Instant getLastSystemAwakeTime() {
        return lastSystemAwake;
    }

    @Override
    public void systemAboutToSleep(SystemSleepEvent e) {
        // nothing to do
    }

    @Override
    public void systemAwoke(SystemSleepEvent e) {
        lastSystemAwake = Instant.now();
    }
}
