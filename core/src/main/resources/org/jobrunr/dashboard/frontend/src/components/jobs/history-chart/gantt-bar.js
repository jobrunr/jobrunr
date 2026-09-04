import {keyframes, styled} from "@mui/material/styles";
import LinearProgress, {linearProgressClasses} from "@mui/material/LinearProgress";
import {lighten} from "@mui/material";

const animateInProgressBar = keyframes`
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 28px 0;
    }
`;
export const getBarColor = (step, theme) => {
    if (step.state === 'ENQUEUED') return theme.palette.info.light;
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