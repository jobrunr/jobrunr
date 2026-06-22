import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useMemo} from "react";

const CostAwareSavingsTodayCard = ({dailySavings}) => {
    const formatDateForDailySavings = (date) => {
        return date.toISOString().split("T")[0];
    }

    const savingsForToday = useMemo(() => {
        const savings = dailySavings.get(formatDateForDailySavings(new Date()));
        if (!savings) return 0;
        else return savings.totalSavings;
    }, [dailySavings])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Savings today
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(savingsForToday * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareSavingsTodayCard;