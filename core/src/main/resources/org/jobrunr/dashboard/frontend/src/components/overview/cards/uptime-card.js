import makeStyles from '@mui/styles/makeStyles';

import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
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