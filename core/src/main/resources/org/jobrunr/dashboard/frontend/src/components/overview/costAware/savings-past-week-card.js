import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useMemo} from "react";

const CostAwareSavingsPastWeekCard = ({dailySavings}) => {
    const todayDate = new Date();

    const totalSavings = useMemo(() => {
        let total = 0;
        dailySavings.forEach((value, key) => {
            const date = new Date(key);
            const dateLastWeek = new Date(todayDate);
            dateLastWeek.setDate(todayDate.getDate() - 7);
            dateLastWeek.setHours(0, 0, 0);
            if (date >= dateLastWeek) {
                total += value.totalSavings;
            }
        })
        return total;
    }, [dailySavings])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Savings for past 7 days
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(totalSavings * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareSavingsPastWeekCard;