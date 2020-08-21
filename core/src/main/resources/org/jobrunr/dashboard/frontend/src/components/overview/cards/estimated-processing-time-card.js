import React, {useRef} from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import TimeAgo from "react-timeago/lib";
import statsState from "../../../StatsStateContext";

const useStyles = makeStyles(theme => ({
    metadata: {
        display: 'flex',
    },
    card: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const EstimatedProcessingTimeCard = () => {
    const classes = useStyles();
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    const stats = statsState.useStatsState(EstimatedProcessingTimeCard);

    const processingTimeRef = useRef(<>Calculating...</>);
    React.useEffect(() => {
        if (stats.estimation.processingDone) {
            processingTimeRef.current = <>All done!</>;
        } else {
            if (stats.estimation.estimatedProcessingTimeAvailable) {
                const estimatedProcessingTimeDate = new Date(stats.estimation.estimatedProcessingFinishedAt);
                processingTimeRef.current =
                    <TimeAgo date={estimatedProcessingTimeDate} title={estimatedProcessingTimeDate.toString()}
                             formatter={timeAgoFormatter}/>;
            } else {
                processingTimeRef.current = <>Calculating...</>;
            }
        }
    }, [stats]);

    return (
        <Card className={classes.card}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Estimated processing time
                </Typography>
                <Typography variant="h5" component="h2">
                    {processingTimeRef.current}
                </Typography>
            </CardContent>
        </Card>
    );
};

export default EstimatedProcessingTimeCard;