import {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from "react-router";
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Button from '@mui/material/Button';
import ButtonGroup from '@mui/material/ButtonGroup';
import Typography from '@mui/material/Typography';
import Grid from '@mui/material/Grid';
import Alert from '@mui/material/Alert';
import Paper from '@mui/material/Paper';

import Awaiting from "./states/awaiting-state";
import Scheduled from "./states/scheduled-state";
import Enqueued from "./states/enqueued-state";
import Processing from "./states/processing-state";
import Succeeded from "./states/succeeded-state";
import Failed from "./states/failed-state";
import Deleted from "./states/deleted-state";
import JobCode from "./job-code";
import {Snackbar} from "@mui/material";
import {SortAscending, SortDescending} from "mdi-material-ui";
import IconButton from "@mui/material/IconButton";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import Breadcrumbs from "@mui/material/Breadcrumbs";
import Box from "@mui/material/Box";
import LoadingIndicator from "../LoadingIndicator";
import {jobStateToHumanReadableName} from "../utils/job-utils";
import SucceededNotification from "./notifications/succeeded-notification";
import DeletedNotification from "./notifications/deleted-notification";
import JobDetailsNotCacheableNotification from "./notifications/job-details-not-cacheable-notification";
import CarbonAwareScheduledNotification from "./notifications/carbon-aware-scheduled-notification";
import VersionFooter from "../utils/version-footer";
import JobLabel from "../utils/job-label";
import {ItemsNotFound} from "../utils/items-not-found";

const JobView = (props) => {
    const navigate = useNavigate();

    const [apiStatus, setApiStatus] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [job, setJob] = useState(null);
    const [stateBreadcrumb, setStateBreadcrumb] = useState({});
    const [jobStates, setJobStates] = useState([]);
    const [order, setOrder] = useState(true);
    const {jobId} = useParams();

    useEffect(() => {
        getJob(jobId);

        const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/jobs/" + jobId);
        eventSource.addEventListener('message', e => onJob(JSON.parse(e.data)));
        eventSource.addEventListener('close', e => eventSource.close());
        return () => eventSource.close();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [jobId]);

    useEffect(() => {
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
        fetch(`/api/jobs/${jobId}`, {
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
        fetch(`/api/jobs/${jobId}/requeue`, {
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
            link: state.toUpperCase()
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
            navigate(-1);
        }
    };

    const changeSortOrder = () => {
        setOrder(!order);
    };

    return (
        <main style={{width: "100%", overflowX: "hidden"}}>
            {isLoading
                ? <LoadingIndicator/>
                : <>{job === null
                    ?
                    <Paper><ItemsNotFound id="no-jobs-found-message">
                        Job not found
                    </ItemsNotFound></Paper>
                    : <>
                        <Breadcrumbs id="breadcrumb" separator={<NavigateNextIcon fontSize="small"/>}
                                     aria-label="breadcrumb">
                            <Link color="inherit" to="/dashboard/jobs">Jobs</Link>
                            <Link color="inherit"
                                  to={`/dashboard/jobs?state=${stateBreadcrumb.link}`}>{stateBreadcrumb.name}</Link>
                            <Typography color="textPrimary">{job.id}</Typography>
                        </Breadcrumbs>
                        <Box mt={3}>
                            <Card sx={{display: "flex", borderBottomLeftRadius: 0, borderBottomRightRadius: 0}}>
                                <CardContent sx={{width: "100%"}}>
                                    <Grid container spacing={1}>
                                        <Grid size={6}>
                                            <Typography id="job-id-title" color="textSecondary">
                                                Job Id: {job.id}
                                            </Typography>
                                        </Grid>
                                        <Grid container justifyContent="flex-end" size={6}>
                                            <ButtonGroup>
                                                {stateBreadcrumb.state !== 'ENQUEUED' &&
                                                    <Button variant="outlined" color="primary" onClick={requeueJob}>
                                                        Requeue
                                                    </Button>
                                                }
                                                {stateBreadcrumb.state !== 'DELETED' &&
                                                    <Button variant="outlined" color="primary" onClick={deleteJob}>
                                                        Delete
                                                    </Button>
                                                }
                                            </ButtonGroup>
                                        </Grid>
                                        <Grid size={12}>
                                            <Typography id="job-name-title" variant="h5" component="h2" gutterBottom>
                                                {job.jobName} {job.labels?.map((label) => <JobLabel text={label} key={label}/>)}
                                            </Typography>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Box>

                        <Grid container spacing={3}>
                            <JobCode job={job}/>

                            {job.jobDetails.cacheable === false && <JobDetailsNotCacheableNotification job={job}/>}
                            {stateBreadcrumb.state === 'SUCCEEDED' && <SucceededNotification job={job}/>}
                            {stateBreadcrumb.state === 'DELETED' && <DeletedNotification job={job}/>}
                            {stateBreadcrumb.state === 'SCHEDULED' && <CarbonAwareScheduledNotification job={job}/>}
                            {stateBreadcrumb.state === 'AWAITING' && <CarbonAwareScheduledNotification job={job}/>}

                            <Grid size={12}>
                                <Typography variant="h5" component="h2">
                                    History&nbsp;
                                    <IconButton
                                        id={`jobhistory-sort-${order ? "desc" : "asc"}-btn`}
                                        color="inherit"
                                        onClick={changeSortOrder}
                                        style={{scrollMarginTop: '70px'}}
                                        size="large"
                                    >
                                        {order ? <SortDescending/> : <SortAscending/>}
                                    </IconButton>
                                </Typography>
                            </Grid>

                            <Grid id="job-history-panel" size={12}>
                                {
                                    jobStates.map((jobState, index) => {
                                        switch (jobState.state) {
                                            case 'AWAITING':
                                                return <Awaiting key={index} job={job} jobState={jobState}/>;
                                            case 'SCHEDULED':
                                                return <Scheduled key={index} jobState={jobState}/>;
                                            case 'ENQUEUED':
                                                return <Enqueued key={index} jobState={jobState}/>;
                                            case 'PROCESSING':
                                                return <Processing key={index} index={index} job={job} jobState={jobState}/>;
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
                    </>
                }
                </>
            }
            <VersionFooter/>
        </main>
    );
};

export default JobView;
