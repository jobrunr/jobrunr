import {useEffect, useRef, useState} from 'react';
import TimeAgo from "react-timeago/lib";
import statsState from "../../../StatsStateContext";
import StatCard from "./stat-card.js";

const EstimatedProcessingTimeCard = () => {
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    const [stats, setStats] = useState(statsState.getStats());
    useEffect(() => {
        statsState.addListener(setStats);
        return () => statsState.removeListener(setStats);
    }, [])

    const processingTimeRef = useRef(<>Calculating...</>);
    useEffect(() => {
        if (stats.estimation.processingDone) {
            processingTimeRef.current = <>All done!</>;
        } else {
            if (stats.estimation.estimatedProcessingTimeAvailable) {
                const estimatedProcessingTimeDate = new Date(stats.estimation.estimatedProcessingFinishedAt);
                processingTimeRef.current =
                    <TimeAgo date={estimatedProcessingTimeDate} title={estimatedProcessingTimeDate.toString()}
                             formatter={timeAgoFormatter}/>;
            } else {
                processingTimeRef.current = <>Calculating...</>;
            }
        }
    }, [stats]);

    return (
        <StatCard title="Estimated processing time">
            {processingTimeRef.current}
        </StatCard>
    );
};

export default EstimatedProcessingTimeCard;