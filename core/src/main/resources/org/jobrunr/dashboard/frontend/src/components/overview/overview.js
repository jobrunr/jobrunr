import React, {useContext, useRef, useState} from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Box from "@material-ui/core/Box";
import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import TimeAgo from "react-timeago/lib";
import Chart from "react-apexcharts";
import ApexCharts from "apexcharts";

import {StatsContext} from "../../layouts/Admin";

const useStyles = makeStyles(theme => ({
    metadata: {
        display: 'flex',
    },
    infocard: {
        minWidth: '230px',
        minHeight: '105px',
        marginRight: '20px'
    },
}));

const Overview = () => {
    const classes = useStyles();
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    const statsContext = useContext(StatsContext);
    const oldStatsRef = useRef({enqueued: 0, failed: 0, succeeded: 0});
    const succeededDataRef = useRef(getArrayWithLimitedLength(200));
    const failedDataRef = useRef(getArrayWithLimitedLength(200));
    const processingTimeRef = useRef();
    const {stats} = statsContext;

    const [state] = useState({
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
        if (!oldStats.succeeded || oldStats.succeeded < 1 || stats.succeeded == oldStats.succeeded) {
            oldStatsRef.current = {...stats, timestamp: new Date()};
            return;
        }

        const succeededData = succeededDataRef.current;
        const failedData = failedDataRef.current;
        const amountSucceeded = stats.succeeded - oldStats.succeeded;
        const amountFailed = stats.failed - oldStats.failed;

        const timeDiff = new Date() - oldStats.timestamp;
        if (!isNaN(timeDiff)) {
            const amountSucceededPerSecond = amountSucceeded * 1000 / timeDiff;
            const estimatedProcessingTime = stats.enqueued / amountSucceededPerSecond
            console.log("processingTime: ", timeDiff, estimatedProcessingTime, (new Date(new Date().getTime() + (estimatedProcessingTime * 1000))))
            processingTimeRef.current = (new Date(new Date().getTime() + (estimatedProcessingTime * 1000)));
        }


        if (!isNaN(amountSucceeded)) {
            succeededData.push(amountSucceeded)
            failedData.push(amountFailed)
            console.log("Updated stats from overview", timeDiff);
            ApexCharts.exec('processing-chart', 'updateSeries', [
                {data: failedData},
                {data: succeededData}
            ])
        }
        oldStatsRef.current = {...stats, timestamp: new Date()};
    }, [stats]);

    const [isLoading, setIsLoading] = React.useState(true);
    const [servers, setServers] = React.useState([{firstHeartbeat: undefined}]);


    React.useEffect(() => {
        fetch(`/api/servers`)
            .then(res => res.json())
            .then(response => {
                const serversSorted = response;
                serversSorted.sort((a, b) => a.firstHeartbeat > b.firstHeartbeat)
                setServers(serversSorted);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    return (
        <div className="app">
            <div className="row">
                <Box my={3}>
                    <Typography id="title" variant="h4">Dashboard</Typography>
                </Box>
            </div>
            <div className={classes.metadata}>
                <Card className={classes.infocard}>
                    <CardContent>
                        <Typography className={classes.title} color="textSecondary" gutterBottom>
                            Estimated processing time
                        </Typography>
                        <Typography variant="h5" component="h2">
                            {processingTimeRef.current
                                ? <TimeAgo date={processingTimeRef.current} formatter={timeAgoFormatter}/>
                                : <>Calculating...</>
                            }
                        </Typography>
                    </CardContent>
                </Card>
                <Card className={classes.infocard}>
                    <CardContent>
                        <Typography className={classes.title} color="textSecondary" gutterBottom>
                            Uptime
                        </Typography>
                        <Typography variant="h5" component="h2">
                            <TimeAgo date={new Date(servers[0].firstHeartbeat)} formatter={timeAgoFormatter}/>
                        </Typography>
                    </CardContent>
                </Card>
                <Card className={classes.infocard}>
                    <CardContent>
                        <Typography className={classes.title} color="textSecondary" gutterBottom>
                            Avg Load
                        </Typography>
                        <Typography variant="h5" component="h2">
                            {servers[0].firstHeartbeat}
                        </Typography>
                    </CardContent>
                </Card>
                <Card className={classes.infocard}>
                    <CardContent>
                        <Typography className={classes.title} color="textSecondary" gutterBottom>
                            Avg Memory Usage
                        </Typography>
                        <Typography variant="h5" component="h2">
                            {servers[0].firstHeartbeat}
                        </Typography>
                    </CardContent>
                </Card>
            </div>
            <div className="row">
                <Box my={3}>
                    <Typography id="title" variant="h5">Realtime graph</Typography>
                </Box>
                <div className="mixed-chart">
                    <Chart
                        options={state.options}
                        series={state.series}
                        type="bar"
                        height={500}
                    />
                </div>
            </div>
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

export default Overview;