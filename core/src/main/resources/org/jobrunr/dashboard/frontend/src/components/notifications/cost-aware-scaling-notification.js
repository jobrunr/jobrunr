import {Notification} from "./notification";

export const getCostAwareScaling = (spotScaling) => {
    if (!spotScaling) return;
    return {...spotScaling, type: "cost-aware-spot-scaling"};
}

export const CostAwareScalingNotification = ({problem, ...rest}) => {

    const directionScalingText = () => {
        switch (problem.direction) {
            case "UP":
                return <>
                    JobRunr is scaling the number of spot instances up from {problem.currentServers} to {problem.currentServers + 1}.
                </>
            case "DOWN":
                return <>
                    JobRunr is scaling the number of spot instances down from {problem.currentServers} to {problem.currentServers - 1}.
                </>
        }
    }

    const processingStatusText = () => {
        switch (problem.scalingStatus) {
            case "PROCESSING":
                return <>
                    The cost aware API is currently processing your request.
                </>
            case "PROVISIONED":
                return <>
                    An instance has been provisioned, we are now waiting for it to come up.
                </>
            case "SCALED_DOWN":
                return <>
                    An instance has been scaled down, waiting for it to be inactive.
                </>
            case "FAILED":
                return <>
                    {problem.direction === "UP" ? "Creating" : "Scaling down"} a spot instance has failed.
                </>
        }
    }

    return (
        <Notification title="Cost Aware Scaling" date={problem.createdAt} read={problem.read} severity={"info"} {...rest}>
            {directionScalingText()}
            <br/>
            {processingStatusText()}
        </Notification>
    );
};