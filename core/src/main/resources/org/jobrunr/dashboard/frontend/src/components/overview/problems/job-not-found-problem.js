import {ProblemNotification} from "./problem-notification";

const JobNotFoundProblem = () => {
    return (
        <ProblemNotification>
            There are SCHEDULED jobs that do not exist anymore in your code. These jobs will
            fail with a <strong>JobNotFoundException</strong> (due to a ClassNotFoundException or a
            MethodNotFoundException). <br/>
        </ProblemNotification>
    );
};

export default JobNotFoundProblem;