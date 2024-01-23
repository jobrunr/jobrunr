import {useEffect, useState} from 'react';
import {ProblemNotification} from "./problem-notification";


const JobRunrApiNotification = () => {
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
        return <ProblemNotification severity="info" title={notification.title}>
            <span dangerouslySetInnerHTML={{__html: notification.body}}/>
        </ProblemNotification>
    }
    return null;
};

export default JobRunrApiNotification;