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
import TimeAgo from "react-timeago/lib";
import cronstrue from 'cronstrue';
import Box from "@material-ui/core/Box";
import {CircularProgress} from "@material-ui/core";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    content: {
        width: '100%',
    },
}));

const RecurringJobs = (props) => {
    const classes = useStyles();

    const [isLoading, setIsLoading] = React.useState(true);
    const [recurringJobs, setRecurringJobs] = React.useState({total: 0, limit: 20, currentPage: 0, items: []});
    React.useEffect(() => {
        fetch(`/api/recurring-jobs`)
            .then(res => res.json())
            .then(response => {
                setRecurringJobs(response);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    return (
        <div>
            <Box my={3}>
                <Typography variant="h4">Recurring Jobs</Typography>
            </Box>
            <Paper className={classes.paper}>
                {isLoading
                    ? <CircularProgress/>
                    : <>
                        {recurringJobs.length < 1
                            ? <Typography variant="body1" className={classes.noItemsFound}>No recurring jobs
                                found</Typography>
                            : <>
                                <TableContainer>
                                    <Table className={classes.table} aria-label="recurring jobs overview">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell padding="checkbox">
                                                    <Checkbox/>
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
                                                        <Checkbox/>
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
                                                                 title={new Date(recurringJob.nextRun)}/>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </>
                        }
                    </>
                }
            </Paper>
        </div>
    )
};

export default RecurringJobs;