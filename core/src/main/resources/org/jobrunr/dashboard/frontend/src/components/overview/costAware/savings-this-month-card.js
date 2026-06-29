import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useMemo} from "react";

// TODO for `${Math.round(totalSavings * 10000) / 10000}`, do we not have humanreadable numbers? Shall we add something for pricing?
// TODO overall I think we are not reusing enough of code by creating components or util functions
const CostAwareSavingsThisMonthCard = ({dailySavings}) => {
    const todayDate = new Date();

    const totalSavings = useMemo(() => {
        let total = 0;
        dailySavings.forEach((value, key) => {
            const date = new Date(key);
            if (date.getFullYear() === todayDate.getFullYear() && date.getMonth() === todayDate.getMonth()) {
                total += value.totalSavings;
            }
        })
        return total;
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