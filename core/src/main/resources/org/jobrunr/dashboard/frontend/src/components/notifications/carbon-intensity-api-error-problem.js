import {DismissibleClusterProblemNotification} from "./dismissible-notification";

export const CarbonIntensityApiErrorProblem = ({problem, ...rest}) => {
    return (
        <DismissibleClusterProblemNotification
            endpoint="/api/problems/carbon-intensity-api-error"
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
            <p>
                As long as this problem persists, jobs will be scheduled at their preferred time instead of the optimal time.
            </p>
        </DismissibleClusterProblemNotification>
    );
};