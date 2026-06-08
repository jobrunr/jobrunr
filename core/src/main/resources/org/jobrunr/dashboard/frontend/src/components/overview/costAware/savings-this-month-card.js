import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareSavingsThisMonthCard = ({dailySavings}) => {
    const todayDate = new Date();
    const [totalSavings, setTotalSavings] = useState(0);

    useEffect(() => {
        let total = 0;
        dailySavings.forEach((value, key) => {
            const date = new Date(key);
            if (date.getFullYear() === todayDate.getFullYear() && date.getMonth() === todayDate.getMonth()) {
                total += value.totalSavings;
            }
        })
        setTotalSavings(total);
    }, [dailySavings])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Savings this month
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(totalSavings * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareSavingsThisMonthCard;