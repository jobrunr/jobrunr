import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import LoadingIndicator from "../../LoadingIndicator";
import JobNotFoundProblem from "./job-not-found-problem";
import SevereJobRunrExceptionProblem from "./severe-jobrunr-exception-problem";
import Grid from '@material-ui/core/Grid';
import CpuAllocationIrregularityProblem from "./cpu-allocation-irregularity-problem";
import NewJobRunrVersionAvailable from "./new-jobrunr-version-available";
import PollIntervalInSecondsIsTooSmallProblem from "./poll-interval-timebox-is-too-small-problem";

const useStyles = makeStyles(theme => ({
    alert: {
        width: '100%',
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    },
    metadata: {
        display: 'flex',
    },
    noServersFound: {
        marginTop: '1rem',
        padding: '1rem',
        width: '100%'
    },
}));

const Problems = () => {
    const classes = useStyles();

    const [isProblemsApiLoading, setProblemsApiIsLoading] = React.useState(true);
    const [problems, setProblems] = React.useState([]);
    const [mustRefresh, setRefresh] = React.useState(true);

    const refresh = () => setRefresh(!mustRefresh);

    React.useEffect(() => {
        fetch(`/api/problems`)
            .then(res => res.json())
            .then(response => {
                setProblems(response);
                setProblemsApiIsLoading(false);
            })
            .catch(error => console.log(error));
    }, [mustRefresh]);

    return (
        <div className={classes.metadata}>
            {isProblemsApiLoading
                ? <LoadingIndicator/>
                : <Grid container> {problems.map((problem, index) => {
                    switch (problem.type) {
                        case 'jobs-not-found':
                            return <Grid item xs={12} key={index}><JobNotFoundProblem problem={problem}/></Grid>
                        case 'severe-jobrunr-exception':
                            return <Grid item xs={12} key={index}><SevereJobRunrExceptionProblem problem={problem}
                                                                                                 refresh={refresh}/></Grid>
                        case 'cpu-allocation-irregularity':
                            return <Grid item xs={12} key={index}><CpuAllocationIrregularityProblem problem={problem}
                                                                                                    refresh={refresh}/></Grid>
                        case 'poll-interval-in-seconds-is-too-small':
                            return <Grid item xs={12} key={index}><PollIntervalInSecondsIsTooSmallProblem problem={problem}
                                                                                                    refresh={refresh}/></Grid>
                        case 'new-jobrunr-version':
                            return <Grid item xs={12} key={index}><NewJobRunrVersionAvailable problem={problem} /></Grid>
                        default:
                            return <Grid item xs={12} key={index}>Unknown error</Grid>
                    }
                })}
                </Grid>
            }
        </div>
    );
};

export default Problems;