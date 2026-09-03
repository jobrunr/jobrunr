import {darken, lighten, useColorScheme} from "@mui/material";
import {keyframes, styled} from "@mui/material/styles";
import LinearProgress, {linearProgressClasses} from "@mui/material/LinearProgress";
import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {RhombusOutline} from "mdi-material-ui";

const animateInProgressBar = keyframes`
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 28px 0;
    }
`;

export const getBarColor = (step, theme) => {
    if (['ENQUEUED', 'RETRYING'].includes(step.state)) return theme.palette.info.light;
    if (step.state === 'SCHEDULED') return theme.palette.grey[600];
    if (step.succeeded === false || step.state === 'FAILED') return theme.palette.error.light;
    if (step.succeeded === true || step.state === 'SUCCEEDED') return theme.palette.success.light;
    return theme.palette.warning.light;
};

export const GanttBar = styled(LinearProgress, {
    shouldForwardProp: (prop) => prop !== 'active',
})(({theme, active, step}) => {
    const color = getBarColor(step, theme), lighter = lighten(color, 0.4);
    return {
        height: '50%', width: '100%', alignSelf: 'center', borderRadius: 4,
        [`&.${linearProgressClasses.colorPrimary}`]: {backgroundColor: active ? '#f0f4f8' : 'transparent'},
        [`& .${linearProgressClasses.bar}`]: {
            borderRadius: 4, backgroundColor: color,
            ...(active && {
                width: '100%', transform: 'none !important',
                animation: `${animateInProgressBar} 1s linear infinite !important`,
                backgroundImage: `repeating-linear-gradient(45deg, ${color}, ${color} 10px, ${lighter} 10px, ${lighter} 20px)`,
                backgroundSize: '28px 28px',
            }),
        },
        ...(active && {[`& .${linearProgressClasses.bar2Indeterminate}`]: {display: 'none'}}),
    };
});

export const BreakIndicator = ({leftPct = 50, color}) => {
    const {mode, systemMode} = useColorScheme();

    return (
        <Box sx={{
            position: 'absolute', left: `${leftPct}%`, top: '51.5%', transform: 'translate(-50%, -50%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3,
            px: 1, fontSize: '1.5rem', fontWeight: 600,
            color: mode === "light" || systemMode === "light" ? darken(color, 0.3) : lighten(color, 0.6),
        }}>
            //
        </Box>
    )
};

export const RetrySeparator = ({label}) => (
    <Box aria-hidden="true" sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pl: 0.5, zIndex: 1}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600, opacity: 0.6}}>{label}</Typography>
        <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}}/>
    </Box>
);

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