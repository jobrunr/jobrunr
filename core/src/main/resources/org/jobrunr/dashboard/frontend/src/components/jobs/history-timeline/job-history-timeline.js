import Awaiting from "../states/awaiting-state.js";
import Scheduled from "../states/scheduled-state.js";
import Enqueued from "../states/enqueued-state.js";
import Processing from "../states/processing-state.js";
import Failed from "../states/failed-state.js";
import Succeeded from "../states/succeeded-state.js";
import Deleted from "../states/deleted-state.js";
import {AWAITING, DELETED, ENQUEUED, FAILED, PROCESSING, SCHEDULED, SUCCEEDED} from "../../utils/state-names.js";

export const JobHistoryTimeline = ({job, order}) => {
    const orderedJobStates = job ? (order ? [...job.jobHistory] : [...job.jobHistory].reverse()) : [];
    return (
        <>
            {orderedJobStates.map((jobState, index) => {
                switch (jobState.state) {
                    case AWAITING:
                        return <Awaiting key={index} job={job} jobState={jobState}/>;
                    case SCHEDULED:
                        return <Scheduled key={index} jobState={jobState}/>;
                    case ENQUEUED:
                        return <Enqueued key={index} jobState={jobState}/>;
                    case PROCESSING:
                        return <Processing key={index} index={index} job={job} jobState={jobState}/>;
                    case FAILED:
                        return <Failed key={index} jobState={jobState}/>;
                    case SUCCEEDED:
                        return <Succeeded key={index} jobState={jobState}/>;
                    case DELETED:
                        return <Deleted key={index} jobState={jobState}/>;
                    default:
                        return <div key={index}>Unknown state</div>
                }
            })}
        </>
    )
}