import TimeAgo from "react-timeago";
import {DismissibleClusterProblemNotification, getEndpointToDismissProblem} from "./dismissible-notification";

export const PollIntervalInSecondsIsTooSmallProblemNotification = ({problem, ...rest}) => {
    return (
        <DismissibleClusterProblemNotification
            title="Poll Interval Exceeded"
            endpoint={getEndpointToDismissProblem(problem)}
            date={problem.createdAt}
            read={problem.read}
            {...rest}
        >
            JobRunr detected that your poll interval in seconds setting is too small - are you perhaps scheduling a lot
            of <a href={"https://www.jobrunr.io/en/documentation/background-methods/recurring-jobs/"}>recurring jobs</a>?
            It means that the BackgroundJob Master node cannot execute all relevant tasks like scheduling recurring jobs
            and doing maintenance tasks like deleting succeeded jobs.
            <em><b>&nbsp;You are urged to increase your poll interval in seconds setting as soon as possible.</b></em>
            <br/>
            <ul>
                {problem.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet.map((issue, index) => {
                    return <li key={issue.owner}>{issue.owner} has a poll interval in seconds that is too small.
                        &nbsp;The poll interval in seconds time box was exceeded <TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(issue.createdAt)}
                            title={new Date(issue.createdAt).toString()}/>.</li>
                })}
            </ul>
        </DismissibleClusterProblemNotification>
    );
};