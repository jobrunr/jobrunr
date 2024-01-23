import {ProblemNotification} from "./problem-notification";


const NewJobRunrVersionAvailable = (props) => {
    return (
        <ProblemNotification severity="info" title="Info">
            JobRunr version {props.problem.latestVersion} is available. Please upgrade JobRunr as it brings bugfixes,
            performance improvements and new features.<br/>
        </ProblemNotification>
    );
};

export default NewJobRunrVersionAvailable;