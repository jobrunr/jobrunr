import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import {JobState} from "./job-state";

const getDuration = (duration) => {
    try {
        const actualDuration = duration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(duration) : duration;
        const totalSeconds = actualDuration.toFixed(2);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds - (hours * 3600)) / 60);
        const seconds = totalSeconds - (hours * 3600) - (minutes * 60);

        let result = "";
        if (hours > 0) {
            result += hours + " hours, "
        }
        if (minutes > 0) {
            result += minutes + ((minutes > 1) ? " minutes" : " minute")
        }
        if (minutes > 0 && seconds > 0) {
            result += " and "
        }
        if (seconds > 0) {
            result += seconds.toFixed(2) + " seconds"
        } else if (result === '') {
            result += "less than 10ms"
        }
        return result;
    } catch (e) {
        console.warn("Could not parse " + duration + ". If you want pretty dates in the succeeded view, your durations must be formatted as either seconds or ISO8601 duration format (e.g. PT5M33S). This is a settings in Jackson.");
        return duration + " (unsupported duration format - see console)";
    }
}

const Succeeded = ({jobState}) => {
    return (
        <JobState
            state="success"
            title="Job Processing Succeeded"
            date={jobState.createdAt}
        >
            <ul style={{margin: 0, padding: 0, listStylePosition: "inside"}}>
                <li>Latency duration: {getDuration(jobState.latencyDuration)}</li>
                <li>Process duration: {getDuration(jobState.processDuration)}</li>
            </ul>
        </JobState>
    )
};

export default Succeeded;