import { useState } from 'react';
import { Alert, AlertTitle } from '@mui/material';
import {Button, Dialog, Link, Snackbar} from "@mui/material";
import MuiDialogTitle from "@mui/material/DialogTitle";
import MuiDialogContent from "@mui/material/DialogContent";
import Highlight from "react-highlight";

const classes = {
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
};

const SevereJobRunrExceptionProblem = (props) => {
    const [copyStatus, setCopyStatus] = useState(null);
    const [issueDialog, setIssueDialogContent] = useState(null);

    const handleCloseSnackbar = (event, reason) => setCopyStatus(null);
    const handleCloseDialog = (event, reason) => setIssueDialogContent(null);

    const dismissProblem = () => {
        fetch(`/api/problems/severe-jobrunr-exception`, {
            method: 'DELETE'
        })
            .then(resp => props.refresh())
            .catch(error => console.log(error));
    }

    const showDialogAsContentCouldNotBeCopied = () => {
        setIssueDialogContent(props.problem.githubIssueBody);
    }

    const copyToClipboard = () => {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(props.problem.githubIssueBody)
                .then(
                    () => setCopyStatus({
                        severity: 'success',
                        message: 'Successfully copied Github issue data to the clipboard'
                    }),
                    showDialogAsContentCouldNotBeCopied
                );
        } else {
            showDialogAsContentCouldNotBeCopied();
        }
    }

    return (
        <Alert style={classes.alert} severity="error" action={
            <Button color="inherit" size="small" onClick={dismissProblem}>
                DISMISS
            </Button>
        }>
            <AlertTitle><h4 style={classes.alertTitle}>Fatal</h4></AlertTitle>
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
                    To make life easy, you can <Link onClick={copyToClipboard} underline="hover">click here</Link> to copy
                    all necessary information to your clipboard and paste it in the Github issue. <br/>
                    <strong>You can still cancel the creation of the issue in Github if you think it contains sensitive
                        information.</strong>.<br/></>
            }
            {copyStatus &&
                <Snackbar open={true}
                    autoHideDuration={3000}
                    onClose={handleCloseSnackbar}
                    anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
                >
                    <Alert severity={copyStatus.severity}>
                        {copyStatus.message}
                    </Alert>
                </Snackbar>
            }
            {issueDialog &&
                <Dialog open={true} onClose={handleCloseDialog} aria-labelledby="customized-dialog-title" >
                    <MuiDialogTitle id="customized-dialog-title" onClose={handleCloseDialog}>
                        Could not copy issue data to clipboard
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        JobRunr could not copy the issue data to the clipboard (are you running an older browser or are you not using https?).
                        Please copy the data below and paste it in the Github issue as is.
                        <Highlight language='yaml'>
                            {issueDialog}
                        </Highlight>
                    </MuiDialogContent>
                </Dialog>
            }
        </Alert>
    );
}

export default SevereJobRunrExceptionProblem;