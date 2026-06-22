import Box from "@mui/material/Box";
import {lazy, Suspense, useMemo} from "react";
import LoadingIndicator from "../../LoadingIndicator.js";
import Typography from "@mui/material/Typography";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import TableBody from "@mui/material/TableBody";

const ApexChartsModule = import("apexcharts");
const Chart = lazy(() => import("react-apexcharts"));

const CostAwareMonthlySavingsTab = ({monthlySavings}) => {
    const sortedDates = useMemo(() => {
        return Array.from(monthlySavings.keys()).sort();
    }, [monthlySavings]);

    const monthsArray = useMemo(() => {
        if (sortedDates.length === 0) return [];
        const minDate = sortedDates[0];
        const maxDate = sortedDates[sortedDates.length - 1];
        const months = [];

        const endDate = new Date(minDate);
        for (let dt = new Date(maxDate); dt >= endDate; dt.setMonth(dt.getMonth() - 1)) {
            months.push(new Date(dt));
        }
        return months;
    }, [sortedDates]);

    const formatDateForMonthlySavings = (date) => {
        const dateParts = date.toISOString().split("T")[0].split("-");
        return dateParts[0] + "-" + dateParts[1];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {month: "long", year: "numeric"});
    }

    const chartData = useMemo(() => {
        if (!monthlySavings) return [];

        return [...monthlySavings.entries()]
            .sort((a, b) => new Date(a[0]) - new Date(b[0]))
            .map(([date, value]) => [
                new Date(date).getTime(),
                parseFloat(value.totalSavings.toFixed(4))
            ]);
    }, [monthlySavings]);

    const chartOptions = useMemo(() => {
        return {
            chart: {
                id: "monthly-savings-chart",
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
                type: "numeric",
                title: {text: "Date"},
                tickAmount: monthlySavings.size - 1,
                labels: {
                    formatter: function (val) {
                        if (!val) return "";
                        const date = new Date(val);
                        return date.toLocaleDateString(undefined, {
                            month: "short",
                            year: "numeric",
                            timeZone: "UTC"
                        });
                    }
                }
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
            {monthlySavings.size > 1 && <>
                <Typography>Monthly Savings Overview</Typography>
                <Suspense fallback={<LoadingIndicator/>}>
                    <Chart
                        options={chartOptions}
                        series={[{name: "Monthly Savings", data: chartData}]}
                        type="scatter"
                        height={300}
                    />
                </Suspense>
            </>}
            <Typography>Monthly Savings Breakdown</Typography>
            <Table style={{width: "100%"}} aria-label="simple table">
                <TableHead>
                    <TableRow>
                        <TableCell>Month</TableCell>
                        <TableCell>Total Savings</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {monthsArray.map((date) => (
                        <TableRow>
                            <TableCell>{formatDateToHumanReadable(date)}</TableCell>
                            <TableCell>${Math.round((monthlySavings.get(formatDateForMonthlySavings(date))?.totalSavings ?? 0) * 10000) / 10000}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Box>
    )
}

export default CostAwareMonthlySavingsTab;