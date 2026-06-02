import {JobNotification} from "./job-notification";
import {PiggyBank} from "mdi-material-ui";
import {createTheme, ThemeProvider} from "@mui/material";

const notificationTheme = createTheme({
    palette: {
        costAware: {
            main: '#3C6371',
            light: '#3C6371',
            dark: '#3C6371',
            contrastText: '#FFFFFF',
        },
    },
});

const CostAwareNotification = ({job}) => {
    const succeededState = job.jobHistory[job.jobHistory.length - 1]

    if (!succeededState.instancePrice || !succeededState.spotPrice) return (<></>);

    const hourlySavings = (succeededState.instancePrice - succeededState.spotPrice).toFixed(4);
    const savingsPercentage = (((succeededState.instancePrice - succeededState.spotPrice) / succeededState.instancePrice) * 100).toFixed(2);

    return (
        <ThemeProvider theme={notificationTheme}>
            <JobNotification
                variant="filled"
                color="costAware"
                icon={<PiggyBank fontSize="inherit" sx={{color: "#FFFFFF"}}/>}
            >
                This job was executed on a spot instance, resulting in <strong>{savingsPercentage}% savings </strong>
                and <strong>reducing costs by ${hourlySavings}</strong> per hour.
            </JobNotification>
        </ThemeProvider>
    )
};

export default CostAwareNotification;