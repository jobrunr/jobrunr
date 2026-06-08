import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareRollingSavingsCard = ({dailySavings}) => {
    const [totalSavings, setTotalSavings] = useState(0);

    useEffect(() => {
        let total = 0;
        dailySavings.forEach((value) => {
            total += value.totalSavings;
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
                    Since {new Date(Array.from(dailySavings.keys()).sort()[0]).toLocaleDateString(undefined, {day: "numeric", month: "long", year: "numeric"})}
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(totalSavings * 10000) / 10000}
                </Typography>
            </CardContent>
        </Card>
    );
}

export default CostAwareRollingSavingsCard;