import {useEffect, useState} from 'react';
import LoadingIndicator from "../../LoadingIndicator";
import JobNotFoundProblem from "./job-not-found-problem";
import SevereJobRunrExceptionProblem from "./severe-jobrunr-exception-problem";
import Grid from '@mui/material/Grid';
import CpuAllocationIrregularityProblem from "./cpu-allocation-irregularity-problem";
import NewJobRunrVersionAvailable from "./new-jobrunr-version-available";
import JobRunrApiNotification from "./jobrunr-api-notification";
import PollIntervalInSecondsIsTooSmallProblem from "./poll-interval-timebox-is-too-small-problem";

const problemTypeOrder = {
    "severe-jobrunr-exception": 0, "jobs-not-found": 1, "cpu-allocation-irregularity": 2,
    "poll-interval-in-seconds-is-too-small": 3, "new-jobrunr-version": 4
};
const getProblemOrder = (type) => problemTypeOrder[type] ?? 5;
const problemCompareFn = (a, b) => {
    return getProblemOrder(a.type) - getProblemOrder(b.type);
}

const Problems = () => {
    const [isProblemsApiLoading, setProblemsApiIsLoading] = useState(true);
    const [problems, setProblems] = useState([]);
    const [mustRefresh, setRefresh] = useState(true);

    const refresh = () => setRefresh(!mustRefresh);

    useEffect(() => {
        fetch(`/api/problems`)
            .then(res => res.json())
            .then(response => {
                response.sort(problemCompareFn);
                setProblems(response);
                setProblemsApiIsLoading(false);
            })
            .catch(error => console.log(error));
    }, [mustRefresh]);

    return (
        <div style={{display: "flex"}}>
            {isProblemsApiLoading
                ? <LoadingIndicator/>
                : <Grid container>
                    <Grid item xs={12}><JobRunrApiNotification/></Grid>
                    {problems.map((problem) => {
                        switch (problem.type) {
                            case 'jobs-not-found':
                                return <Grid item xs={12} key={problem.type}>
                                    <JobNotFoundProblem problem={problem}/>
                                </Grid>
                            case 'severe-jobrunr-exception':
                                return <Grid item xs={12} key={problem.type}>
                                    <SevereJobRunrExceptionProblem problem={problem} refresh={refresh}/>
                                </Grid>
                            case 'cpu-allocation-irregularity':
                                return <Grid item xs={12} key={problem.type}>
                                    <CpuAllocationIrregularityProblem problem={problem} refresh={refresh}/>
                                </Grid>
                            case 'poll-interval-in-seconds-is-too-small':
                                return <Grid item xs={12} key={problem.type}>
                                    <PollIntervalInSecondsIsTooSmallProblem problem={problem} refresh={refresh}/>
                                </Grid>
                            default:
                                return <Grid item xs={12} key="unknown-problem">Unknown error</Grid>
                        }
                    })}
                    <Grid item xs={12}>
                        <NewJobRunrVersionAvailable/>
                    </Grid>
                </Grid>
            }
        </div>
    );
};

export default Problems;