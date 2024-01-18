import Alert from '@mui/material/Alert';
import Paper from "@mui/material/Paper";
import Grid from "@mui/material/Grid";

const classes = {
    alert: {
        fontSize: '1rem'
    }
};

const JobDetailsNotCacheableNotification = (props) => {
    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="error" style={classes.alert}>
                    <strong>Job details not cacheable!</strong>&nbsp;The analysis for this job cannot be cached as the provided lambda is too complex. This means that <b>enqueueing jobs take more time</b>. More info can be found on the <a href="https://www.jobrunr.io/en/documentation/background-methods/best-practices/" target="_blank" rel="noreferrer">best practices page</a> of JobRunr.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default JobDetailsNotCacheableNotification;