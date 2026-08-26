import {useEffect, useMemo, useRef, useState} from 'react';

import Box from "@mui/material/Box";
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import {useJobStats} from "../../../hooks/useJobStats";
import {BarChart} from "@mui/x-charts/BarChart";

const appendCapped = (prev, value, cap = 200) => [...prev, value].slice(-cap);

const pad2 = (n) => String(n).padStart(2, '0');

const formatAxisTime = (date) => {
    const d = date instanceof Date ? date : new Date(date);
    return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
};

const createInitialCategories = (now = Date.now(), count = 200, stepSeconds = 5) =>
    Array.from({length: count}, (_, i) => new Date(now - (count - i) * stepSeconds * 1000));

const RealtimeGraph = () => {
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});

    const [succeededChartData, setSucceededChartData] = useState(() => new Array(200).fill(0));
    const [failedChartData, setFailedChartData] = useState(() => new Array(200).fill(0));
    const [xAxisCategories, setXAxisCategories] = useState(() => createInitialCategories());

    const [stats, _] = useJobStats();

    useEffect(() => {
        const oldStats = oldStatsRef.current;

        if (!stats.succeeded || stats.succeeded < 1) return;
        if (!oldStats.succeeded || oldStats.succeeded < 1) {
            oldStatsRef.current = stats;
            return;
        }

        const amountSucceeded = (stats.succeeded + stats.allTimeSucceeded) - (oldStats.succeeded + oldStats.allTimeSucceeded);
        const amountFailed = stats.failed - oldStats.failed;

        if (!isNaN(amountSucceeded) && !isNaN(amountFailed) && amountSucceeded >= 0 && amountFailed >= 0) {
            setSucceededChartData(prev => appendCapped(prev, amountSucceeded));
            setFailedChartData(prev => appendCapped(prev, amountFailed));
            setXAxisCategories(prev => appendCapped(prev, new Date()));
        }

        oldStatsRef.current = stats;
    }, [stats]);

    const series = useMemo(() => [
        {data: failedChartData, label: 'Failed jobs', stack: 'jobs', color: '#E91E63'},
        {data: succeededChartData, label: 'Succeeded jobs', stack: 'jobs', color: '#66DA26'}
    ], [failedChartData, succeededChartData]);

    const xAxis = useMemo(() => [{
        scaleType: 'band',
        data: xAxisCategories,
        valueFormatter: formatAxisTime,
        categoryGapRatio: 0.1,
        barGapRatio: 0,
        tickLabelStyle: {display: 'none'},
        disableTicks: true
    }], [xAxisCategories]);

    const yAxis = useMemo(() => {
        const stackedMax = Math.max(
            ...succeededChartData.map((value, index) => value + failedChartData[index]),
            0
        );
        const yMax = Math.max(1, Math.ceil(stackedMax));
        return [{
            min: 0,
            max: yMax,
            tickMinStep: 1,
            domainLimit: 'strict',
            valueFormatter: (value) => String(Math.round(value))
        }];
    }, [succeededChartData, failedChartData]);

    return (
        <div className="row">
            <Box sx={{mt: 3, mb: 2}}>
                <Typography id="realtime-graph" variant="h5">Realtime graph</Typography>
            </Box>
            <Paper>
                <BarChart
                    height={500}
                    skipAnimation
                    series={series}
                    xAxis={xAxis}
                    yAxis={yAxis}
                />
            </Paper>
        </div>
    );
};

export default RealtimeGraph;