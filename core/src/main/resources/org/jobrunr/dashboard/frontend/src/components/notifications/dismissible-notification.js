import {MenuItem} from "@mui/material";
import ListItemIcon from "@mui/material/ListItemIcon";
import {Delete} from "mdi-material-ui";
import ListItemText from "@mui/material/ListItemText";
import {Notification} from "./notification";

export const getEndpointToDismissProblem = (problem) => {
    return `/api/problems/${problem.type}`;
}

export const DismissibleNotification = ({onDismiss, extraMenuItems, children, ...rest}) => {
    return (
        <Notification {...rest} extraMenuItems={
            <>
                {extraMenuItems}
                <MenuItem onClick={onDismiss}>
                    <ListItemIcon><Delete fontSize="small" color="error"/></ListItemIcon>
                    <ListItemText>Dismiss</ListItemText>
                </MenuItem>
            </>
        }>
            {children}
        </Notification>
    )
}

export const DismissibleClusterProblemNotification = ({endpoint, onDismiss, ...rest}) => {
    const handleDismiss = () => {
        fetch(endpoint, {
            method: 'DELETE'
        })
            .then(() => onDismiss())
            .catch(error => console.error(error));
    }

    return (
        <DismissibleNotification onDismiss={handleDismiss} {...rest}/>
    )
}