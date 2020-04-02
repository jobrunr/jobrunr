import React, {useContext, useState} from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Chart from "react-apexcharts";

import {StatsContext} from "../../layouts/Admin";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    content: {
        width: '100%',
    },
}));

const Overview = (props) => {
    const classes = useStyles();
    const statsContext = useContext(StatsContext);
    const {stats} = statsContext;
    const [state, setState] = useState({
        options: {
            chart: {
                id: "processing-chart",
                width: '100%'
            },
            xaxis: {
                categories: [-75, -70, -65, -60, -55, -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0]
            }
        },
        series: [
            {
                name: "series-1",
                data: [30, 40, 45, 50, 49, 60, 70, 91]
            }
        ]
    });

    React.useEffect(() => {
        console.log("Updated stats from overview", stats);
        setState({
            options: {
                ...state.options,
            },
            series: [
                {
                    name: "series-1",
                    data: [30, 40, 45, 50, 49, 60, 70, 23]
                }
            ]
        });
    }, [stats]);

    return (
        <div className="app">
            <div className="row">
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
};

export default Overview;