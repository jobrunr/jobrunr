import {useEffect, useState} from 'react';
import Box from "@mui/material/Box";
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import RealtimeGraph from "./cards/realtime-graph";
import EstimatedProcessingTimeCard from "./cards/estimated-processing-time-card";
import UptimeCard from "./cards/uptime-card";
import NbrOfBackgroundJobServersCard from "./cards/number-of-background-job-servers-card";
import AvgSystemCpuLoadCard from "./cards/avg-system-cpu-load-card";
import AvgProcessMemoryUsageCard from "./cards/avg-process-memory-usage-card";
import AvgProcessFreeMemoryCard from "./cards/avg-process-free-memory-card";
import LoadingIndicator from "../LoadingIndicator";
import Problems from "./problems/problems-notifications";
import VersionFooter from "../utils/version-footer";

const Overview = () => {
    const [isServersApiLoading, setServersApiIsLoading] = useState(true);
    const [servers, setServers] = useState([{firstHeartbeat: undefined}]);

    useEffect(() => {
        fetch(`/api/servers`)
            .then(res => res.json())
            .then(response => {
                setServers(sortServers(response));
                setServersApiIsLoading(false);
            })
            .catch(error => console.log(error));

        const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/servers");
        eventSource.addEventListener('message', e => setServers(sortServers(JSON.parse(e.data))));
        eventSource.addEventListener('close', e => eventSource.close());
        return function cleanUp() {
            eventSource.close();
        }
    }, []);

    const sortServers = (servers) => {
        servers.sort((a, b) => a.firstHeartbeat > b.firstHeartbeat)
        return servers;
    }

    return (
        <div className="app">
            <div className="row">
                <Box my={3}>
                    <Typography id="title" variant="h4">Dashboard</Typography>
                </Box>
            </div>
            <Problems/>
            <div style={{display: "flex"}}>
                {isServersApiLoading
                    ? <LoadingIndicator/>
                    : <> {servers.length > 0
                        ? <>
                            <EstimatedProcessingTimeCard/>
                            <UptimeCard servers={servers}/>
                            <NbrOfBackgroundJobServersCard servers={servers}/>
                            <AvgSystemCpuLoadCard servers={servers}/>
                            <AvgProcessMemoryUsageCard servers={servers}/>
                            <AvgProcessFreeMemoryCard servers={servers}/>
                        </>
                        : <Paper style={{marginTop: '1rem', padding: '1rem', width: '100%'}}>
                            <Typography id="no-servers-found-message" variant="body1">
                                No background job server available - jobs will not be processed.
                            </Typography>
                        </Paper>
                    }
                    </>
                }
            </div>
            <RealtimeGraph/>
            <VersionFooter/>
        </div>
    );
};

export default Overview;