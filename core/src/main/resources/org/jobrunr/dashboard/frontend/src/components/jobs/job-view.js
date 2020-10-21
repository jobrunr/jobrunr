import React from 'react';
import {Link, useHistory, useLocation} from "react-router-dom";
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import Typography from '@material-ui/core/Typography';
import {makeStyles} from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import {Alert} from '@material-ui/lab';
import Paper from '@material-ui/core/Paper';

import Scheduled from "./states/scheduled-state";
import Enqueued from "./states/enqueued-state";
import Processing from "./states/processing-state";
import Succeeded from "./states/succeeded-state";
import Failed from "./states/failed-state";
import Deleted from "./states/deleted-state";
import JobCode from "./job-code";
import {Snackbar} from "@material-ui/core";
import {SortAscending, SortDescending} from "mdi-material-ui";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import Box from "@material-ui/core/Box";
import LoadingIndicator from "../LoadingIndicator";
import {jobStateToHumanReadableName} from "../utils/job-utils";
import SucceededNotification from "./notifications/succeeded-notification";
import DeletedNotification from "./notifications/deleted-notification";

const useStyles = makeStyles(() => ({
    root: {
        display: 'flex',
    },
    box: {
        marginBottom: '0'
    },
    content: {
        width: '100%',
    },
    noItemsFound: {
        padding: '1rem'
    },
    cardContent: {
        width: "100%"
    },
    sortButton: {
        scrollMarginTop: '70px'
    },
    jobDetails: {
        paddingBottom: "0 !important",
    }
}));

