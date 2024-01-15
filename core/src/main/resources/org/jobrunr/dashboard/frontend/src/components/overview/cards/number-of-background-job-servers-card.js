import makeStyles from '@mui/styles/makeStyles';

import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';

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

const NbrOfBackgroundJobServersCard = (props) => {
    const classes = useStyles();
    const servers = props.servers;

    return (
        <Card className={classes.card}>
            <CardContent>
                <Typography className={classes.title} color="textSecondary" gutterBottom>
                    Nbr of servers
                </Typography>
                <Typography variant="h5" component="h2">
                    {servers.length}
                </Typography>
            </CardContent>
        </Card>
    );
};

export default NbrOfBackgroundJobServersCard;