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

const CPUAllocationIrregularityProblem = (props) => {
    const classes = useStyles();

    const dismissProblem = () => {
        fetch(`/api/problems/cpu-allocation-irregularity`, {
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
            JobRunr detected some CPU Allocation Irregularities (e.g. due to a very long garbage collect cycle
            or due to CPU starvation on shared hosting). This may result in unstable behaviour of your JobRunr cluster.
            <em><b>&nbsp;You are urged to look into this as soon as possible.</b></em>
            <br/>
            <ul>
                {props.problem.cpuAllocationIrregularityMetadataSet.map((irregularity, index) => {
                    return <li key={index}>{irregularity.owner} had a CPU Allocation Irregularity
                        of {irregularity.value} seconds <TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(irregularity.createdAt)}
                            title={new Date(irregularity.createdAt).toString()}/></li>
                })}
            </ul>
        </Alert>
    );
};

export default CPUAllocationIrregularityProblem;