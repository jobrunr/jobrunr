import { Alert, AlertTitle } from '@mui/material';

const classes = {
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
};

const JobNotFoundProblem = (props) => {
    return (
        <Alert style={classes.alert} severity="warning">
            <AlertTitle><h4 style={classes.alertTitle}>Warning</h4></AlertTitle>
            There are SCHEDULED jobs that do not exist anymore in your code. These jobs will
            fail with a <strong>JobNotFoundException</strong> (due to a ClassNotFoundException or a
            MethodNotFoundException). <br/>
        </Alert>
    );
};

export default JobNotFoundProblem;