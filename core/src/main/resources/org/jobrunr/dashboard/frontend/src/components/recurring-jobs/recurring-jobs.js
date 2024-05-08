import {useEffect, useState} from 'react';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import Checkbox from '@mui/material/Checkbox';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Button from '@mui/material/Button';
import ButtonGroup from '@mui/material/ButtonGroup';
import Grid from '@mui/material/Grid';
import TimeAgo from "react-timeago/lib";
import cronstrue from 'cronstrue';
import Box from "@mui/material/Box";
import {Snackbar} from "@mui/material";
import Alert from '@mui/material/Alert'
import LoadingIndicator from "../LoadingIndicator";
import VersionFooter from "../utils/version-footer";
import {useLocation, useNavigate} from "react-router-dom";
import TablePagination from "@mui/material/TablePagination";
import JobLabel from "../utils/job-label";
import {JobRunrProNotice} from "../utils/jobrunr-pro-notice";
import {ItemsNotFound} from "../utils/items-not-found";
import {formatDuration} from "../../utils/helper-functions"

const RecurringJobs = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const urlSearchParams = new URLSearchParams(location.search);
    const page = urlSearchParams.get('page');
    const [isLoading, setIsLoading] = useState(true);
    const [recurringJobPage, setRecurringJobPage] = useState({total: 0, limit: 20, currentPage: 0, items: []});
    const [recurringJobs, setRecurringJobs] = useState([{}]);
    const [apiStatus, setApiStatus] = useState(null);

    useEffect(() => {
        getRecurringJobs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page]);

    const getRecurringJobs = () => {
        setIsLoading(true);
        const offset = (page) * 20;
        const limit = 20;
        fetch(`/api/recurring-jobs?offset=${offset}&limit=${limit}`)
            .then(res => res.json())
            .then(response => {
                setRecurringJobPage(response);
                setRecurringJobs(response.items.map(recurringJob => ({...recurringJob, selected: false})));
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    };

    const handleChangePage = (event, newPage) => {
        let urlSearchParams = new URLSearchParams(location.search);
        urlSearchParams.set("page", newPage);
        navigate(`?${urlSearchParams.toString()}`);
    };

    const selectAll = (event) => {
        if (event.target.checked) {
            setRecurringJobs(recurringJobs.map(recurringJob => ({...recurringJob, selected: true})));
        } else {
            setRecurringJobs(recurringJobs.map(recurringJob => ({...recurringJob, selected: false})));
        }
    }

    const selectRecurringJob = (event, updatedRecurringJob) => {
        setRecurringJobs(recurringJobs.map(recurringJob => {
            if (recurringJob.id === updatedRecurringJob.id) {
                return ({...recurringJob, selected: !recurringJob.selected});
            } else {
                return recurringJob;
            }
        }))
    };

    const handleCloseAlert = (event, reason) => {
        setApiStatus(null);
    };

    const deleteSelectedRecurringJobs = () => {
        Promise.all(
            recurringJobs
                .filter(recurringJob => recurringJob.selected)
                .map(recurringJob => fetch(`/api/recurring-jobs/${recurringJob.id}`, {method: 'DELETE'}))
        ).then(responses => {
            const succeeded = responses.every(response => response.status === 204);
            if (succeeded) {
                setApiStatus({type: 'deleted', severity: 'success', message: 'Successfully deleted recurring jobs'});
                getRecurringJobs();
            } else {
                setApiStatus({
                    type: 'deleted',
                    severity: 'error',
                    message: 'Error deleting recurring jobs - please refresh the page'
                });
            }
        })
    };

    const triggerSelectedRecurringJobs = () => {
        Promise.all(
            recurringJobs
                .filter(recurringJob => recurringJob.selected)
                .map(recurringJob => fetch(`/api/recurring-jobs/${recurringJob.id}/trigger`, {method: 'POST'}))
        ).then(responses => {
            const succeeded = responses.every(response => response.status === 204);
            if (succeeded) {
                setApiStatus({
                    type: 'triggered',
                    severity: 'success',
                    message: 'Successfully triggered recurring jobs'
                });
            } else {
                setApiStatus({
                    type: 'triggered',
                    severity: 'error',
                    message: 'Error triggering recurring jobs - please refresh the page'
                });
            }
        })
    };

    return (
        <div>
            <Box my={3}>
                <Typography variant="h4">Recurring Jobs</Typography>
            </Box>

            {isLoading
                ? <LoadingIndicator/>
                : <>
                    <JobRunrProNotice>Do you want to pause a recurring job? With <a
                        href="https://www.jobrunr.io/en/documentation/pro/" target="_blank" rel="noreferrer"
                        title="Support the development of JobRunr by getting a Pro license!">JobRunr Pro</a> that's just
                        a click away.
                    </JobRunrProNotice>
                    <Paper>
                        {recurringJobs.length < 1
                            ? <ItemsNotFound>No recurring jobs found</ItemsNotFound>
                            : <>
                                <Grid item xs={3} container>
                                    <ButtonGroup
                                        style={{margin: '1rem'}}
                                        disabled={recurringJobs.every(recurringJob => !recurringJob.selected)}
                                    >
                                        <Button variant="outlined" color="primary"
                                                onClick={triggerSelectedRecurringJobs}>
                                            Trigger
                                        </Button>
                                        <Button variant="outlined" color="primary"
                                                onClick={deleteSelectedRecurringJobs}>
                                            Delete
                                        </Button>
                                    </ButtonGroup>
                                </Grid>
                                <TableContainer>
                                    <Table aria-label="recurring jobs overview">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell padding="checkbox">
                                                    <Checkbox
                                                        checked={recurringJobs.every(recurringJob => recurringJob.selected)}
                                                        onClick={selectAll}/>
                                                </TableCell>
                                                <TableCell>Id</TableCell>
                                                <TableCell>Job name</TableCell>
                                                <TableCell>Cron</TableCell>
                                                <TableCell>Time zone</TableCell>
                                                <TableCell>Next run</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {recurringJobs.map(recurringJob => {
                                                console.log("Recurring Job: ", recurringJob) //TODO: remove this line (debugging)
                                                const {nextRun} = recurringJob;
                                                console.log("nextRun: ", nextRun)
                                                let nextRunDisplay;

                                                // Check if nextRun has the instant or carbonAwarePeriod field
                                                if (nextRun.instant) {
                                                    const instantDate = new Date(nextRun.instant);
                                                    nextRunDisplay = (
                                                        <TimeAgo date={instantDate} title={instantDate.toString()}/>
                                                    );
                                                } else if (nextRun.carbonAwarePeriod) {
                                                    const fromDate = new Date(nextRun.carbonAwarePeriod.from);
                                                    const toDate = new Date(nextRun.carbonAwarePeriod.to);
                                                    nextRunDisplay = (
                                                        <div>
                                                            <TimeAgo date={fromDate} title={fromDate.toString()}/>
                                                            {" - "}
                                                            <TimeAgo date={toDate} title={toDate.toString()}/>
                                                        </div>
                                                    );
                                                } else {
                                                    nextRunDisplay = <span>No data</span>;
                                                }

                                                return (
                                                    <TableRow key={recurringJob.id}>
                                                        <TableCell padding="checkbox">
                                                            <Checkbox checked={recurringJob.selected}
                                                                      onClick={(event) => selectRecurringJob(event, recurringJob)}/>
                                                        </TableCell>
                                                        <TableCell component="th" scope="row">
                                                            {recurringJob.id}
                                                        </TableCell>
                                                        <TableCell>
                                                            {recurringJob.labels?.map((label) => <JobLabel
                                                                text={label}/>)}
                                                            {recurringJob.jobName}
                                                        </TableCell>
                                                        <TableCell>
                                                            {recurringJob.schedule['@class'] === 'org.jobrunr.scheduling.cron.CronExpression' ?
                                                                cronstrue.toString(recurringJob.schedule.expression) :
                                                                recurringJob.schedule['@class'] === 'org.jobrunr.scheduling.cron.CarbonAwareCronExpression' ?
                                                                    <>
                                                                        {cronstrue.toString(recurringJob.schedule.cronExpression.expression)}
                                                                        (Allowed to run
                                                                        earlier: {formatDuration(recurringJob.schedule.allowedDurationBefore)} ,
                                                                        Allowed to run
                                                                        later: {formatDuration(recurringJob.schedule.allowedDurationAfter)})
                                                                    </> :
                                                                    recurringJob.scheduleExpression
                                                            }
                                                        </TableCell>
                                                        <TableCell>
                                                            {recurringJob.zoneId}
                                                        </TableCell>
                                                        <TableCell>
                                                            {nextRunDisplay}
                                                        </TableCell>
                                                    </TableRow>
                                                );
                                            })}
                                        </TableBody>

                                    </Table>
                                </TableContainer>
                                <TablePagination
                                    id="recurring-jobs-table-pagination"
                                    component="div"
                                    rowsPerPageOptions={[]}
                                    count={recurringJobPage.total}
                                    rowsPerPage={recurringJobPage.limit}
                                    page={recurringJobPage.currentPage}
                                    onPageChange={handleChangePage}
                                />
                            </>
                        }
                    </Paper>
                    <VersionFooter/>
                </>
            }
            {apiStatus &&
                <Snackbar open={true}
                          autoHideDuration={3000}
                          onClose={handleCloseAlert}
                          anchorOrigin={{vertical: "bottom", horizontal: "center"}}
                >
                    <Alert severity={apiStatus.severity}>
                        {apiStatus.message}
                    </Alert>
                </Snackbar>
            }
        </div>
    )
};

export default RecurringJobs;