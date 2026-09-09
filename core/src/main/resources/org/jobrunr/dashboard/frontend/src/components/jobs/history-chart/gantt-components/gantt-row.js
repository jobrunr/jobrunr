import {GanttRowLabel} from "./gantt-row-label.js";
import {ROW_HEIGHT} from "../timeline-gantt-chart.js";
import Box from '@mui/material/Box';

export const GanttRow = ({label, isStep, children, ...rest}) => {
    return (
        <Box {...rest}
             sx={{
                 display: 'grid',
                 gridTemplateColumns: 'subgrid',
                 gridColumn: '1 / -1',
                 alignItems: 'center',
                 minHeight: ROW_HEIGHT,
                 px: 0.5,
                 zIndex: 1,
                 '&:hover': {
                     backgroundColor: 'rgba(0, 0, 0, 0.03)',
                 },
             }}>
            {GanttRowLabel(label, isStep)}
            {children}
        </Box>
    );
}