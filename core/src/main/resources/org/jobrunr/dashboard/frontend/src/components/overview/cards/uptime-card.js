import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import TimeAgo from "react-timeago/lib";

const useStyles = makeStyles(theme => ({
    infocard: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const UptimeCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    return (
        <Card className={classes.infocard}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Uptime
                </Typography>
                <Typography variant="h5" component="h2">
                    <TimeAgo date={new Date(servers[0].firstHeartbeat)}
                             title={new Date(servers[0].firstHeartbeat).toString()} formatter={timeAgoFormatter}/>
                </Typography>
            </CardContent>
        </Card>
    );
};

export default UptimeCard;