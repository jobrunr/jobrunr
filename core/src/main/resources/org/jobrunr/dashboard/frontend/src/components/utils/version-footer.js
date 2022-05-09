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
    const [jobRunrInfo, setJobRunrInfo] = React.useState({version: '0.0.0-SNAPSHOT', allowAnonymousDataUsage: false, clusterId: undefined, storageProviderType: undefined});

    React.useEffect(() => {
        fetch(`/api/version`)
            .then(res => res.json())
            .then(response => setJobRunrInfo(response))
            .catch(error => console.log(error));
    }, []);

    React.useEffect(() => {
        if(jobRunrInfo.allowAnonymousDataUsage && stats.backgroundJobServers) {
            const anonymousUsageDataSent = localStorage.getItem('anonymousUsageDataSent');
            if(!anonymousUsageDataSent || Math.abs(new Date() - Date.parse(anonymousUsageDataSent)) > (1000 * 60 * 60 * 4)) {
                let url = `https://api.jobrunr.io/api/analytics/jobrunr/report`;
                url += `?clusterId=${jobRunrInfo.clusterId}&currentVersion=${jobRunrInfo.version}&storageProviderType=${jobRunrInfo.storageProviderType}`;
                url += `&amountOfBackgroundJobServers=${stats.backgroundJobServers}&succeededJobCount=${(stats.succeeded + stats.allTimeSucceeded)}`;
                fetch(url)
                    .then(res => console.log(`JobRunr ${jobRunrInfo.version} - Thank you for sharing anonymous data!`))
                    .catch(error => console.log(`JobRunr ${jobRunrInfo.version} - Could not share anonymous data :-(!`));

                localStorage.setItem('anonymousUsageDataSent', new Date().toISOString());
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [jobRunrInfo]);

    return (
        <>
            <Typography align="center" className={classes.footer} variant="caption">
                Processed {(stats.succeeded + stats.allTimeSucceeded)} jobs with <span style={{color: 'red'}}>♥</span> using
                JobRunr {jobRunrInfo.version}.<br />
                Support open-source development and <a href="https://www.jobrunr.io/en/about/#eco-friendly-software" target="_blank" rel="noreferrer">our planet</a> by purchasing a <a href="https://www.jobrunr.io/en/pricing/" target="_blank" rel="noreferrer">JobRunr Pro</a> license.
            </Typography>
        </>
    )
}