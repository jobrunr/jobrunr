package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.HttpHandler;

public interface TeenyHttpHandler extends HttpHandler {

    String getContextPath();

}
