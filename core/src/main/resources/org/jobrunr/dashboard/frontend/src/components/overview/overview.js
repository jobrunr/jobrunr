import {useEffect} from 'react';
import RealtimeGraph from "./cards/realtime-graph";
import VersionFooter from "../utils/version-footer";
import {openEventSource} from "../../stores/serversStore";
import {Stack} from "@mui/material";
import {ServerAnalytics} from "./cards/server-analytics.js";

const Overview = () => {
    useEffect(() => {
        return openEventSource();
    }, []);

    return (
        <div className="app">
            <Stack sx={{
                gap: 4
            }}>
                <ServerAnalytics/>
                <RealtimeGraph/>
            </Stack>
            <VersionFooter/>
        </div>
    );
};

export default Overview;