import {ProblemNotification} from "./problem-notification";

export const getApiNotificationProblem = (apiNotification) => {
    if (!apiNotification) return;
    return {...apiNotification, type: "api-notification"};
}

const JobRunrApiNotification = ({problem}) => {
    return <ProblemNotification severity="info" title={problem.title}>
        <span dangerouslySetInnerHTML={{__html: problem.body}}/>
    </ProblemNotification>
};

export default JobRunrApiNotification;