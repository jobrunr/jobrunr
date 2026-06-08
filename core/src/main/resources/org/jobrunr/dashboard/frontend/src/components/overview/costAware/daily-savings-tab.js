import Box from "@mui/material/Box";
import {lazy, Suspense, useEffect, useMemo, useState} from "react";
import LoadingIndicator from "../../LoadingIndicator.js";
import Typography from "@mui/material/Typography";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import TableBody from "@mui/material/TableBody";

const ApexChartsModule = import("apexcharts");
const Chart = lazy(() => import("react-apexcharts"));

const CostAwareDailySavingsTab = ({dailySavings}) => {
    const minDate = Array.from(dailySavings.keys()).sort()[0];
    const maxDate = Array.from(dailySavings.keys()).sort()[dailySavings.size - 1];
    const [daysArray, setDaysArray] = useState([]);

    useEffect(() => {
        if (minDate && maxDate) {
            const days = [];
            for (const dt = new Date(maxDate); dt >= new Date(minDate); dt.setDate(dt.getDate() - 1)) {
                days.push(new Date(dt));
            }
            setDaysArray(days);
        }
    }, [minDate, maxDate])

    const formatDateForDailySavings = (date) => {
        return date.toISOString().split("T")[0];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {day: "numeric", month: "long", year: "numeric"});
    }

    const chartData = useMemo(() => {
        if (!dailySavings) return [];

        return [...dailySavings.entries()]
            .sort((a, b) => new Date(a[0]) - new Date(b[0]))
            .map(([date, value]) => [
                new Date(date).getTime(),
                parseFloat(value.totalSavings.toFixed(4))
            ]);
    }, [dailySavings]);

    const chartOptions = useMemo(() => {
        return {
            chart: {
                id: "daily-savings-chart",
                type: "scatter",
                zoom: {
                    enabled: false
                },
                animations: {
                    enabled: false
                },
                toolbar: {
                    show: false
                },
            },
            dataLabels: {
                enabled: false
            },
            xaxis: {
                type: "datetime",
                title: {text: "Date"}
            },
            yaxis: {
                title: {text: "Savings ($)"},
                labels: {
                    formatter: (val) => val.toFixed(2)
                }
            },
            tooltip: {
                enabled: false
            }
        };
    }, []);

    return (
        <Box>
            <Typography>Daily Savings Overview</Typography>
            <Suspense fallback={<LoadingIndicator/>}>
                <Chart
                    options={chartOptions}
                    series={[{name: "Daily Savings", data: chartData}]}
                    type="scatter"
                    height={300}
                />
            </Suspense>
            <Typography>Daily Savings Breakdown</Typography>
            <Table style={{width: "100%"}} aria-label="simple table">
                <TableHead>
                    <TableRow>
                        <TableCell>Date</TableCell>
                        <TableCell>Total Savings</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {daysArray.map((date) => (
                        <TableRow>
                            <TableCell>{formatDateToHumanReadable(date)}</TableCell>
                            <TableCell>${Math.round((dailySavings.get(formatDateForDailySavings(date))?.totalSavings ?? 0) * 10000) / 10000}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Box>
    )
}

export default CostAwareDailySavingsTab;