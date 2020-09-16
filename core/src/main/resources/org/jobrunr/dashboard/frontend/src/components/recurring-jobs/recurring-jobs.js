import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import Typography from '@material-ui/core/Typography';
import Table from '@material-ui/core/Table';
import Checkbox from '@material-ui/core/Checkbox';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import Grid from '@material-ui/core/Grid';
import TimeAgo from "react-timeago/lib";
import cronstrue from 'cronstrue';
import Box from "@material-ui/core/Box";
import {Snackbar} from "@material-ui/core";
import Alert from '@material-ui/lab/Alert'
import LoadingIndicator from "../LoadingIndicator";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    recurringJobActions: {
        margin: '1rem',
    },
    noItemsFound: {
        padding: '1rem'
    },
}));

const RecurringJobs = (props) => {
    const classes = useStyles();

    const [isLoading, setIsLoading] = React.useState(true);
    const [recurringJobs, setRecurringJobs] = React.useState([{}]);
    const [apiStatus, setApiStatus] = React.useState(null);

    React.useEffect(() => {
        getRecurringJobs();
    }, []);

    const getRecurringJobs = () => {
        fetch(`/api/recurring-jobs`)
            .then(res => res.json())
            .then(response => {
                setRecurringJobs(response.map(recurringJob => ({...recurringJob, selected: false})));
                setIsLoading(false);
            })
            .catch(error => console.log(error));
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
                : <Paper className={classes.paper}>
                    {recurringJobs.length < 1
                        ? <Typography variant="body1" className={classes.noItemsFound}>No recurring jobs
                            found</Typography>
                        : <>
                            <Grid item xs={3} container>
                                <ButtonGroup className={classes.recurringJobActions}
                                             disabled={recurringJobs.every(recurringJob => !recurringJob.selected)}>
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
                                <Table className={classes.table} aria-label="recurring jobs overview">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell padding="checkbox">
                                                <Checkbox
                                                    checked={recurringJobs.every(recurringJob => recurringJob.selected)}
                                                    onClick={selectAll}/>
                                            </TableCell>
                                            <TableCell className={classes.idColumn}>Id</TableCell>
                                            <TableCell>Job name</TableCell>
                                            <TableCell>Cron</TableCell>
                                            <TableCell>Time zone</TableCell>
                                            <TableCell>Next run</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {recurringJobs.map(recurringJob => (
                                            <TableRow key={recurringJob.id}>
                                                <TableCell padding="checkbox">
                                                    <Checkbox checked={recurringJob.selected}
                                                              onClick={(event) => selectRecurringJob(event, recurringJob)}/>
                                                </TableCell>
                                                <TableCell component="th" scope="row" className={classes.idColumn}>
                                                    {recurringJob.id}
                                                </TableCell>
                                                <TableCell>
                                                    {recurringJob.jobName}
                                                </TableCell>
                                                <TableCell>
                                                    {cronstrue.toString(recurringJob.cronExpression)}
                                                </TableCell>
                                                <TableCell>
                                                    {recurringJob.zoneId}
                                                </TableCell>
                                                <TableCell>
                                                    <TimeAgo date={new Date(recurringJob.nextRun)}
                                                             title={new Date(recurringJob.nextRun).toString()}/>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </>
                    }
                </Paper>
            }
            {apiStatus &&
            <Snackbar open={true} autoHideDuration={3000} onClose={handleCloseAlert}>
                <Alert severity={apiStatus.severity}>
                    {apiStatus.message}
                </Alert>
            </Snackbar>
            }
        </div>
    )
};

export default RecurringJobs;