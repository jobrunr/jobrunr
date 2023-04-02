import React from 'react';
import {Link, useHistory, useLocation} from "react-router-dom";
import Typography from '@material-ui/core/Typography';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TablePagination from '@material-ui/core/TablePagination';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import TimeAgo from "react-timeago/lib";
import {makeStyles} from '@material-ui/core/styles';
import LoadingIndicator from "../LoadingIndicator";
import JobLabel from "./job-label";

const useStyles = makeStyles(theme => ({
    root: {
        width: '100%',
        //maxWidth: 360,
        backgroundColor: theme.palette.background.paper,
    },
    content: {
        width: '100%',
    },
    table: {
        width: '100%',
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
    const location = useLocation();
    const history = useHistory();
    const urlSearchParams = new URLSearchParams(location.search);
    const urlSearchParamsForPro = new URLSearchParams(location.search);
    urlSearchParamsForPro.delete('state');
    urlSearchParamsForPro.delete('page');

    const isLoading = props.isLoading;
    const jobPage = props.jobPage;
    const jobState = props.jobState;

    let column;
    let columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].createdAt;
    switch (jobState) {
        case 'SCHEDULED':
            column = "Scheduled";
            columnFunction = (job) => job.jobHistory[job.jobHistory.length - 1].scheduledAt;
            break;
        case 'ENQUEUED':
            column = "Enqueued";
            break;
        case 'PROCESSING':
            column = "Started";
            break;
        case 'SUCCEEDED':
            column = "Succeeded";
            break;
        case 'FAILED':
            column = "Failed";
            break;
        case 'DELETED':
            column = "Deleted";
            break;
        default:
        // code block
    }

    const handleChangePage = (event, newPage) => {
        urlSearchParams.set("page", newPage);
        history.push(`?${urlSearchParams.toString()}`);
    };

    return (
        <> {isLoading
            ? <LoadingIndicator/>
            : <> {Array.from(urlSearchParamsForPro).length > 0
                ? <Typography id="no-jobs-found-message" variant="body1" className={classes.noItemsFound}>
                    Looking 🔍 for a specific job in JobRunr, but can't search for it in the free version? <a
                    href={'https://www.jobrunr.io/en/get-jobrunr-pro/'} rel={'noreferrer'}
                    target={'_blank'}>Upgrade to JobRunr Pro today</a> and unlock
                    powerful search functionality to easily find the jobs you need. 😎 <br/><br/>
                    Take your job management to the next level 🚀 - <a href={'https://www.jobrunr.io/en/get-jobrunr-pro/'} rel={'noreferrer'}
                                                                      target={'_blank'}>get JobRunr Pro now</a>!
                </Typography>
                : <> {jobPage.items.length < 1
                    ? <Typography id="no-jobs-found-message" variant="body1" className={classes.noItemsFound}>No jobs found</Typography>
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
                                                {job.labels &&
                                                    <>
                                                        {job.labels.map((label) => <JobLabel text={label}/>)}
                                                        <span style={{marginRight: '0.5rem'}}></span>
                                                    </>
                                                }
                                                <Link to={{
                                                    pathname: `/dashboard/jobs/${job.id}`,
                                                    job: job
                                                }}>{job.jobName}</Link>
                                            </TableCell>
                                            <TableCell>
                                                <Link to={{pathname: `/dashboard/jobs/${job.id}`, job: job}}>
                                                    <TimeAgo date={new Date(columnFunction(job))}
                                                             title={new Date(columnFunction(job)).toString()}/>
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
                            onPageChange={handleChangePage}
                        />
                    </>
                }
                </>
            }
            </>
        }
        </>
    );
}

export default JobsTable;