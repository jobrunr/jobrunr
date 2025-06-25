import {Notification} from "./notification";

export const JobNotFoundProblemNotification = ({problem, ...rest}) => {
    return (
        <Notification title="Job Not Found Exception" date={problem.createdAt} read={problem.read} {...rest}>
            There are SCHEDULED jobs that do not exist anymore in your code. These jobs will
            fail with a <strong>JobNotFoundException</strong> (due to a ClassNotFoundException or a
            MethodNotFoundException). <br/>
        </Notification>
    );
};