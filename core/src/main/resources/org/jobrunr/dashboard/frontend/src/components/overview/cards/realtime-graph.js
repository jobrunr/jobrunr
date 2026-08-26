import {useEffect, useRef, useState} from 'react';

import Box from "@mui/material/Box";
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import {useJobStats} from "../../../hooks/useJobStats";
import {BarChart} from "@mui/x-charts/BarChart";

const xAxisCategories = Array.from({length: 200}, (_, i) => "(t-" + ((200 * 5) - (i * 5)) + "s)");
const appendCapped = (prev, value, cap = 200) => [...prev, value].slice(-cap);

const RealtimeGraph = () => {
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});

    const [succeededChartData, setSucceededChartData] = useState(() => new Array(200).fill(0));
    const [failedChartData, setFailedChartData] = useState(() => new Array(200).fill(0));

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
        }

        oldStatsRef.current = stats;
    }, [stats]);

    return (
        <div className="row">
            <Box sx={{mt: 3, mb: 2}}>
                <Typography id="realtime-graph" variant="h5">Realtime graph</Typography>
            </Box>
            <Paper>
                <BarChart
                    height={500}
                    skipAnimation
                    series={[
                        {data: failedChartData, label: 'Failed jobs', stack: 'jobs', color: '#E91E63'},
                        {data: succeededChartData, label: 'Succeeded jobs', stack: 'jobs', color: '#66DA26'}
                    ]}
                    xAxis={[{
                        scaleType: 'band',
                        data: xAxisCategories,
                        categoryGapRatio: 0.1,
                        barGapRatio: 0,
                        tickLabelStyle: {display: 'none'},
                        disableTicks: true
                    }]}
                />
            </Paper>
        </div>
    );
};

export default RealtimeGraph;