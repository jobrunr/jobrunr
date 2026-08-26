import {useEffect} from 'react';
import RealtimeGraph from "./server-analytics/realtime-graph";
import VersionFooter from "../utils/version-footer";
import {openEventSource} from "../../stores/serversStore";
import {Box, Stack, Typography} from "@mui/material";
import {ServerAnalytics} from "./server-analytics/server-analytics.js";

const Overview = () => {
    useEffect(() => {
        return openEventSource();
    }, []);

    return (
        <div className="app">
            <Box sx={{my: 3}}>
                <Typography id="title" variant="h4">Dashboard</Typography>
            </Box>
            <Stack sx={{gap: 2}}>
                <ServerAnalytics/>
                <RealtimeGraph/>
            </Stack>
            <VersionFooter/>
        </div>
    );
};

export default Overview;