import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Box from "@material-ui/core/Box";
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import RealtimeGraph from "./cards/realtime-graph";
import EstimatedProcessingTimeCard from "./cards/estimated-processing-time-card";
import UptimeCard from "./cards/uptime-card";
import NbrOfBackgroundJobServersCard from "./cards/number-of-background-job-servers-card";
import AvgSystemCpuLoadCard from "./cards/avg-system-cpu-load-card";
import AvgProcessMemoryUsageCard from "./cards/avg-process-memory-usage-card";
import AvgProcessFreeMemoryCard from "./cards/avg-process-free-memory-card";
import LoadingIndicator from "../LoadingIndicator";
import {Alert, AlertTitle} from '@material-ui/lab';

const useStyles = makeStyles(theme => ({
    alert: {
        width: '100%',
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    },
    metadata: {
        display: 'flex',
    },
    noServersFound: {
        marginTop: '1rem',
        padding: '1rem',
        width: '100%'
    },
}));

const Overview = () => {
    const classes = useStyles();

    const [isProblemsApiLoading, setProblemsApiIsLoading] = React.useState(true);
    const [isServersApiLoading, setServersApiIsLoading] = React.useState(true);
    const [problems, setProblems] = React.useState([]);
    const [servers, setServers] = React.useState([{firstHeartbeat: undefined}]);


    React.useEffect(() => {
        fetch(`/api/problems`)
            .then(res => res.json())
            .then(response => {
                setProblems(response);
                setProblemsApiIsLoading(false);
            })
            .catch(error => console.log(error));

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
            <div className={classes.metadata}>
                {isProblemsApiLoading
                    ? <LoadingIndicator/>
                    : <> {problems.map((problem, index) => {
                        switch (problem.type) {
                            case 'jobs-not-found':
                                return <Alert key={index} className={classes.alert} severity="warning">
                                    <AlertTitle><h4 className={classes.alertTitle}>Warning</h4></AlertTitle>
                                    There are SCHEDULED jobs that do not exist anymore in your code. These jobs will
                                    fail with a <strong>JobNotFoundException</strong> (due to a ClassNotFoundException
                                    or a MethodNotFoundException).<br/>
                                </Alert>
                            default:
                                return <div key={index}>Unknown error</div>
                        }
                    })}
                    </>
                }
            </div>
            <div className={classes.metadata}>
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
                        : <Paper className={classes.noServersFound}>
                            <Typography id="no-servers-found-message" variant="body1">
                                No background job server available - jobs will not be processed.
                            </Typography>
                        </Paper>
                    }
                    </>
                }
            </div>
            <RealtimeGraph/>
        </div>
    );
};

export default Overview;