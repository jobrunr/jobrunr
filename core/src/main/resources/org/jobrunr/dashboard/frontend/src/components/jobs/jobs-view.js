import React from 'react';
import {Link, useHistory} from "react-router-dom";
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import Breadcrumbs from '@material-ui/core/Breadcrumbs';
import NavigateNextIcon from '@material-ui/icons/NavigateNext';
import Box from "@material-ui/core/Box";
import {makeStyles} from '@material-ui/core/styles';
import LoadingIndicator from "../LoadingIndicator";
import JobsTable from "../utils/jobs-table";

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

    let title, sortField = 'createdAt', sortOrder = 'ASC';
    switch (jobState.toUpperCase()) {
        case 'SCHEDULED':
            title = "Scheduled jobs";
            break;
        case 'ENQUEUED':
            title = "Enqueued jobs";
            break;
        case 'PROCESSING':
            title = "Jobs being processed";
            break;
        case 'SUCCEEDED':
            title = "Succeeded jobs";
            sortField = 'updatedAt';
            sortOrder = 'DESC';
            break;
        case 'FAILED':
            title = "Failed jobs";
            sortField = 'updatedAt';
            sortOrder = 'DESC';
            break;
        case 'DELETED':
            title = "Deleted jobs";
            sortField = 'updatedAt';
            sortOrder = 'DESC';
            break;
        default:
        // code block
    }

    React.useEffect(() => {
        setIsLoading(true);
        const offset = (page) * 20;
        const limit = 20;
        let url = `/api/jobs?state=${jobState.toUpperCase()}&offset=${offset}&limit=${limit}&order=${sortOrder}&orderOnField=${sortField}`;
        fetch(url)
            .then(res => res.json())
            .then(response => {
                setJobPage(response);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, [page, jobState, sortField, sortOrder, history.location.key]);

    return (
        <main className={classes.content}>
            <Breadcrumbs id="breadcrumb" separator={<NavigateNextIcon fontSize="small"/>} aria-label="breadcrumb">
                <Link color="inherit" to="/dashboard/jobs">Jobs</Link>
                <Link color="inherit" to="/dashboard/jobs/default/enqueued">Default queue</Link>
                <Typography color="textPrimary">{title}</Typography>
            </Breadcrumbs>
            <Box my={3}>
                <Typography id="title" variant="h4">{title}</Typography>
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