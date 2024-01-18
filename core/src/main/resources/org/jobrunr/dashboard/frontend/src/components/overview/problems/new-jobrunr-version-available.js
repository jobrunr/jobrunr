import { Alert, AlertTitle } from '@mui/material';

const classes = {
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
};

const NewJobRunrVersionAvailable = (props) => {
    return (
        <Alert style={classes.alert} severity="info">
            <AlertTitle><h4 style={classes.alertTitle}>Info</h4></AlertTitle>
            JobRunr version {props.problem.latestVersion} is available. Please upgrade JobRunr as it brings bugfixes, performance improvements and new features.<br/>
        </Alert>
    );
};

export default NewJobRunrVersionAvailable;