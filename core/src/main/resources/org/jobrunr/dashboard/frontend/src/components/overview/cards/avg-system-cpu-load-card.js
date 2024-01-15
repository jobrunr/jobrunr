import makeStyles from '@mui/styles/makeStyles';

import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';

const useStyles = makeStyles(theme => ({
    card: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const AvgSystemCpuLoadCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    let averageSystemCpuLoad = servers[0].systemCpuLoad;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.systemCpuLoad, 0) / array.length;
        averageSystemCpuLoad = average(servers);
    }

    return (
        <Card className={classes.card}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Avg System Cpu Load
                </Typography>
                <Typography variant="h5" component="h2">
                    {parseFloat(averageSystemCpuLoad * 100).toFixed(2)} %
                </Typography>
            </CardContent>
        </Card>
    );
};

export default AvgSystemCpuLoadCard;