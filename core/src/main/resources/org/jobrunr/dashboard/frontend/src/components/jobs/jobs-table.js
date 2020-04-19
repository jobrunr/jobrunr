import React from 'react';
import {Link} from "react-router-dom";
import Typography from '@material-ui/core/Typography';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TablePagination from '@material-ui/core/TablePagination';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import Breadcrumbs from '@material-ui/core/Breadcrumbs';
import NavigateNextIcon from '@material-ui/icons/NavigateNext';
import TimeAgo from "react-timeago/lib";
import Box from "@material-ui/core/Box";
import {makeStyles} from '@material-ui/core/styles';
import {CircularProgress} from "@material-ui/core";

const useStyles = makeStyles(theme => ({
    table: {
        width: '100%',
    },
    root: {
        width: '100%',
        //maxWidth: 360,
        backgroundColor: theme.palette.background.paper,
    },
    noItemsFound: {
        padding: '1rem'
    },
    idColumn: {
        maxWidth: 0,
        width: '20%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    jobNameColumn: {
        width: '60%'
    },
    inline: {
        display: 'inline',
    },
}));

const JobsTable = (props) => {
    const classes = useStyles();

    const [page, setPage] = React.useState(0);
    const [isLoading, setIsLoading] = React.useState(true);
    const [jobPage, setJobPage] = React.useState({total: 0, limit: 20, currentPage: 0, items: []});
    const jobState = props.match.params.state;

    let title, column;
    let columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].createdAt;
    switch (jobState) {
        case 'scheduled':
            title = "Scheduled jobs";
            column = "Scheduled";
            columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].scheduledAt;
            break;
        case 'enqueued':
            title = "Enqueued jobs";
            column = "Enqueued";
            break;
        case 'processing':
            title = "Jobs being processed";
            column = "Started";
            break;
        case 'succeeded':
            title = "Succeeded jobs";
            column = "Succeeded";
            break;
        case 'failed':
            title = "Failed jobs";
            column = "Failed";
            break;
        default:
        // code block
    }

    React.useEffect(() => {
        setIsLoading(true);
        let offset = (page) * 20;
        let limit = 20;
        fetch(`/api/jobs/default/${jobState}?offset=${offset}&limit=${limit}`)
            .then(res => res.json())
            .then(response => {
                setJobPage(response);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, [page, jobState]);

    const handleChangePage = (event, newPage) => {
        setPage(newPage);
    };

    return (
        <div>
            <Breadcrumbs id="breadcrumb" separator={<NavigateNextIcon fontSize="small"/>} aria-label="breadcrumb">
                <Link color="inherit" to="/dashboard/jobs">Jobs</Link>
                <Link color="inherit" to="/dashboard/jobs/default/enqueued">Default queue</Link>
                <Typography color="textPrimary">{title}</Typography>
            </Breadcrumbs>
            <Box my={3}>
                <Typography id="title" variant="h4">{title}</Typography>
            </Box>
            <Paper>
                {isLoading
                    ? <CircularProgress/>
                    : <>
                        {jobPage.items < 1
                            ? <Typography id="no-jobs-found-message" variant="body1" className={classes.noItemsFound}>No
                                jobs found</Typography>
                            : <>
                                <TableContainer>
                                    <Table id="jobs-table" className={classes.table} aria-label="jobs table">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell className={classes.idColumn}>Id</TableCell>
                                                <TableCell className={classes.jobNameColumn}>Job details</TableCell>
                                                <TableCell>{column}</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {jobPage.items.map(job => (
                                                <TableRow key={job.id}>
                                                    <TableCell component="th" scope="row" className={classes.idColumn}>
                                                        <Link to={{
                                                            pathname: `/dashboard/jobs/${job.id}`,
                                                            job: job
                                                        }}>{job.id}</Link>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Link to={{
                                                            pathname: `/dashboard/jobs/${job.id}`,
                                                            job: job
                                                        }}>{job.jobName}</Link>
                                                    </TableCell>
                                                    <TableCell>
                                                        <Link to={{pathname: `/dashboard/jobs/${job.id}`, job: job}}>
                                                            <TimeAgo date={new Date(columnFunction(job))}/>
                                                        </Link>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                                <TablePagination
                                    id="jobs-table-pagination"
                                    component="div"
                                    rowsPerPageOptions={[]}
                                    count={jobPage.total}
                                    rowsPerPage={jobPage.limit}
                                    page={jobPage.currentPage}
                                    onChangePage={handleChangePage}
                                />
                            </>
                        }
                    </>
                }
            </Paper>

        </div>
    );
}

export default JobsTable;