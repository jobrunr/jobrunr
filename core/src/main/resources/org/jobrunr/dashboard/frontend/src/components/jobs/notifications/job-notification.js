import Grid from "@mui/material/Grid";
import Paper from "@mui/material/Paper";
import Alert from "@mui/material/Alert";

export const JobNotification = ({children, severity = "info", ...rest}) => (
    (
        <Grid size={12}>
            <Paper>
                <Alert
                    severity={severity}
                    sx={{fontSize: '1rem'}}
                    {...rest}
                >
                    {children}
                </Alert>
            </Paper>
        </Grid>
    )
)