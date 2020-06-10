import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';

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