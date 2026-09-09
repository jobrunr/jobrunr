import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {MAX_LABEL_WIDTH, MIN_LABEL_WIDTH, ROW_HEIGHT} from "./timeline-gantt-chart.js";

export const GanttRowLabel = (label, isStep) => (
    <Box sx={{
        maxWidth: MAX_LABEL_WIDTH,
        minWidth: MIN_LABEL_WIDTH,
        height: ROW_HEIGHT,
        display: 'flex',
        alignItems: 'center',
        pr: 1,
        overflow: 'hidden',
        mr: 0.5
    }}>
        <Typography variant={isStep ? 'caption' : 'body2'} noWrap
                    sx={{pl: isStep ? 2 : 0, color: isStep ? 'text.secondary' : 'text.primary', display: 'flex', alignItems: 'center'}}>
            {isStep && <Box component="span" sx={{opacity: 0.6, mr: 0.5}}>└</Box>}
            {label}
        </Typography>
    </Box>
);