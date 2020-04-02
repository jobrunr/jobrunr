import React from 'react';
import {Link, useHistory} from "react-router-dom";
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import Button from '@material-ui/core/Button';
import ButtonGroup from '@material-ui/core/ButtonGroup';
import Typography from '@material-ui/core/Typography';
import {makeStyles} from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import Alert from '@material-ui/lab/Alert';

import Scheduled from "./states/scheduled-state";
import Enqueued from "./states/enqueued-state";
import Processing from "./states/processing-state";

import Sidebar from "./sidebar";
import Failed from "./states/failed-state";
import Succeeded from "./states/succeeded-state";
import JobCode from "./job-code";
import {CircularProgress, Snackbar} from "@material-ui/core";
import {SortAscending, SortDescending} from "mdi-material-ui";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import Box from "@material-ui/core/Box";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    box: {
        marginBottom: '0'
    },
    content: {
        width: '100%',
    },
    cardContent: {
        width: "100%"
    },
    jobDetails: {
        paddingBottom: "0 !important",
    }
}));

const Job = (props) => {
    const classes = useStyles();
    const history = useHistory();
    const [openJobDeletedSucceededAlert, setOpenJobDeletedSucceededAlert] = React.useState(false);
    const [openJobDeletedFailedAlert, setOpenJobDeletedFailedAlert] = React.useState(false);

    const [isLoading, setIsLoading] = React.useState(true);
    const [job, setJob] = React.useState({jobDetails: {className: "", jobParameters: []}, jobHistory: [{state:""}]});
    const [stateBreadcrumb, setStateBreadcrumb] = React.useState({});
    const [jobStates, setJobStates] = React.useState([]);
    const [order, setOrder] = React.useState(true);

    React.useEffect(() => {
        if (props.location.job) {
            onJob(props.location.job);
        } else {
            fetch(`/api/jobs/${props.match.params.id}`)
                .then(res => res.json())
                .then(job => {
                    onJob(job);
                })
                .catch(error => console.error(error));
        }
    }, [props.location.job, props.match.params.id]);

    const deleteJob = () => {
        fetch(`/api/jobs/${props.match.params.id}`, {
            method: 'DELETE',
        })
            .then(res => {
                if (res.status === 204) {
                    setOpenJobDeletedSucceededAlert(true);
                } else {
                    setOpenJobDeletedFailedAlert(true);
                }
            })
            .catch(error => console.error(error));
    };

    const onJob = (job) => {
        setJob(job);
        setJobStates([...job.jobHistory]);
        setIsLoading(false);
        let state = job.jobHistory[job.jobHistory.length -1].state;
        setStateBreadcrumb({name: state.charAt(0).toUpperCase() + state.substring(1).toLocaleLowerCase(), link: state.toLowerCase()})
    }

    const handleCloseAlert = (event, reason) => {
        setOpenJobDeletedSucceededAlert(false);
        setOpenJobDeletedFailedAlert(false);
        history.goBack();
    };

    const changeSortOrder = () => {
        setOrder(!order);
        jobStates.reverse();
    };

    return (
        <div className={classes.root}>
            <Sidebar/>
            <main className={classes.content}>
                {isLoading
                    ? <CircularProgress/>
                    : <>
                        <Breadcrumbs separator={<NavigateNextIcon fontSize="small"/>} aria-label="breadcrumb">
                            <Link color="inherit" to="/dashboard/jobs">Jobs</Link>
                            <Link color="inherit" to="/dashboard/jobs/default/enqueued">Default queue</Link>
                            <Link color="inherit"
                                  to={`/dashboard/jobs/default/${stateBreadcrumb.link}`}>{stateBreadcrumb.name}</Link>
                            <Typography color="textPrimary">{job.id}</Typography>
                        </Breadcrumbs>
                        <Box my={3} className={classes.box}>
                            <Card className={classes.root}>
                                <CardContent className={classes.cardContent}>
                                    <Grid container spacing={3} justify="space-between">
                                        <Grid item xs={9} className={classes.jobDetails}>
                                            <Typography id="job-id-title" className={classes.title} color="textSecondary">
                                                Job Id: {job.id}
                                            </Typography>
                                        </Grid>
                                        <Grid item xs={3} container className={classes.jobDetails} justify="flex-end">
                                            <ButtonGroup>
                                                {/*<Button variant="outlined" color="primary">*/}
                                                {/*    Requeue*/}
                                                {/*</Button>*/}
                                                <Button variant="outlined" color="primary" onClick={deleteJob}>
                                                    Delete
                                                </Button>
                                            </ButtonGroup>
                                        </Grid>
                                        <Grid item xs={12} className={classes.jobDetails} style={{paddingTop: 0}}>
                                            <Typography id="job-name-title" variant="h5" component="h2" gutterBottom>
                                                {job.jobName}
                                            </Typography>
                                        </Grid>
                                    </Grid>
                                </CardContent>
                            </Card>
                        </Box>
                        <Grid container spacing={3}>
                            <JobCode job={job}/>
                            <Grid item xs={12}>
                                <Typography variant="h5" component="h2">
                                    History&nbsp;
                                    {order
                                        ? <IconButton id="jobhistory-sort-desc-btn" color="inherit"
                                                      onClick={changeSortOrder}><SortDescending/></IconButton>
                                        : <IconButton id="jobhistory-sort-asc-btn" color="inherit"
                                                      onClick={changeSortOrder}><SortAscending/></IconButton>
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
                                                return <Processing key={index} jobState={jobState}/>;
                                            case 'FAILED':
                                                return <Failed key={index} jobState={jobState}/>;
                                            case 'SUCCEEDED':
                                                return <Succeeded key={index} jobState={jobState}/>;
                                            default:
                                                return <>Unknown state</>
                                        }
                                    })}
                            </Grid>
                        </Grid>
                        <Snackbar open={openJobDeletedSucceededAlert} autoHideDuration={3000}
                                  onClose={handleCloseAlert}>
                            <Alert severity="success">
                                Job deleted
                            </Alert>
                        </Snackbar>
                        <Snackbar open={openJobDeletedFailedAlert} autoHideDuration={6000} onClose={handleCloseAlert}>
                            <Alert severity="error">
                                Error deleting job
                            </Alert>
                        </Snackbar>
                    </>
                }
            </main>
        </div>
    );
};

export default Job;