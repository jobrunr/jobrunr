import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareSavingsTodayCard = ({dailySavings}) => {
    const todayDate = new Date();
    const [savingsForSelectedDay, setSavingsForSelectedDay] = useState(0);

    const formatDateForDailySavings = (date) => {
        return date.toISOString().split("T")[0];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {day: "numeric", month: "long", year: "numeric"});
    }

    useEffect(() => {
        const savings = dailySavings.get(formatDateForDailySavings(todayDate));
        if (!savings) setSavingsForSelectedDay(0);
        else setSavingsForSelectedDay(savings.totalSavings);
    }, [todayDate])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Savings today
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(savingsForSelectedDay * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareSavingsTodayCard;