import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

export const GanttRowDuration = ({children}) => (
    <Box>
        <Typography sx={{fontSize: '11px', textAlign: 'right', color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
            {children}
        </Typography>
    </Box>
);