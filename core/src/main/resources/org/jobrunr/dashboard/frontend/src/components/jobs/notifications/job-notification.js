import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Alert from "@mui/material/Alert";

export const JobNotification = ({children, severity = "info", ...rest}) => (
    (
        <Grid item xs={12}>
            <Paper>
                <Alert
                    severity={severity}
                    style={{fontSize: '1rem'}}
                    {...rest}
                >
                    {children}
                </Alert>
            </Paper>
        </Grid>
    )
)