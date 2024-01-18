import Alert from '@mui/material/Alert';
import { useState, useEffect } from "react";
import Paper from "@mui/material/Paper";
import Grid from "@mui/material/Grid";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";


const classes = {
    alert: {
        fontSize: '1rem'
    }
};


const SucceededNotification = (props) => {
    const job = props.job;
    const [serverStats, setServerStats] = useState(serversState.getServers());
    useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    const deleteDuration = serverStats[0].deleteSucceededJobsAfter;
    const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const succeededState = job.jobHistory[job.jobHistory.length - 1]
    const succeededDate = new Date(succeededState.createdAt);
    const deleteDate = new Date(succeededDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" style={classes.alert}>
                    <strong>This job has succeeded.</strong> It will automatically go to the deleted state in <TimeAgo
                    date={deleteDate} title={deleteDate.toString()}/>.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default SucceededNotification;