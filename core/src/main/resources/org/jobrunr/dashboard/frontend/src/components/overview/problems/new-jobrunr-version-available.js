import makeStyles from '@mui/styles/makeStyles';
import { Alert, AlertTitle } from '@mui/material';

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const NewJobRunrVersionAvailable = (props) => {
    const classes = useStyles();

    return (
        <Alert className={classes.alert} severity="info">
            <AlertTitle><h4 className={classes.alertTitle}>Info</h4></AlertTitle>
            JobRunr version {props.problem.latestVersion} is available. Please upgrade JobRunr as it brings bugfixes, performance improvements and new features.<br/>
        </Alert>
    );
};

export default NewJobRunrVersionAvailable;