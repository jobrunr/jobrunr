import {
    DismissibleInstanceProblemNotification,
    DismissibleProblemNotification
} from "./dismissible-problem-notification";

const CarbonIntensityApiErrorProblem = (props) => {
    return (
        <DismissibleInstanceProblemNotification endpoint="/api/problems/carbon-intensity-api-error" refresh={props.refresh}
                                                severity="warning" title="Carbon Aware API Warning">
            The Background Job Server has carbon aware jobs enabled but the Carbon Intensity API is currently unreachable.
            <br/>
            As long as this problem persists, jobs will be scheduled at their preferred time instead of the optimal time.
            <br/>
        </DismissibleInstanceProblemNotification>
    );
};

export default CarbonIntensityApiErrorProblem;