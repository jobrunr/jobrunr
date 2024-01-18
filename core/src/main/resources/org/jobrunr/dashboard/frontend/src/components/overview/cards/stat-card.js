import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';

const classes = {
    card: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
};

const StatCard = ({title, content}) => {
    return (
        <Card style={classes.card}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    {title}
                </Typography>
                <Typography variant="h5" component="h2">
                    {content}
                </Typography>
            </CardContent>
        </Card>
    );
};

export default StatCard;