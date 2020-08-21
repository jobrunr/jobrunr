import React, {useRef, useState} from 'react';

import Box from "@material-ui/core/Box";
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import Chart from "react-apexcharts";
import ApexCharts from "apexcharts";
import statsState from "../../../StatsStateContext";

const RealtimeGraph = () => {
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});
    const succeededDataRef = useRef(getArrayWithLimitedLength(200));
    const failedDataRef = useRef(getArrayWithLimitedLength(200));
    const stats = statsState.useStatsState(RealtimeGraph);

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

    React.useEffect(() => {
        const oldStats = oldStatsRef.current;

        if (!stats.succeeded || stats.succeeded < 1) return;
        if (!oldStats.succeeded || oldStats.succeeded < 1) {
            oldStatsRef.current = {...stats, timestamp: new Date()};
            return;
        }

        const succeededData = succeededDataRef.current;
        const failedData = failedDataRef.current;
        const amountSucceeded = stats.succeeded - oldStats.succeeded;
        const amountFailed = stats.failed - oldStats.failed;

        if (!isNaN(amountSucceeded) && !isNaN(amountFailed)) {
            succeededData.push(amountSucceeded)
            failedData.push(amountFailed)
            ApexCharts.exec('processing-chart', 'updateSeries', [
                {data: failedData},
                {data: succeededData}
            ])
        }
        oldStatsRef.current = {...stats, timestamp: new Date()};
    }, [stats]);

    return (
        <div className="row">
            <Box my={3}>
                <Typography id="title" variant="h5">Realtime graph</Typography>
            </Box>
            <Paper>
                <Chart
                    options={graphState.options}
                    series={graphState.series}
                    type="bar"
                    height={500}
                />
            </Paper>
        </div>
    );

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
};

export default RealtimeGraph;