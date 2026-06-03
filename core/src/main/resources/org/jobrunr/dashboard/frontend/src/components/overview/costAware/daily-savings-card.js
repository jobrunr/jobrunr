import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import {ArrowBack, ArrowForward} from "@mui/icons-material";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareDailySavingsCard = ({dailySavings}) => {
    const todayDate = new Date();
    const [selectedDateForDaily, setSelectedDateForDaily] = useState(todayDate);
    const [savingsForSelectedDay, setSavingsForSelectedDay] = useState(0);

    const formatDateForDailySavings = (date) => {
        return date.toISOString().split("T")[0];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {day: "numeric", month: "long", year: "numeric"});
    }

    useEffect(() => {
        const savings = dailySavings.get(formatDateForDailySavings(selectedDateForDaily));
        if (!savings) setSavingsForSelectedDay(0);
        else setSavingsForSelectedDay(savings.totalSavings);
    }, [selectedDateForDaily])

    return (
        <Card sx={{minWidth: "215px", minHeight: '105px'}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Daily Savings
                </Typography>
                <Typography variant="h6" component="h3">
                    On {formatDateToHumanReadable(selectedDateForDaily)}
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(savingsForSelectedDay * 10000) / 10000}
                </Typography>

                <div style={{display: "flex", flexWrap: "wrap", gap: "16px", justifyContent: "space-between"}}>
                    <IconButton
                        disabled={!dailySavings.get(formatDateForDailySavings(new Date(new Date(selectedDateForDaily).setDate(selectedDateForDaily.getDate() - 1))))}
                        onClick={() => {
                            const prevDay = new Date(selectedDateForDaily);
                            prevDay.setDate(prevDay.getDate() - 1);
                            setSelectedDateForDaily(prevDay);
                        }}
                    >
                        <Tooltip title={"Previous Day"}>
                            <ArrowBack/>
                        </Tooltip>
                    </IconButton>
                    <IconButton
                        disabled={!dailySavings.get(formatDateForDailySavings(new Date(new Date(selectedDateForDaily).setDate(selectedDateForDaily.getDate() + 1))))}
                        onClick={() => {
                            const nextDay = new Date(selectedDateForDaily);
                            nextDay.setDate(nextDay.getDate() + 1);
                            setSelectedDateForDaily(nextDay);
                        }}
                    >
                        <Tooltip title={"Next Day"}>
                            <ArrowForward/>
                        </Tooltip>
                    </IconButton>
                </div>
            </CardContent>
        </Card>
    );
}

export default CostAwareDailySavingsCard;