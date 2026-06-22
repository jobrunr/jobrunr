import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useMemo} from "react";

const CostAwareSavingsThisYearCard = ({dailySavings, monthlySavings}) => {
    const todayDate = new Date();

    const totalSavings = useMemo(() => {
        let total = 0;
        // Current month
        dailySavings.forEach((value, key) => {
            const date = new Date(key);
            if (date.getFullYear() === todayDate.getFullYear() && date.getMonth() === todayDate.getMonth()) {
                total += value.totalSavings;
            }
        })
        // Rest of the months this year
        monthlySavings.forEach((value, key) => {
            const date = new Date(key);
            if (date.getFullYear() === todayDate.getFullYear()) {
                total += value.totalSavings;
            }
        })
        return total;
    }, [monthlySavings])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Savings this year
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(totalSavings * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareSavingsThisYearCard;