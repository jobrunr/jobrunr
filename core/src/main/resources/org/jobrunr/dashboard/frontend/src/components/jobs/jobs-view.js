import {useEffect, useState} from 'react';
import {useLocation} from "react-router";
import Typography from '@mui/material/Typography';
import Paper from '@mui/material/Paper';
import Box from "@mui/material/Box";
import LoadingIndicator from "../LoadingIndicator";
import JobsTable from "./jobs-table";
import {jobStateToHumanReadableName} from "../utils/job-utils";
import VersionFooter from "../utils/version-footer";
import {JobRunrProNotice} from "../utils/jobrunr-pro-notice";


const JobsView = () => {
    const location = useLocation();

    const urlSearchParams = new URLSearchParams(location.search);
    const page = urlSearchParams.get('page');
    const jobState = urlSearchParams.get('state') ?? 'ENQUEUED';
    const [currentFetchKey, setCurrentFetchKey] = useState(undefined);
    const [jobPage, setJobPage] = useState({total: 0, limit: 20, currentPage: 0, items: []});

    let sort = 'updatedAt:ASC';
    switch (jobState.toUpperCase()) {
        case 'SUCCEEDED':
            sort = 'updatedAt:DESC';
            break;
        case 'FAILED':
            sort = 'updatedAt:DESC';
            break;
        default:
    }

    const fetchKey = `${page}-${jobState}-${sort}-${location.key}`;

    useEffect(() => {
        const abortController = new AbortController();
        const offset = (page) * 20;
        const limit = 20;
        let url = `/api/jobs?state=${jobState.toUpperCase()}&offset=${offset}&limit=${limit}&order=${sort}`;
        fetch(url, {signal: abortController.signal})
            .then(res => res.json())
            .then(response => {
                setJobPage(response);
            })
            .catch(error => console.log(error))
            .finally(() => setCurrentFetchKey(fetchKey));
        return () => abortController.abort("Starting a new request due to state changes");
    }, [fetchKey]);

    const isLoading = currentFetchKey !== fetchKey;

    return (
        <main style={{width: '100%'}}>
            <Box my={3}>
                <Typography id="title" variant="h4">{jobStateToHumanReadableName(jobState)}</Typography>
            </Box>
            {isLoading
                ? <LoadingIndicator/>
                : <>
                    {jobState === 'ENQUEUED' &&
                        <JobRunrProNotice>Do you want instant job processing? That comes out of the
                            box with <a
                                href="https://www.jobrunr.io/en/documentation/pro/" target="_blank" rel="noreferrer"
                                title="Support the development of JobRunr by getting a Pro license!">JobRunr Pro</a>.
                        </JobRunrProNotice>
                    }
                    {jobState === 'FAILED' &&
                        <JobRunrProNotice>Need to requeue a lot of failed jobs? That's easy-peasy
                            with <a
                                href="https://www.jobrunr.io/en/documentation/pro/jobrunr-pro-dashboard/"
                                target="_blank" rel="noreferrer"
                                title="Support the development of JobRunr by getting a Pro license!">JobRunr Pro</a>.
                        </JobRunrProNotice>
                    }
                    {jobState !== 'ENQUEUED' && jobState !== 'FAILED' &&
                        <JobRunrProNotice>Are you trying to find a certain job here? With <a
                            href="https://www.jobrunr.io/en/documentation/pro/jobrunr-pro-dashboard/" target="_blank"
                            rel="noreferrer" title="Support the development of JobRunr by getting a Pro license!">JobRunr
                            Pro</a> you would have already found it.</JobRunrProNotice>
                    }
                    <Paper>
                        <JobsTable jobPage={jobPage} jobState={jobState}/>
                    </Paper>
                    <VersionFooter/>
                </>
            }
        </main>
    );
};

export default JobsView;