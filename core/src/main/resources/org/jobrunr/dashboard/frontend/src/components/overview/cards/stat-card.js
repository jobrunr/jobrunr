import Typography from '@mui/material/Typography';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';

const StatCard = ({title, children}) => {
    return (
        <Card role="gridcell" style={{minWidth: '215px', minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    {title}
                </Typography>
                <Typography variant="h5" component="h2">
                    {children}
                </Typography>
            </CardContent>
        </Card>
    );
};

export default StatCard;