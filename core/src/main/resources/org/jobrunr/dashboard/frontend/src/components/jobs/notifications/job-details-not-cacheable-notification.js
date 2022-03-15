import Alert from "@material-ui/lab/Alert";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";

const useStyles = makeStyles(() => ({
    alert: {
        fontSize: '1rem'
    }
}));

const JobDetailsNotCacheableNotification = (props) => {
    const classes = useStyles();

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="error" className={classes.alert}>
                    <strong>Job details not cacheable!</strong>&nbsp;The analysis for this job cannot be cached as the provided lambda is too complex. This means that <b>enqueueing jobs take more time</b>. More info can be found on the <a href="https://www.jobrunr.io/en/documentation/background-methods/best-practices/" target="_blank" rel="noreferrer">best practices page</a> of JobRunr.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default JobDetailsNotCacheableNotification;