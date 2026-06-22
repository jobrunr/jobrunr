import Alert from "@mui/material/Alert";
import Paper from "@mui/material/Paper";
import Grid from "@mui/material/Grid";
import Typography from "@mui/material/Typography";
import {useMemo} from "react";

export const ServerScalingNotification = ({spotScaling}) => {

    const directionScalingText = () => {
        switch (spotScaling.direction) {
            case "UP":
                return <Typography>
                    JobRunr is scaling the number of spot instances up from {spotScaling.currentServers} to {spotScaling.currentServers + 1}.
                </Typography>
            case "DOWN":
                return <Typography>
                    JobRunr is scaling the number of spot instances down from {spotScaling.currentServers} to {spotScaling.currentServers - 1}.
                </Typography>
        }
    }

    const processingStatusText = () => {
        switch (spotScaling.scalingStatus) {
            case "PROCESSING":
                return <Typography>
                    The cost aware API is currently processing your request.
                </Typography>
            case "PROVISIONED":
                return <Typography>
                    An instance has been provisioned, we are now waiting for it to come up.
                </Typography>
            case "SCALED_DOWN":
                return <Typography>
                    An instance has been scaled down, we are now waiting for it to be inactive.
                </Typography>
            case "FAILED":
                return <Typography>
                    {spotScaling.direction === "UP" ? "Creating" : "Scaling down"} a spot instance has failed.
                </Typography>
        }
    }

    const severity = useMemo(() => {
        switch (spotScaling.scalingStatus) {
            case "PROCESSING":
                return "info";
            case "PROVISIONED":
                return "success";
            case "SCALED_DOWN":
                return "success";
            case "FAILED":
                return "error";
        }
    }, [spotScaling])

    return (
        <Paper sx={{marginBottom: '1rem'}}>
            <Grid size={12}>
                <Paper>
                    <Alert
                        severity={severity}
                        sx={{fontSize: '1rem'}}
                    >
                        {directionScalingText()}
                        {processingStatusText()}
                    </Alert>
                </Paper>
            </Grid>
        </Paper>
    )
}