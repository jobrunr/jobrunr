import {ProblemNotification} from "./problem-notification";
import {Button} from "@mui/material";


export const DismissibleProblemNotification = ({endpoint, refresh, children, ...rest}) => {
    const dismiss = () => {
        fetch(endpoint, {
            method: 'DELETE'
        })
            .then(resp => refresh())
            .catch(error => console.log(error));
    }

    return (
        <ProblemNotification {...rest} action={
            <Button color="inherit" size="small" style={{textTransform: "uppercase"}} onClick={dismiss}>
                dismiss
            </Button>
        }>
            {children}
        </ProblemNotification>
    )
}