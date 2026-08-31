import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {lighten, useTheme} from "@mui/material";
import {RadioButtonChecked} from "@mui/icons-material";

const asMs = (date) => new Date(date).getTime();

const getNextJobHistoryStep = (steps, index) =>
    steps.find((step, i) => i > index && step.state !== 'RUN_STEP_ONCE') ?? null;

const END_STATES = ['SUCCEEDED', 'FAILED', 'DELETED'];

const validEndAt = (step) =>
    step.updatedAt && asMs(step.updatedAt) > asMs(step.createdAt) ? asMs(step.updatedAt) : null;

const getStepEndAt = (step, nextJobHistoryStep, jobInProgress, now) => {
    if (step.state === 'RUN_STEP_ONCE') {
        const end = validEndAt(step);
        if (end !== null) return {end, active: false};
        if (jobInProgress) return {end: now, active: true};
        return {end: null, active: false};
    }
    if (step.state === 'SUCCEEDED' || step.state === 'FAILED') {
        return {end: asMs(step.createdAt), active: false};
    }
    if (nextJobHistoryStep) return {end: asMs(nextJobHistoryStep.createdAt), active: false};
    if (jobInProgress) return {end: now, active: true};
    return {end: validEndAt(step), active: false};
};

const getTimeline = (steps) => {
    const jobInProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
    const now = Date.now();
    let start = Infinity, end = -Infinity;
    const stepEnds = [];
    const stepActive = [];
    steps.forEach((step, i) => {
        const stepStart = asMs(step.createdAt);
        if (stepStart < start) start = stepStart;
        const {end: stepEnd, active} = getStepEndAt(step, getNextJobHistoryStep(steps, i), jobInProgress, now);
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEnds.push(stepEnd);
        stepActive.push(active);
    });
    if (end < start) end = start;
    return {start, end, stepEnds, stepActive};
};

const ignoreEnqueuedAndScheduled = (executionSteps) =>
    executionSteps.filter(step => step.state !== 'ENQUEUED' && step.state !== 'SCHEDULED' && step.state !== 'AWAITING');

const MIN_ACTIVE_WIDTH_PERCENTAGE = 1;

const getStepPlacement = (step, stepEndMs, start, end, active) => {
    const stepStart = asMs(step.createdAt);
    const span = end - start;
    const percentage = (ms) => span > 0 ? (ms / span) * 100 : 0;
    const offset = percentage(stepStart - start);
    if (active) {
        let width = Math.max(percentage(stepEndMs - stepStart), 0);
        if (width < MIN_ACTIVE_WIDTH_PERCENTAGE) width = MIN_ACTIVE_WIDTH_PERCENTAGE;
        return {offset, width, isPoint: false};
    }
    if (stepEndMs === null || stepEndMs <= stepStart) return {offset, width: null, isPoint: true};
    return {offset, width: Math.max(percentage(stepEndMs - stepStart), 0), isPoint: false};
};

const STEP_LABELS = {
    PROCESSING: 'Processing',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    DELETED: 'Deleted',
    RUN_STEP_ONCE: 'Step that ran once',
};

