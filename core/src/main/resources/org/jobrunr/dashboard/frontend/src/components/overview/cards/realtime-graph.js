import {lazy, Suspense, useEffect, useRef, useState} from 'react';

import Box from "@mui/material/Box";
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import LoadingIndicator from "../../LoadingIndicator";
import {useJobStats} from "../../../hooks/useJobStats";

function getArrayWithLimitedLength(length) {
    const array = [];

    array.push = function () {
        if (this.length >= length) {
            this.shift();
        }
        return Array.prototype.push.apply(this, arguments);
    }

    for (let i = 0; i < length; i++) {
        array.push(0);
    }

    return array;

}

const ApexChartsModule = import("apexcharts");
const Chart = lazy(() => import("react-apexcharts"));

const RealtimeGraph = () => {
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});
    const succeededDataRef = useRef(getArrayWithLimitedLength(200));
    const failedDataRef = useRef(getArrayWithLimitedLength(200));

    const [stats, _] = useJobStats();

    const [graphState] = useState({
        options: {
            chart: {
                id: "processing-chart",
                type: 'bar',
                stacked: true,
                width: '100%',
                animations: {
                    enabled: false
                },
                toolbar: {
                    show: false
                }
            },
            dataLabels: {
                enabled: false
            },
            plotOptions: {
                bar: {
                    columnWidth: '90%'
                }
            },
            xaxis: {
                labels: {
                    show: false
                }
            },
            colors: ['#E91E63', '#66DA26']
        },
        series: [
            {name: "Failed jobs", data: []},
            {name: "Succeeded jobs", data: []}
        ]
    });

    useEffect(() => {
        const oldStats = oldStatsRef.current;

        if (!stats.succeeded || stats.succeeded < 1) return;
        if (!oldStats.succeeded || oldStats.succeeded < 1) {
            oldStatsRef.current = stats;
            return;
        }

        const succeededData = succeededDataRef.current;
        const failedData = failedDataRef.current;
        const amountSucceeded = (stats.succeeded + stats.allTimeSucceeded) - (oldStats.succeeded + oldStats.allTimeSucceeded);
        const amountFailed = stats.failed - oldStats.failed;

        if (!isNaN(amountSucceeded) && !isNaN(amountFailed) && amountSucceeded >= 0 && amountFailed >= 0) {
            succeededData.push(amountSucceeded)
            failedData.push(amountFailed)
            ApexChartsModule.then(({default: ApexCharts}) => {
                ApexCharts.exec('processing-chart', 'updateSeries', [
                    {name: "Failed jobs", data: failedData},
                    {name: "Succeeded jobs", data: succeededData}
                ])
            })
        }
        oldStatsRef.current = stats;
    }, [stats]);

    return (
        <div className="row">
            <Box my={3}>
                <Typography id="realtime-graph" variant="h5">Realtime graph</Typography>
            </Box>
            <Paper>
                <Suspense fallback={<LoadingIndicator/>}>
                    <Chart
                        options={graphState.options}
                        series={graphState.series}
                        type="bar"
                        height={500}
                    />
                </Suspense>
            </Paper>
        </div>
    );
};

export default RealtimeGraph;