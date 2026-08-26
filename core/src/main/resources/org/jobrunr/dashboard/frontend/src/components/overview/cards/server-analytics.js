import Paper from '@mui/material/Paper';
import {useServers} from "../../../hooks/useServers.js";
import EstimatedProcessingTimeCard from "./estimated-processing-time-card.js";
import UptimeCard from "./uptime-card.js";
import NbrOfBackgroundJobServersCard from "./number-of-background-job-servers-card.js";
import AvgSystemCpuLoadCard from "./avg-system-cpu-load-card.js";
import AvgProcessMemoryUsageCard from "./avg-process-memory-usage-card.js";
import AvgProcessFreeMemoryCard from "./avg-process-free-memory-card.js";
import {ItemsNotFound} from "../../utils/items-not-found.js";
import Grid from "@mui/material/Grid";
import Typography from "@mui/material/Typography";

export const ServerAnalytics = () => {
    const [servers, _] = useServers();

    return (
        <div>
            <Typography id="server-subtitle" variant="h4" sx={{my: 2}}>Server Analytics</Typography>
            {servers.length > 0
                ? <>
                    <Grid container spacing={3}>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <EstimatedProcessingTimeCard/>
                        </Grid>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <UptimeCard servers={servers}/>
                        </Grid>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <NbrOfBackgroundJobServersCard servers={servers}/>
                        </Grid>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <AvgSystemCpuLoadCard servers={servers}/>
                        </Grid>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <AvgProcessMemoryUsageCard servers={servers}/>
                        </Grid>
                        <Grid size={{xs: 12, sm: 6, md: 4, lg: 3, xl: 2}}>
                            <AvgProcessFreeMemoryCard servers={servers}/>
                        </Grid>
                    </Grid>
                </>
                : <Paper style={{marginTop: '1rem', width: '100%'}}>
                    <ItemsNotFound id="no-servers-found-message">
                        No background job server available - jobs will not be processed.
                    </ItemsNotFound>
                </Paper>
            }
        </div>
    )
}