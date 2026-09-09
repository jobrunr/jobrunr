import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {useId} from "react";

export const RetrySeparator = ({label, vertical, position}) => {
    const id = useId();
    return (
        <>
             {vertical ?
                 <Box sx={{
                     position: 'absolute', top: 0, bottom: 0, left: `${position}%`,
                     borderLeft: '1px dashed', borderColor: 'divider', opacity: 0.6,
                 }}>
                     <Typography variant="caption" sx={{
                         position: 'absolute', top: -7, left: '50%', transform: 'translateX(-50%)',
                         fontWeight: 600, fontSize: '10px', whiteSpace: 'nowrap', bgcolor: 'background.paper', px: 0.5,
                     }}>
                         {label}
                     </Typography>
                 </Box>
                 :
                 <Box sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pl: 0.5, zIndex: 1}}>
                     <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600, opacity: 0.6}} id={id}>{label}</Typography>
                     <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}} aria-labelledby={id}/>
                 </Box>
             }
        </>
    );
}