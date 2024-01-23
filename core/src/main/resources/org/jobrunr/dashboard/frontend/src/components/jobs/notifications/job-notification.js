import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Alert from "@mui/material/Alert";

export const JobNotification = ({children}) => (
    (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" style={{fontSize: '1rem'}}>
                    {children}
                </Alert>
            </Paper>
        </Grid>
    )
)