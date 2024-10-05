import {useState} from 'react';
import {Alert, Dialog, Link, Snackbar} from '@mui/material';
import MuiDialogTitle from "@mui/material/DialogTitle";
import MuiDialogContent from "@mui/material/DialogContent";
import Highlight from '../../utils/highlighter';
import {DismissibleInstanceProblemNotification} from "./dismissible-problem-notification";
import Button from "@mui/material/Button";

const SevereJobRunrExceptionProblem = ({problem, hasCpuAllocationIrregularity, refresh}) => {
    const [copyStatus, setCopyStatus] = useState(null);
    const [showIssueDialog, setShowIssueDialog] = useState(false);

    const handleCloseSnackbar = () => setCopyStatus(null);

    const toggleShowIssueDialog = () => {
        setShowIssueDialog(prev => !prev);
    }

    const copyToClipboard = () => {
        const successStatus = {
            severity: 'success',
            message: 'Successfully copied Github issue data to the clipboard'
        }
        const failedStatus = {
            severity: 'error',
            message: 'Failed to copy Github issue data to the clipboard'
        }
        if (navigator.clipboard) {
            navigator.clipboard.writeText(problem.githubIssueBody)
                .then(
                    () => setCopyStatus(successStatus),
                    () => setCopyStatus(failedStatus)
                );
        } else {
            setCopyStatus(failedStatus);
        }
    }

    const issueTitle = `[BUG] ${problem.githubIssueTitle}`;
    const issueLink = problem.githubIssueBodyLength < 2000 ?
        `https://github.com/jobrunr/jobrunr/issues/new?title=${encodeURIComponent(issueTitle)}&body=${encodeURIComponent(problem.githubIssueBody)}&labels=bug`
        : `https://github.com/jobrunr/jobrunr/issues/new?title=${encodeURIComponent(issueTitle)}&body=%3C%21--%20Please%20paste%20the%20issue%20content%20generated%20JobRunr%20below.%20--%3E&labels=bug`;

    return (
        <DismissibleInstanceProblemNotification
            title="Fatal"
            severity="error"
            endpoint="/api/problems/severe-jobrunr-exception"
            refresh={refresh}
        >
            <div>JobRunr encountered an exception that should not happen that could result in the Background Job Servers stopping. <b>You need to look into
                this.</b>
            </div>
            <Button onClick={toggleShowIssueDialog} size="small">More details</Button>
            {copyStatus &&
                <Snackbar open={true}
                          autoHideDuration={3000}
                          onClose={handleCloseSnackbar}
                          anchorOrigin={{vertical: "bottom", horizontal: "center"}}
                >
                    <Alert severity={copyStatus.severity}>
                        {copyStatus.message}
                    </Alert>
                </Snackbar>
            }
            {showIssueDialog &&
                <Dialog maxWidth="md" open={true} onClose={toggleShowIssueDialog} aria-labelledby="severe-jobrunr-exception-dialog-title">
                    <MuiDialogTitle id="severe-jobrunr-exception-dialog-title">
                        {problem.githubIssueTitle}
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        <p>
                            JobRunr encountered the below <b>severe exception</b>. In most cases this relates to a failure in your system.
                            We encourage you to look into it as soon as possible. The following may help you identify the root cause:
                        </p>
                        <ul>
                            {hasCpuAllocationIrregularity &&
                                <li>
                                    The Background Job Servers are showing <i>CPU Allocation Irregularities</i>, there is a high chance that this issue is
                                    linked to it.
                                </li>
                            }
                            <li>If you've configured database replicas, make sure JobRunr reads and writes from the primary node.</li>
                            <li>Make sure your servers have enough resources (memory, CPU, DB connections, etc.) to handle the workload.</li>
                        </ul>
                        <p>
                            Over the years, we've addressed many similar exceptions. Please check that yours is not already addressed
                            <a target="_blank"
                               rel="noopener noreferrer"
                               href={`https://github.com/jobrunr/jobrunr/issues?q=${encodeURIComponent(problem.githubIssueTitle)}`}> over on GitHub</a>.
                        </p>

                        <p>
                            If you still think the failure is caused by JobRunr itself, please report this as a
                            {" "}<a href={issueLink} target="_blank" rel="noopener noreferrer">bug on GitHub</a>.
                            {problem.githubIssueBodyLength < 2000
                                ? <> To make life easy, all necessary information will already be prefilled in the Github issue.</>
                                : <> To make life easy, you can <Link onClick={copyToClipboard} underline="hover">click here</Link> to copy
                                    all necessary information to your clipboard and paste it in the Github issue. Copying did not work? Please copy the data
                                    below and paste it in the Github issue as is.</>
                            }
                            <br/><strong>You can still cancel the creation of the issue in Github if you think it contains sensitive information.</strong>.
                        </p>
                        <p><em>Please also make sure you're following <a
                            target="_blank" rel="noopener noreferrer"
                            href="https://github.com/jobrunr/jobrunr/issues/new?assignees=&labels=&projects=&template=bug_template.yml"> the community
                            guidelines</a> when submitting issues.</em>
                        </p>
                        <Highlight language='yaml'>
                            {problem.githubIssueBody}
                        </Highlight>
                    </MuiDialogContent>
                </Dialog>
            }
        </DismissibleInstanceProblemNotification>
    );
}

export default SevereJobRunrExceptionProblem;