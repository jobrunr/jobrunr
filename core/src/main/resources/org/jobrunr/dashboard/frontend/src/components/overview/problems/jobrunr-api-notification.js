import { useState, useEffect } from 'react';
import makeStyles from '@mui/styles/makeStyles';
import { Alert, AlertTitle } from '@mui/material';

const useStyles = makeStyles(theme => ({
    alert: {
        marginBottom: '2rem',
    },
    alertTitle: {
        lineHeight: 1,
        margin: 0
    }
}));

const JobRunrApiNotification = (props) => {
    const classes = useStyles();

    const [notification, setNotification] = useState(null);

    useEffect(() => {
        fetch(`https://api.jobrunr.io/api/notifications/jobrunr`)
            .then(res => res.json())
            .then(response => {
                setNotification(response);
            })
            .catch(error => console.log('No JobRunr notifications!'));
    }, []);


    if (notification) {
        return <Alert className={classes.alert} severity="info">
            <AlertTitle><h4 className={classes.alertTitle}>{notification.title}</h4></AlertTitle>
            <span dangerouslySetInnerHTML={{__html: notification.body}}/>
        </Alert>
    }
    return null;
};

export default JobRunrApiNotification;