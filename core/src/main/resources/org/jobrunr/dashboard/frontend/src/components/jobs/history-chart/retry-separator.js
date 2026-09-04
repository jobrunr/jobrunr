import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";

export const RetrySeparator = ({label}) => (
    <Box aria-hidden="true" sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pl: 0.5, zIndex: 1}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600, opacity: 0.6}}>{label}</Typography>
        <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}}/>
    </Box>
);