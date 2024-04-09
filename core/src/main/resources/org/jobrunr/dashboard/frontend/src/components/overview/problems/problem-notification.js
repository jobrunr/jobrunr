import {Alert, AlertTitle} from "@mui/material";

export const ProblemNotification = ({title = "Warning", children, severity = "warning", ...rest}) => (
    <Alert style={{marginBottom: '2rem'}} severity={severity} {...rest}>
        <AlertTitle><h4 style={{lineHeight: 1, margin: 0}}>{title}</h4></AlertTitle>
        {children}
    </Alert>
)