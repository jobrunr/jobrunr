import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import {ArrowBack, ArrowForward} from "@mui/icons-material";
import Card from "@mui/material/Card";
import {useEffect, useState} from "react";

const CostAwareYearlySavingsCard = ({yearlySavings}) => {
    const todayDate = new Date();
    const [selectedDateForYearly, setSelectedDateForYearly] = useState(new Date(new Date(todayDate).setFullYear(todayDate.getFullYear() - 1, 0, 1)))
    const [savingsForSelectedYear, setSavingsForSelectedYear] = useState(0);

    const formatDateForYearlySavings = (date) => {
        const dateParts = date.toISOString().split("T")[0].split("-");
        return dateParts[0];
    }

    useEffect(() => {
        const savings = yearlySavings.get(formatDateForYearlySavings(selectedDateForYearly));
        if (!savings) setSavingsForSelectedYear(0);
        else setSavingsForSelectedYear(savings.totalSavings);
    }, [selectedDateForYearly])

    return (
        <Card sx={{width: "215px"}}>
            <CardContent>
                <Typography color="textSecondary" gutterBottom>
                    Yearly Savings
                </Typography>
                <Typography variant="h6" component="h3">
                    During {formatDateForYearlySavings(selectedDateForYearly)}
                </Typography>
                <Typography variant="h5" component="h2">
                    ${Math.round(savingsForSelectedYear * 10000) / 10000}
                </Typography>

                <div style={{display: "flex", flexWrap: "wrap", gap: "16px", justifyContent: "space-between"}}>
                    <IconButton
                        disabled={
                            !Array.from(yearlySavings.keys()).some(key => {
                                const keyDate = new Date(new Date(key).setMonth(0, 1));
                                return keyDate.setHours(0, 0, 0, 0) < new Date(new Date(selectedDateForYearly).setMonth(0, 1)).setHours(0, 0, 0, 0);
                            })
                        }
                        onClick={() => {
                            const prevYear = new Date(selectedDateForYearly);
                            prevYear.setFullYear(selectedDateForYearly.getFullYear() - 1, 0, 1);
                            setSelectedDateForYearly(prevYear);
                        }}
                    >
                        <Tooltip title={"Previous Year"}>
                            <ArrowBack/>
                        </Tooltip>
                    </IconButton>
                    <IconButton
                        disabled={
                            !Array.from(yearlySavings.keys()).some(key => {
                                const keyDate = new Date(new Date(key).setMonth(0, 1));
                                return keyDate.setHours(0, 0, 0, 0) > new Date(new Date(selectedDateForYearly).setMonth(0, 1)).setHours(0, 0, 0, 0);
                            })
                        }
                        onClick={() => {
                            const nextYear = new Date(selectedDateForYearly);
                            nextYear.setFullYear(selectedDateForYearly.getFullYear() + 1, 0, 1);
                            setSelectedDateForYearly(nextYear);
                        }}
                    >
                        <Tooltip title={"Next Year"}>
                            <ArrowForward/>
                        </Tooltip>
                    </IconButton>
                </div>
            </CardContent>
        </Card>
    );
}

export default CostAwareYearlySavingsCard;