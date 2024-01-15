import makeStyles from '@mui/styles/makeStyles';

import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import {humanFileSize} from "../../../utils/helper-functions";

const useStyles = makeStyles(theme => ({
    card: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const AvgProcessMemoryUsageCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    let averageProcessMemoryUsage = servers[0].processAllocatedMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processAllocatedMemory, 0) / array.length;
        averageProcessMemoryUsage = average(servers);
    }

    return (
        <Card className={classes.card}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Avg Process Memory Usage
                </Typography>
                <Typography variant="h5" component="h2">
                    {humanFileSize(averageProcessMemoryUsage, true)}
                </Typography>
            </CardContent>
        </Card>
    );
};

export default AvgProcessMemoryUsageCard;