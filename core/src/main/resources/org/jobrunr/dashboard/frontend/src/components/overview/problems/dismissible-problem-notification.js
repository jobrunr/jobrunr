import {ProblemNotification} from "./problem-notification";
import {Button} from "@mui/material";


export const DismissibleProblemNotification = ({onDismiss, children, ...rest}) => {
    return (
        <ProblemNotification {...rest} action={
            <Button color="inherit" size="small" style={{textTransform: "uppercase"}} onClick={onDismiss}>
                dismiss
            </Button>
        }>
            {children}
        </ProblemNotification>
    )
}

export const DismissibleInstanceProblemNotification = ({endpoint, refresh, ...rest}) => {
    const dismiss = () => {
        fetch(endpoint, {
            method: 'DELETE'
        })
            .then(resp => refresh())
            .catch(error => console.log(error));
    }

    return (
        <DismissibleProblemNotification onDismiss={dismiss} {...rest}/>
    )
}