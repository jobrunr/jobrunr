import {darken, lighten, useColorScheme} from "@mui/material";
import Box from "@mui/material/Box";

export const BreakIndicator = ({leftPct = 50, color}) => {
    const {mode, systemMode} = useColorScheme();

    return (
        <Box sx={{
            position: 'absolute', left: `${leftPct}%`, top: '55.5%', transform: 'translate(-50%, -50%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3,
            px: 1, fontSize: '1.5rem', fontWeight: 600,
            color: mode === "light" || systemMode === "light" ? darken(color, 0.3) : lighten(color, 0.6),
        }}>
            //
        </Box>
    )
};