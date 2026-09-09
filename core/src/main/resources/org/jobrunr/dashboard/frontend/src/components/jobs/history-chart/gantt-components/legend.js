import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {RhombusOutline} from "mdi-material-ui";
import {ENQUEUED, FAILED, PROCESSING, SCHEDULED, STATE_LABELS, SUCCEEDED} from "../../../utils/state-names.js";

export const Legend = () => {
    const states = [
        {label: STATE_LABELS[SCHEDULED], color: 'grey.600'},
        {label: STATE_LABELS[ENQUEUED], color: 'info.light'},
        {label: STATE_LABELS[PROCESSING], color: 'warning.light'},
        {label: STATE_LABELS[SUCCEEDED], color: 'success.light'},
        {label: STATE_LABELS[FAILED], color: 'error.light'},
    ];
    return (
        <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 2, mt: 2, pt: 1, borderTop: '1px solid', borderColor: 'divider'}}>
            {states.map((item) => (
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
}