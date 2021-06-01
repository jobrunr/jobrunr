import React from 'react';
import {Typography} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";
import statsState from "../../StatsStateContext";

const useStyles = makeStyles(theme => ({
    footer: {
        paddingTop: '1rem',
        width: '100%',
        display: 'inline-block'
    }
}));

export default function VersionFooter() {

    const classes = useStyles();
    const stats = statsState.getStats();

    const [jobRunrInfo, setJobRunrInfo] = React.useState({version: '0.0.0-SNAPSHOT', succeededJobs: 0});


    React.useEffect(() => {
        fetch(`/api/version`)
            .then(res => res.json())
            .then(response => {
                setJobRunrInfo(response);
            })
            .catch(error => console.log(error));
    }, []);


    return (
        <Typography align="center" className={classes.footer} variant="caption">
            Processed {(stats.succeeded + stats.allTimeSucceeded)} jobs with <span style={{color: 'red'}}>♥</span> using
            JobRunr {jobRunrInfo.version}
        </Typography>
    )
}