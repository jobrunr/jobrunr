import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Box from "@material-ui/core/Box";
import Typography from '@material-ui/core/Typography';
import RealtimeGraph from "./cards/realtime-graph";
import EstimatedProcessingTimeCard from "./cards/estimated-processing-time-card";
import UptimeCard from "./cards/uptime-card";
import AvgProcessCpuLoadCard from "./cards/avg-process-cpu-load-card";
import AvgProcessMemoryUsageCard from "./cards/avg-process-memory-usage-card";
import AvgProcessFreeMemoryCard from "./cards/avg-process-free-memory-card";
import LoadingIndicator from "../LoadingIndicator";

const useStyles = makeStyles(theme => ({
    metadata: {
        display: 'flex',
    }
}));

const Overview = () => {
    const classes = useStyles();

    const [isLoading, setIsLoading] = React.useState(true);
    const [servers, setServers] = React.useState([{firstHeartbeat: undefined}]);


    React.useEffect(() => {
        fetch(`/api/servers`)
            .then(res => res.json())
            .then(response => {
                const serversSorted = response;
                serversSorted.sort((a, b) => a.firstHeartbeat > b.firstHeartbeat)
                setServers(serversSorted);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    return (
        <div className="app">
            <div className="row">
                <Box my={3}>
                    <Typography id="title" variant="h4">Dashboard</Typography>
                </Box>
            </div>
            <div className={classes.metadata}>
                {isLoading
                    ? <LoadingIndicator/>
                    : <>
                        <EstimatedProcessingTimeCard/>
                        <UptimeCard servers={servers}/>
                        <AvgProcessCpuLoadCard servers={servers}/>
                        <AvgProcessMemoryUsageCard servers={servers}/>
                        <AvgProcessFreeMemoryCard servers={servers}/>
                    </>
                }
            </div>
            <RealtimeGraph/>
        </div>
    );
};

export default Overview;