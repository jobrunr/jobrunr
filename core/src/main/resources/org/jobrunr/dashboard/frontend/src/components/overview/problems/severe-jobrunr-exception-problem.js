import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import {Alert, AlertTitle} from '@material-ui/lab';
import {Button, Link, Snackbar} from "@material-ui/core";

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const SevereJobRunrExceptionProblem = (props) => {
    const classes = useStyles();

    const [copyStatus, setCopyStatus] = React.useState(null);

    const handleCloseSnackbar = (event, reason) => {
        setCopyStatus(null);
    };

    const dismissProblem = () => {
        fetch(`/api/problems/severe-jobrunr-exception`, {
            method: 'DELETE'
        })
            .then(resp => props.refresh())
            .catch(error => console.log(error));
    }

    const copyToClipboard = () => {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(props.problem.githubIssueBody)
                .then(
                    () => setCopyStatus({
                        severity: 'success',
                        message: 'Successfully copied Github issue data to the clipboard'
                    }),
                    () => setCopyStatus({
                        severity: 'error',
                        message: 'Could not copy data to the clipboard. Are you using an old browser?'
                    })
                );
        } else {
            setCopyStatus({
                severity: 'error',
                message: 'Could not copy data to the clipboard. Are you using an old browser?'
            });
        }
    }

    return (
        <Alert className={classes.alert} severity="error" action={
            <Button color="inherit" size="small" onClick={dismissProblem}>
                DISMISS
            </Button>
        }>
            <AlertTitle><h4 className={classes.alertTitle}>Fatal</h4></AlertTitle>
            {props.problem.githubIssueBodyLength < 2000
                ? <>JobRunr encountered an exception that should not happen. To resolve this issue, can you please
                    create a <a
                        href={`https://github.com/jobrunr/jobrunr/issues/new?title=${encodeURIComponent(props.problem.githubIssueTitle)}&body=${encodeURIComponent(props.problem.githubIssueBody)}&labels=bug`}
                        target="_blank" rel="noopener noreferrer">Github issue</a>?
                    To make life easy, all necessary information will already be prefilled in the Github issue. <br/>
                    <strong>You can still cancel the creation of the issue in Github if you think it contains sensitive
                        information.</strong>.<br/></>
                : <>JobRunr encountered an exception that should not happen. To resolve this issue, can you please
                    create a <a
                        href={`https://github.com/jobrunr/jobrunr/issues/new?title=${encodeURIComponent(props.problem.githubIssueTitle)}&body=%3C%21--%20Please%20paste%20the%20issue%20content%20generated%20JobRunr%20below.%20--%3E&labels=bug`}
                        target="_blank" rel="noopener noreferrer">Github issue</a>?
                    To make life easy, you can <Link onClick={copyToClipboard} color="initial">click here</Link> to copy
                    all necessary information to your clipboard and paste it in the Github issue. <br/>
                    <strong>You can still cancel the creation of the issue in Github if you think it contains sensitive
                        information.</strong>.<br/></>
            }
            {copyStatus &&
            <Snackbar open={true} autoHideDuration={3000} onClose={handleCloseSnackbar}>
                <Alert severity={copyStatus.severity}>
                    {copyStatus.message}
                </Alert>
            </Snackbar>
            }
        </Alert>
    );
}

export default SevereJobRunrExceptionProblem;