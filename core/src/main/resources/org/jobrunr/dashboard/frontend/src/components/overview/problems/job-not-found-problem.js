import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import {Alert, AlertTitle} from '@material-ui/lab';

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const JobNotFoundProblem = (props) => {
    const classes = useStyles();

    return (
        <Alert className={classes.alert} severity="warning">
            <AlertTitle><h4 className={classes.alertTitle}>Warning</h4></AlertTitle>
            There are SCHEDULED jobs that do not exist anymore in your code. These jobs will
            fail with a <strong>JobNotFoundException</strong> (due to a ClassNotFoundException or a
            MethodNotFoundException). <br/>
        </Alert>
    );
};

export default JobNotFoundProblem;