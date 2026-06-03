import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import {ArrowBack, ArrowForward} from "@mui/icons-material";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareMonthlySavingsCard = ({monthlySavings}) => {
    const todayDate = new Date();
    const [selectedDateForMonthly, setSelectedDateForMonthly] = useState(new Date(new Date(todayDate).setMonth(todayDate.getMonth() - 1, 1)));
    const [savingsForSelectedMonth, setSavingsForSelectedMonth] = useState(0);

    const formatDateForMonthlySavings = (date) => {
        const dateParts = date.toISOString().split("T")[0].split("-");
        return dateParts[0] + "-" + dateParts[1];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {month: "long", year: "numeric"});
    }

    useEffect(() => {
        const savings = monthlySavings.get(formatDateForMonthlySavings(selectedDateForMonthly));
        if (!savings) setSavingsForSelectedMonth(0);
        else setSavingsForSelectedMonth(savings.totalSavings);
    }, [selectedDateForMonthly])

    return (
        <Card sx={{width: "215px"}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Monthly Savings
                </Typography>
                <Typography variant="h6" component="h3">
                    During {formatDateToHumanReadable(selectedDateForMonthly)}
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(savingsForSelectedMonth * 10000) / 10000}
                </Typography>

                <div style={{display: "flex", flexWrap: "wrap", gap: "16px", justifyContent: "space-between"}}>
                    <IconButton
                        disabled={
                            !Array.from(monthlySavings.keys()).some(key => {
                                const keyDate = new Date(new Date(key).setDate(1));
                                return keyDate.setHours(0, 0, 0, 0) < new Date(new Date(selectedDateForMonthly).setDate(1)).setHours(0, 0, 0, 0);
                            })
                        }
                        onClick={() => {
                            const prevMonth = new Date(selectedDateForMonthly);
                            prevMonth.setMonth(prevMonth.getMonth() - 1, 1);
                            setSelectedDateForMonthly(prevMonth);
                        }}
                    >
                        <Tooltip title={"Previous Month"}>
                            <ArrowBack/>
                        </Tooltip>
                    </IconButton>
                    <IconButton
                        disabled={
                            !Array.from(monthlySavings.keys()).some(key => {
                                const keyDate = new Date(new Date(key).setDate(1));
                                return keyDate.setHours(0, 0, 0, 0) > new Date(new Date(selectedDateForMonthly).setDate(1)).setHours(0, 0, 0, 0);
                            })
                        }
                        onClick={() => {
                            const nextMonth = new Date(selectedDateForMonthly);
                            nextMonth.setMonth(nextMonth.getMonth() + 1, 1);
                            setSelectedDateForMonthly(nextMonth);
                        }}
                    >
                        <Tooltip title={"Next Month"}>
                            <ArrowForward/>
                        </Tooltip>
                    </IconButton>
                </div>
            </CardContent>
        </Card>
    );
}

export default CostAwareMonthlySavingsCard;