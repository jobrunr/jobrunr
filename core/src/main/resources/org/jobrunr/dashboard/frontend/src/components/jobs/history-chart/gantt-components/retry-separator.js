import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {useId} from "react";

export const VerticalRetrySeparator = ({label, position, isRequeue}) => (
    <Box sx={{
        position: 'absolute', top: 0, bottom: 0, left: `${position}%`,
        borderLeft: isRequeue ? '2px solid' : '1px dashed', borderColor: 'divider', opacity: 0.6,
    }}>
        <Typography variant="caption" sx={{
            position: 'absolute',
            top: -7,
            left: '50%',
            transform: 'translateX(-50%)',
            whiteSpace: 'nowrap',
            bgcolor: 'background.paper',
            px: 0.5,
        }}>
            {label}
        </Typography>
    </Box>
)

export const HorizontalRetrySeparator = ({label, isRequeue}) => {
    const id = useId();

    return (
        <Box sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pl: 0.5, zIndex: 1}}>
            <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, opacity: 0.6}} id={id}>
                {label}
            </Typography>
            <Box sx={{flexGrow: 1, borderTop: isRequeue ? '2px solid' : '1px dashed', borderColor: 'divider'}} aria-labelledby={id}/>
        </Box>
    )
}