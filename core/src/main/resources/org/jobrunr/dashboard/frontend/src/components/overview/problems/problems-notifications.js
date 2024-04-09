import {useContext, useEffect, useMemo} from 'react';
import LoadingIndicator from "../../LoadingIndicator";
import JobNotFoundProblem from "./job-not-found-problem";
import SevereJobRunrExceptionProblem from "./severe-jobrunr-exception-problem";
import Grid from '@mui/material/Grid';
import CpuAllocationIrregularityProblem from "./cpu-allocation-irregularity-problem";
import NewJobRunrVersionAvailable from "./new-jobrunr-version-available";
import PollIntervalInSecondsIsTooSmallProblem from "./poll-interval-timebox-is-too-small-problem";
import {ProblemsContext} from "../../../ProblemsContext";
import JobRunrApiNotification from "./jobrunr-api-notification";

const problemTypeOrder = {
    "api-notification": 0, "severe-jobrunr-exception": 1, "jobs-not-found": 2, "cpu-allocation-irregularity": 3,
    "poll-interval-in-seconds-is-too-small": 4, "new-jobrunr-version": 5
};
const getProblemOrder = (type) => problemTypeOrder[type] ?? 5;
const problemCompareFn = (a, b) => {
    return getProblemOrder(a.type) - getProblemOrder(b.type);
}

const Problems = () => {
    const {problems, reload, isLoading} = useContext(ProblemsContext);

    const sortedProblems = useMemo(() => {
        return [...problems].sort(problemCompareFn)
    }, [problems]);

    useEffect(() => {
        reload();
    }, [reload]);

    return (
        <div style={{display: "flex"}}>
            {isLoading
                ? <LoadingIndicator/>
                : <Grid container>
                    {sortedProblems.map((problem) => {
                        switch (problem.type) {
                            case "api-notification":
                                return <Grid item xs={12} key={problem.type}><JobRunrApiNotification problem={problem}/></Grid>
                            case 'jobs-not-found':
                                return <Grid item xs={12} key={problem.type}>
                                    <JobNotFoundProblem problem={problem}/>
                                </Grid>
                            case 'severe-jobrunr-exception':
                                return <Grid item xs={12} key={problem.type}>
                                    <SevereJobRunrExceptionProblem problem={problem} refresh={reload}/>
                                </Grid>
                            case 'cpu-allocation-irregularity':
                                return <Grid item xs={12} key={problem.type}>
                                    <CpuAllocationIrregularityProblem problem={problem} refresh={reload}/>
                                </Grid>
                            case 'poll-interval-in-seconds-is-too-small':
                                return <Grid item xs={12} key={problem.type}>
                                    <PollIntervalInSecondsIsTooSmallProblem problem={problem} refresh={reload}/>
                                </Grid>
                            case 'new-jobrunr-version':
                                return <Grid item xs={12} key={problem.type}><NewJobRunrVersionAvailable problem={problem}/></Grid>
                            default:
                                return <Grid item xs={12} key="unknown-problem">Unknown error</Grid>
                        }
                    })}
                </Grid>
            }
        </div>
    );
};

export default Problems;