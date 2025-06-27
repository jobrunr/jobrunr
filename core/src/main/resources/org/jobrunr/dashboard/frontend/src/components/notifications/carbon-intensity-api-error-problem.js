import {DismissibleClusterProblemNotification, getEndpointToDismissProblem} from "./dismissible-notification";
import TimeAgo from "react-timeago/lib";

export const CarbonIntensityApiErrorProblem = ({problem, ...rest}) => {
    return (
        <DismissibleClusterProblemNotification
            endpoint={getEndpointToDismissProblem(problem)}
            severity="warning"
            title="Carbon Aware API Warning"
            date={problem.createdAt}
            read={problem.read}
            {...rest}
        >
            <p>
                The Background Job Server has carbon aware jobs enabled but the Carbon Intensity API is currently unreachable. Please make sure the API is
                reachable from the server.
            </p>

            <ul>
                {problem.carbonIntensityApiErrorMetadata.map((err) => {
                    return <li key={err.owner} style={{marginTop: "4px"}}>{err.owner} encountered <strong>{err.value}</strong>
                        {" "}<TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(err.createdAt)}
                            title={new Date(err.createdAt).toString()}/></li>
                })}
            </ul>

            <p>
                As long as this problem persists, jobs will be scheduled at their preferred time instead of the optimal time.
            </p>
        </DismissibleClusterProblemNotification>
    );
};