import {JobNotification} from "./job-notification";
import {EnergySavingsLeaf} from "@mui/icons-material";
import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Alert from "@mui/material/Alert";
import {SwitchableTimeAgo} from "../../utils/time-ago";

const CarbonAwareScheduledNotification = ({job}) => {
    const awaitingState = job.jobHistory[0];
    if(!awaitingState["@class"].endsWith("CarbonAwareAwaitingState")) {
        return;
    }

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" icon={
                    <EnergySavingsLeaf fontSize="small" color="success" style={{marginRight: "4px"}}/>
                } style={{fontSize: '1rem'}}>
                    This job is scheduled Carbon Aware between <SwitchableTimeAgo date={new Date(awaitingState.from)}/> and <SwitchableTimeAgo date={new Date(awaitingState.to)}/> to minimize carbon impact.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default CarbonAwareScheduledNotification;