import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareAllTimeSavingsCard = ({dailySavings, monthlySavings, yearlySavings}) => {
    const [totalSavings, setTotalSavings] = useState(0);

    useEffect(() => {
        let total = 0;
        const allYears = [];
        const allMonths = [];
        yearlySavings.forEach((value, key) => {
            allYears.push(key);
            total += value.totalSavings;
        })
        monthlySavings.forEach((value, key) => {
            allMonths.push(key);
            if (!allYears.includes(new Date(key).getFullYear())) {
                total += value.totalSavings;
            }
        })
        dailySavings.forEach((value, key) => {
            const date = new Date(key);
            let month = ("" + (date.getMonth() + 1)).padStart(2, '0');
            if (!allMonths.includes(date.getFullYear() + "-" + month)) {
                total += value.totalSavings;
            }
        })
        setTotalSavings(total);
    }, [dailySavings])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Total savings
                </Typography>
                <Typography variant="h6" component="h3">
                    All Time
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(totalSavings * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareAllTimeSavingsCard;