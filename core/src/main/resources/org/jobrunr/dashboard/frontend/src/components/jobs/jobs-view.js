import React from 'react';
import {useHistory} from "react-router-dom";
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import Box from "@material-ui/core/Box";
import {makeStyles} from '@material-ui/core/styles';
import LoadingIndicator from "../LoadingIndicator";
import JobsTable from "../utils/jobs-table";
import {jobStateToHumanReadableName} from "../utils/job-utils";

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

const JobsView = (props) => {
    const classes = useStyles();
    const history = useHistory();

    const urlSearchParams = new URLSearchParams(props.location.search);
    const page = urlSearchParams.get('page');
    const jobState = urlSearchParams.get('state') ?? 'ENQUEUED';
    const [isLoading, setIsLoading] = React.useState(true);
    const [jobPage, setJobPage] = React.useState({total: 0, limit: 20, currentPage: 0, items: []});

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

    React.useEffect(() => {
        setIsLoading(true);
        const offset = (page) * 20;
        const limit = 20;
        let url = `/api/jobs?state=${jobState.toUpperCase()}&offset=${offset}&limit=${limit}&order=${sort}`;
        fetch(url)
            .then(res => res.json())
            .then(response => {
                setJobPage(response);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, [page, jobState, sort, history.location.key]);

    return (
        <main className={classes.content}>
            <Box my={3}>
                <Typography id="title" variant="h4">{jobStateToHumanReadableName(jobState)}</Typography>
            </Box>
            {isLoading
                ? <LoadingIndicator/>
                : <Paper>
                    <JobsTable jobPage={jobPage} jobState={jobState}/>
                </Paper>
            }
        </main>
    );
}

export default JobsView;