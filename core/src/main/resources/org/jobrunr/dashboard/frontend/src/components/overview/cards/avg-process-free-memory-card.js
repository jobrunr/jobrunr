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

const AvgProcessFreeMemoryCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processFreeMemory, 0) / array.length;
        averageProcessFreeMemory = average(servers);
    }

    return (
        <Card className={classes.card}>
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
};

export default AvgProcessFreeMemoryCard;