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

const CostAwareYearlySavingsTab = ({yearlySavings}) => {
    const minDate = Array.from(yearlySavings.keys()).sort()[0];
    const maxDate = Array.from(yearlySavings.keys()).sort()[yearlySavings.size - 1];
    const [yearsArray, setYearsArray] = useState([]);

    useEffect(() => {
        if (minDate && maxDate) {
            const years = [];
            for (const dt = new Date(maxDate); dt >= new Date(minDate); dt.setFullYear(dt.getFullYear() - 1)) {
                years.push(new Date(dt));
            }
            setYearsArray(years);
        }
    }, [minDate, maxDate])

    const formatDateForYearlySavings = (date) => {
        const dateParts = date.toISOString().split("T")[0].split("-");
        return dateParts[0];
    }

    const formatDateToHumanReadable = (date) => {
        return date.toLocaleDateString(undefined, {year: "numeric"});
    }

    const chartData = useMemo(() => {
        if (!yearlySavings) return [];

        return [...yearlySavings.entries()]
            .sort((a, b) => new Date(a[0]) - new Date(b[0]))
            .map(([date, value]) => [
                new Date(date).getTime(),
                parseFloat(value.totalSavings.toFixed(4))
            ]);
    }, [yearlySavings]);

    const chartOptions = useMemo(() => {
        return {
            chart: {
                id: "yearly-savings-chart",
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
                tickAmount: yearlySavings.size - 1,
                labels: {
                    formatter: function (val) {
                        if (!val) return "";
                        const date = new Date(val);
                        return date.toLocaleDateString(undefined, {
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
            {yearlySavings.size > 1 && <>
                <Typography>Yearly Savings Overview</Typography>
                <Suspense fallback={<LoadingIndicator/>}>
                    <Chart
                        options={chartOptions}
                        series={[{name: "Yearly Savings", data: chartData}]}
                        type="scatter"
                        height={300}
                    />
                </Suspense>
            </>}
            <Typography>Yearly Savings Breakdown</Typography>
            <Table style={{width: "100%"}} aria-label="simple table">
                <TableHead>
                    <TableRow>
                        <TableCell>Year</TableCell>
                        <TableCell>Total Savings</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {yearsArray.map((date) => (
                        <TableRow>
                            <TableCell>{formatDateToHumanReadable(date)}</TableCell>
                            <TableCell>${Math.round((yearlySavings.get(formatDateForYearlySavings(date))?.totalSavings ?? 0) * 10000) / 10000}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Box>
    )
}

export default CostAwareYearlySavingsTab;