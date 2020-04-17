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

const AvgProcessFreeMemoryCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a.processFreeMemory + b.processFreeMemory) / array.length;
        averageProcessFreeMemory = average(servers);
    }


    return (
        <Card className={classes.infocard}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Avg Process Free Memory
                </Typography>
                <Typography variant="h5" component="h2">
                    {humanFileSize(averageProcessFreeMemory, true)}
                </Typography>
            </CardContent>
        </Card>
    );

    function humanFileSize(bytes, si) {
        var thresh = si ? 1000 : 1024;
        if (Math.abs(bytes) < thresh) {
            return bytes + ' B';
        }
        var units = si
            ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
            : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes.toFixed(1) + ' ' + units[u];
    }
};

export default AvgProcessFreeMemoryCard;