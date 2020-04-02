package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.StaticFileHttpHandler;

public class JobRunrStaticFileHandler extends StaticFileHttpHandler {

    public JobRunrStaticFileHandler() {
        super("/dashboard", "/org/jobrunr/dashboard/frontend/build", true);
    }

}