const JobView = (props) => {
    const classes = useStyles();
    const history = useHistory();
    const location = useLocation();

    const [apiStatus, setApiStatus] = React.useState(null);
    const [isLoading, setIsLoading] = React.useState(true);
    const [job, setJob] = React.useState(null);
    const [stateBreadcrumb, setStateBreadcrumb] = React.useState({});
    const [jobStates, setJobStates] = React.useState([]);
    const [order, setOrder] = React.useState(true);
    const jobId = props.match.params.id;

    React.useEffect(() => {
        if (location.job) {
            onJob(location.job);
        } else {
            getJob(jobId);
        }

        const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/jobs/" + jobId);
        eventSource.addEventListener('message', e => onJob(JSON.parse(e.data)));
        eventSource.addEventListener('close', e => eventSource.close());
        return () => eventSource.close();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.job, jobId]);

    React.useEffect(() => {
        if (job) {
            if (order) {
                setJobStates([...job.jobHistory]);
            } else {
                setJobStates([...job.jobHistory].reverse());
            }
        }
    }, [job, order]);

    const getJob = (id) => {
        fetch(`/api/jobs/${id}`)
            .then(res => {
                if (res.status === 200) {
                    res.json()
                        .then(job => onJob(job));
                } else {
                    onJobNotFound();
                }
            })
            .catch(error => console.error(error));
    }

    const deleteJob = () => {
        fetch(`/api/jobs/${props.match.params.id}`, {
            method: 'DELETE',
        })
            .then(res => {
                if (res.status === 204) {
                    setApiStatus({type: 'delete', severity: 'success', message: 'Successfully deleted job'});
                } else {
                    setApiStatus({type: 'delete', severity: 'error', message: 'Error deleting job'});
                }
            })
            .catch(error => console.error(error));
    };
    const requeueJob = () => {
        fetch(`/api/jobs/${props.match.params.id}/requeue`, {
            method: 'POST',
        })
            .then(res => {
                if (res.status === 204) {
                    setApiStatus({type: 'requeue', severity: 'success', message: 'Successfully requeued job'});
                    getJob(props.match.params.id);
                } else {
                    setApiStatus({type: 'requeue', severity: 'error', message: 'Error requeueing job'});
                }
            })
            .catch(error => console.error(error));
    };

    const onJob = (job) => {
        setJob(job);
        setIsLoading(false);
        let state = job.jobHistory[job.jobHistory.length - 1].state;
        setStateBreadcrumb({
            state: state,
            name: jobStateToHumanReadableName(state),
            link: state.toLowerCase()
        })
    }

    const onJobNotFound = () => {
        setJob(null);
        setIsLoading(false);
    }

    const handleCloseAlert = (event, reason) => {
        const mustGoBack = 'delete' === apiStatus.type;
        setApiStatus(null);
        if (mustGoBack) {
            history.goBack();
        }
    };

    const changeSortOrder = () => {
        setOrder(!order);
    };

    return (
        <main className={classes.content}>
            {isLoading
                ? <LoadingIndicator/>
                : <>{job === null
                    ?
                    <Paper><Typography id="no-jobs-found-message" variant="body1" className={classes.noItemsFound}>Job
                        not found</Typography></Paper>
                    : <>
                        <Breadcrumbs id="breadcrumb" separator={<NavigateNextIcon fontSize="small"/>}
                                     aria-label="breadcrumb">
                            <Link color="inherit" to="/dashboard/jobs">Jobs</Link>
                            <Link color="inherit"
                                  to={`/dashboard/jobs?state=${stateBreadcrumb.link}`}>{stateBreadcrumb.name}</Link>
                            <Typography color="textPrimary">{job.id}</Typography>
                        </Breadcrumbs>
                        <Box my={3} className={classes.box}>
                            <Card className={classes.root}>
                                <CardContent className={classes.cardContent}>
                                    <Grid container spacing={3} justify="space-between">
                                        <Grid item xs={9} className={classes.jobDetails}>
                                            <Typography id="job-id-title" className={classes.title}
                                                        color="textSecondary">
                                                Job Id: {job.id}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={3} container className={classes.jobDetails}
                                              justify="flex-end">
                                            <ButtonGroup>
                                                <Button variant="outlined" color="primary" onClick={requeueJob}>
                                                    Requeue
                                                </Button>
                                                {stateBreadcrumb.state !== 'DELETED' &&
                                                <Button variant="outlined" color="primary" onClick={deleteJob}>
                                                    Delete
                                                </Button>
                                                }
                                            </ButtonGroup>
                                        </Grid>
                                        <Grid item xs={12} className={classes.jobDetails} style={{paddingTop: 0}}>
                                            <Typography id="job-name-title" variant="h5" component="h2"
                                                        gutterBottom>
                                                {job.jobName}
                                            </Typography>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Box>

                        <Grid container spacing={3}>
                            <JobCode job={job}/>

                            {stateBreadcrumb.state === 'SUCCEEDED' && <SucceededNotification job={job}/>}
                            {stateBreadcrumb.state === 'DELETED' && <DeletedNotification job={job}/>}

                            <Grid item xs={12}>
                                <Typography variant="h5" component="h2">
                                    History&nbsp;
                                    {order
                                        ? <IconButton id="jobhistory-sort-desc-btn" color="inherit"
                                                      onClick={changeSortOrder}
                                                      className={classes.sortButton}><SortDescending/></IconButton>
                                        : <IconButton id="jobhistory-sort-asc-btn" color="inherit"
                                                      onClick={changeSortOrder}
                                                      className={classes.sortButton}><SortAscending/></IconButton>
                                    }
                                </Typography>
                            </Grid>

                            <Grid id="job-history-panel" item xs={12}>
                                {
                                    jobStates.map((jobState, index) => {
                                        switch (jobState.state) {
                                            case 'SCHEDULED':
                                                return <Scheduled key={index} jobState={jobState}/>;
                                            case 'ENQUEUED':
                                                return <Enqueued key={index} jobState={jobState}/>;
                                            case 'PROCESSING':
                                                return <Processing key={index} index={index} job={job}
                                                                   jobState={jobState}/>;
                                            case 'FAILED':
                                                return <Failed key={index} jobState={jobState}/>;
                                            case 'SUCCEEDED':
                                                return <Succeeded key={index} jobState={jobState}/>;
                                            case 'DELETED':
                                                return <Deleted key={index} jobState={jobState}/>;
                                            default:
                                                return <div key={index}>Unknown state</div>
                                        }
                                    })}
                            </Grid>
                        </Grid>
                        {apiStatus &&
                        <Snackbar open={true} autoHideDuration={3000} onClose={handleCloseAlert}>
                            <Alert severity={apiStatus.severity}>
                                {apiStatus.message}
                            </Alert>
                        </Snackbar>
                        }
                    </>
                }
                </>
            }
        </main>
    );
};

export default JobView;