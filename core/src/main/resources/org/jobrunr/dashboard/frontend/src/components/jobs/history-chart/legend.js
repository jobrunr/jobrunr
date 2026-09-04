import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {RhombusOutline} from "mdi-material-ui";

export const Legend = () => (
    <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 2, mt: 2, pt: 1, borderTop: '1px solid', borderColor: 'divider'}}>
        {[
            {label: 'Scheduled', color: 'grey.600'}, {label: 'Enqueued', color: 'info.light'},
            {label: 'Processing', color: 'warning.light'}, {label: 'Succeeded', color: 'success.light'},
            {label: 'Failed', color: 'error.light'},
        ].map((item) => (
            <Box key={item.label} sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
                <Box sx={{width: 10, height: 10, borderRadius: '2px', bgcolor: item.color}}/>
                <Typography variant="caption" color="text.secondary">{item.label}</Typography>
            </Box>
        ))}
        <Box sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
            <Typography sx={{fontWeight: 600}}>//</Typography>
            <Typography variant="caption" color="text.secondary">Compressed</Typography>
        </Box>
        <Box sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
            <RhombusOutline fontSize="tiny" sx={{color: 'grey.500'}}/>
            <Typography variant="caption" color="text.secondary">Skipped</Typography>
        </Box>
    </Box>
);