import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import {Alert, AlertTitle} from '@material-ui/lab';
import {Button} from "@material-ui/core";
import TimeAgo from "react-timeago/lib";

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const PollIntervalInSecondsIsTooSmallProblem = (props) => {
    const classes = useStyles();

    const dismissProblem = () => {
        fetch(`/api/problems/poll-interval-in-seconds-is-too-small`, {
            method: 'DELETE'
        })
            .then(resp => props.refresh())
            .catch(error => console.log(error));
    }

    return (
        <Alert className={classes.alert} severity="warning" action={
            <Button color="inherit" size="small" onClick={dismissProblem}>
                DISMISS
            </Button>
        }>
            <AlertTitle><h4 className={classes.alertTitle}>Warning</h4></AlertTitle>
            JobRunr detected that your poll interval in seconds setting is too small.
            It means that the BackgroundJob Master node cannot execute all relevant tasks like scheduling recurring jobs and doing maintenance tasks like deleting succeeded jobs.
            <em><b>&nbsp;You are urged to increase your poll interval in seconds setting as soon as possible.</b></em>
            <br/>
            <ul>
                {props.problem.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet.map((problem, index) => {
                    return <li key={index}>{problem.owner} has a poll interval in seconds that is too small.
                            &nbsp;The poll interval in seconds time box was exceeded <TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(problem.createdAt)}
                            title={new Date(problem.createdAt).toString()} />.</li>
                })}
            </ul>
        </Alert>
    );
};

export default PollIntervalInSecondsIsTooSmallProblem;