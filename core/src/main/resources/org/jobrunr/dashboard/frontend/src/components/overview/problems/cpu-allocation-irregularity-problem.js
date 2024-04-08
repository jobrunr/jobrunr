import TimeAgo from "react-timeago/lib";
import {DismissibleInstanceProblemNotification} from "./dismissible-problem-notification";

const CPUAllocationIrregularityProblem = (props) => {
    return (
        <DismissibleInstanceProblemNotification
            endpoint="/api/problems/cpu-allocation-irregularity"
            refresh={props.refresh}
        >
            JobRunr detected some CPU Allocation Irregularities (e.g. due to a very long garbage collect cycle
            or due to CPU starvation on shared hosting). This may result in unstable behaviour of your JobRunr cluster.
            <em><b>&nbsp;You are urged to look into this as soon as possible.</b></em>
            <br/>
            <ul>
                {props.problem.cpuAllocationIrregularityMetadataSet.map((irregularity, index) => {
                    return <li key={index}>{irregularity.owner} had a CPU Allocation Irregularity
                        of {irregularity.value} seconds <TimeAgo
                            style={{textDecoration: 'underline', textDecorationStyle: 'dotted'}}
                            date={new Date(irregularity.createdAt)}
                            title={new Date(irregularity.createdAt).toString()}/></li>
                })}
            </ul>
        </DismissibleInstanceProblemNotification>
    );
};

export default CPUAllocationIrregularityProblem;