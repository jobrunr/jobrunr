import {ProblemNotification} from "./problem-notification";
import {Button} from "@mui/material";


export const DismissibleProblemNotification = ({onDismiss, extraAction, children, ...rest}) => {
    return (
        <ProblemNotification {...rest} action={
            <>
                {extraAction}
                <Button variant="contained" color="secondary" size="small" style={{textTransform: "uppercase", marginLeft: "1rem"}} onClick={onDismiss}>
                    Dismiss
                </Button>
            </>
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