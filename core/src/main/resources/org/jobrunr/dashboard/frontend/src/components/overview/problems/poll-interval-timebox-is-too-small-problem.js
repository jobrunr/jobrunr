import TimeAgo from "react-timeago/lib";
import {DismissibleProblemNotification} from "./dismissible-problem-notification";

const PollIntervalInSecondsIsTooSmallProblem = (props) => {
    return (
        <DismissibleProblemNotification
            endpoint="/api/problems/poll-interval-in-seconds-is-too-small"
            refresh={props.refresh}
        >
            JobRunr detected that your poll interval in seconds setting is too small - are you perhaps scheduling a lot
            of <a href={"https://www.jobrunr.io/en/documentation/background-methods/recurring-jobs/"}>recurring jobs</a>?
            It means that the BackgroundJob Master node cannot execute all relevant tasks like scheduling recurring jobs
            and doing maintenance tasks like deleting succeeded jobs.
            <em><b>&nbsp;You are urged to increase your poll interval in seconds setting as soon as possible.</b></em>
            <br/>
            <ul>
                {props.problem.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet.map((problem, index) => {
                    return <li key={index}>{problem.owner} has a poll interval in seconds that is too small.
                        &nbsp;The poll interval in seconds time box was exceeded <TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(problem.createdAt)}
                            title={new Date(problem.createdAt).toString()}/>.</li>
                })}
            </ul>
        </DismissibleProblemNotification>
    );
};

export default PollIntervalInSecondsIsTooSmallProblem;