const stepLabel = (step) => {
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName;
    return STEP_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const formatHumanReadableDate = (ms) => {
    const date = new Date(ms);
    return date.getDate() + "/" + (date.getMonth() + 1) + "/" + date.getFullYear() + " at " + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + "." + date.getMilliseconds();
}

const buildTooltipTitle = (step, stepEndMs, active) => {
    const startMs = asMs(step.createdAt);
    if (active) {
        return `${formatHumanReadableDate(startMs)} → ${formatHumanReadableDate(stepEndMs)} (in progress)`;
    }
    if (stepEndMs === null) {
        return formatHumanReadableDate(startMs);
    }
    return `${formatHumanReadableDate(startMs)} → ${formatHumanReadableDate(stepEndMs)} (${formatDuration(startMs, stepEndMs)})`;
};

const formatAxisTime = (ms) => {
    const d = new Date(ms);
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    if (ms < 1000) return `${ms}ms`;
    const seconds = ms / 1000;
    if (seconds < 60) return `${seconds.toFixed(2)}s`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ${Math.round(seconds - minutes * 60)}s`;
};

const moveStripes = keyframes`
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 28px 0;
    }
`;

export const GanttBar = styled(LinearProgress, {
    shouldForwardProp: (prop) => prop !== 'active',
})(({theme, active}) => {
    const infoLight = theme.palette.info.light;
    const infoLighter = lighten(infoLight, 0.4);

    return {
        height: '50%',
        width: '100%',
        alignSelf: 'center',
        borderRadius: 4,
        [`&.${linearProgressClasses.colorPrimary}`]: {
            backgroundColor: active ? '#f0f4f8' : 'transparent',
        },
        [`& .${linearProgressClasses.bar}`]: {
            borderRadius: 4,
            backgroundColor: infoLight,

            ...(active && {
                width: '100%',
                transform: 'none !important',
                animation: `${moveStripes} 1s linear infinite !important`,
                backgroundImage: `repeating-linear-gradient(
          45deg,
          ${infoLight},
          ${infoLight} 10px,
          ${infoLighter} 10px,
          ${infoLighter} 20px
        )`,
                backgroundSize: '28px 28px',
            }),
        },
        ...(active && {
            [`& .${linearProgressClasses.bar2Indeterminate}`]: {
                display: 'none',
            },
        }),
    };
});

const AxisLabel = ({sx, ...props}) => (
    <Typography variant="caption" sx={{position: 'absolute', top: 0, whiteSpace: 'nowrap', ...sx}} {...props}/>
);

const LABEL_WIDTH = 150;
const ROW_HEIGHT = 28;

export const JobProgressDisplay = ({executionSteps}) => {
    const theme = useTheme();
    const steps = ignoreEnqueuedAndScheduled(executionSteps ?? []);
    if (steps.length === 0) return null;

    const {start, end, stepEnds, stepActive} = getTimeline(steps);
    const mid = (start + end) / 2;

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{
                    '& .MuiBox-root[role=gantt-row]:not(:last-child)': {
                        borderBottom: "1px dashed lightgray"
                    },
                    '& .MuiBox-root[role=gantt-row-label]': {
                        borderRight: "1px dashed lightgray"
                    }
                }}>
                    <Typography variant="h6" gutterBottom>Execution Timeline</Typography>

                    <Box sx={{display: 'flex', alignItems: 'flex-end', mb: 1}}>
                        <Box sx={{minWidth: LABEL_WIDTH, flexShrink: 0}} role="gantt-row-label"/>
                        <Box sx={{
                            flex: 1, position: 'relative', height: 18,
                        }}>
                            <AxisLabel sx={{left: 0}}>{formatAxisTime(start)}</AxisLabel>
                            <AxisLabel sx={{left: '50%', transform: 'translateX(-50%)'}}>{formatAxisTime(mid)}</AxisLabel>
                            <AxisLabel sx={{right: 0}}>{formatAxisTime(end)}</AxisLabel>
                        </Box>
                    </Box>

                    {steps.map((step, index) => {
                        const stepEndMs = stepEnds[index];
                        const active = stepActive[index];
                        const {offset, width, isPoint} = getStepPlacement(step, stepEndMs, start, end, active);
                        const tooltipTitle = buildTooltipTitle(step, stepEndMs, active);
                        return (
                            <Box key={index} role="gantt-row"
                                 sx={{display: 'flex', alignItems: 'center', height: ROW_HEIGHT, mb: 0.5}}>
                                <Box sx={{width: LABEL_WIDTH, flexShrink: 0, pr: 1, overflow: 'hidden'}} role="gantt-row-label">
                                    <Typography variant="body2" noWrap>{stepLabel(step)}</Typography>
                                </Box>
                                <Box sx={{flex: 1, position: 'relative', height: 18}}>
                                    <Tooltip title={tooltipTitle} enterDelay={0}>
                                        {isPoint ? (
                                            <RadioButtonChecked sx={{
                                                position: 'absolute',
                                                left: `${offset}%`,
                                                top: '50%',
                                                transform: 'translate(-50%, -50%)',
                                                borderRadius: '50%',
                                            }} color="info" fontSize="small"
                                            />
                                        ) : (
                                            <Box sx={{
                                                position: 'absolute',
                                                left: `${offset}%`,
                                                width: `${width}%`,
                                                top: 0, bottom: 0,
                                                display: 'flex',
                                                alignItems: 'center',
                                            }}>
                                                <GanttBar
                                                    active={active}
                                                    variant={active ? 'indeterminate' : 'determinate'}
                                                    value={active ? undefined : 100}
                                                />
                                            </Box>
                                        )}
                                    </Tooltip>
                                </Box>
                            </Box>
                        );
                    })}
                </CardContent>
            </Card>
        </Box>
    );
}
