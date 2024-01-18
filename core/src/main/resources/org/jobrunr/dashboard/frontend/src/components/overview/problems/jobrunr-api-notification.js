import { useState, useEffect } from 'react';
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

const JobRunrApiNotification = (props) => {
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
        return <Alert style={classes.alert} severity="info">
            <AlertTitle><h4 style={classes.alertTitle}>{notification.title}</h4></AlertTitle>
            <span dangerouslySetInnerHTML={{__html: notification.body}}/>
        </Alert>
    }
    return null;
};

export default JobRunrApiNotification;