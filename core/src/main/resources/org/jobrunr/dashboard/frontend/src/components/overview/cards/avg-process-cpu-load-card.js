import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';

const useStyles = makeStyles(theme => ({
    infocard: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const AvgProcessCpuLoadCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    let averageProcessCpuLoad = servers[0].processCpuLoad;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a.processCpuLoad + b.processCpuLoad) / array.length;
        averageProcessCpuLoad = average(servers);
    }


    return (
        <Card className={classes.infocard}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Avg Process Cpu Load
                </Typography>
                <Typography variant="h5" component="h2">
                    {parseFloat(averageProcessCpuLoad * 100).toFixed(2)} %
                </Typography>
            </CardContent>
        </Card>
    );
};

export default AvgProcessCpuLoadCard;