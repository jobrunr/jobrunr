import TimeAgo from "react-timeago";
import {DismissibleClusterProblemNotification, getEndpointToDismissProblem} from "./dismissible-notification";

export const CPUAllocationIrregularityProblemNotification = ({problem, ...rest}) => {
    return (
        <DismissibleClusterProblemNotification
            title="CPU Allocation Irregularity"
            endpoint={getEndpointToDismissProblem(problem)}
            date={problem.createdAt}
            read={problem.read}
            {...rest}
        >
            JobRunr detected some CPU Allocation Irregularities (e.g. due to a very long garbage collect cycle
            or due to CPU starvation on shared hosting). This may result in unstable behaviour of your JobRunr cluster.
            <em><b>&nbsp;You are urged to look into this as soon as possible.</b></em>
            <br/>
            <ul>
                {problem.cpuAllocationIrregularityMetadataSet.map((irregularity) => {
                    return <li key={irregularity.owner} style={{marginTop: "4px"}}>{irregularity.owner} was <strong>not able to execute critical tasks
                        for {irregularity.value} seconds</strong>
                        {" "}<TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(irregularity.createdAt)}
                            title={new Date(irregularity.createdAt).toString()}/></li>
                })}
            </ul>
        </DismissibleClusterProblemNotification>
    );
};
