import React, {useContext, useRef} from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import TimeAgo from "react-timeago/lib";

import {StatsContext} from "../../../layouts/Admin";

const useStyles = makeStyles(theme => ({
    metadata: {
        display: 'flex',
    },
    infocard: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const EstimatedProcessingTimeCard = () => {
    const classes = useStyles();
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    const statsContext = useContext(StatsContext);
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});
    const processingTimeRef = useRef();
    const processingDoneRef = useRef();
    const {stats} = statsContext;

    React.useEffect(() => {
        const oldStats = oldStatsRef.current;
        if (!stats.succeeded || stats.succeeded < 1) return;

        if (stats.enqueued && stats.enqueued < 1 && stats.processing && stats.processing < 1) {
            processingDoneRef.current = "All done!";
            return;
        } else if (!oldStats.succeeded || oldStats.succeeded < 1 || stats.succeeded === oldStats.succeeded) {
            oldStatsRef.current = {...stats, timestamp: new Date()};
            return;
        }
        const amountSucceeded = stats.succeeded - oldStats.succeeded;

        const timeDiff = new Date() - oldStats.timestamp;
        if (!isNaN(timeDiff)) {
            const amountSucceededPerSecond = amountSucceeded * 1000 / timeDiff;
            const estimatedProcessingTime = stats.enqueued / amountSucceededPerSecond
            processingTimeRef.current = (new Date().getTime() + (estimatedProcessingTime * 1000));
        }
    }, [stats]);

    return (
        <Card className={classes.infocard}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Estimated processing time
                </Typography>
                <Typography variant="h5" component="h2">
                    {processingDoneRef.current &&
                    <>{processingDoneRef.current}</>
                    }
                    {(!processingDoneRef.current && processingTimeRef.current)
                        ?
                        <TimeAgo date={processingTimeRef.current} title={new Date(processingTimeRef.current).toString()}
                                 formatter={timeAgoFormatter}/>
                        : <>Calculating...</>
                    }
                </Typography>
            </CardContent>
        </Card>
    );
};

export default EstimatedProcessingTimeCard;