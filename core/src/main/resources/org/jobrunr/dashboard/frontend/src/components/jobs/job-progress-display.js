import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {lighten} from "@mui/material";
import {Circle} from "@mui/icons-material";
import {useEffect, useState} from 'react';
import {humanReadableISO8601Duration} from "../../utils/helper-functions.js";

const STEP_DEFAULT_LABELS = {
    ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    RUN_STEP_ONCE: 'Step that ran once',
};
const END_STATES = ['SUCCEEDED', 'FAILED'];
const EXCLUDED_STATES = ['AWAITING', 'SCHEDULED', 'DELETED'];
const MIN_ACTIVE_WIDTH_PERCENTAGE = 1;
const MIN_LABEL_WIDTH = 150;
const MAX_LABEL_WIDTH = 250;
const ROW_HEIGHT = 28;
const GANTT_GRID_COLUMNS = 'minmax(0, max-content) 1fr';

const asMs = (date) => new Date(date).getTime();

const asMicros = (date) => {
    const matcher = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$/.exec(String(date));
    if (!matcher) return new Date(date).getTime() * 1000;
    const wholeSecondMs = Date.parse(matcher[1] + (matcher[3] || 'Z'));
    const frac = matcher[2] ? (matcher[2] + '000000').slice(0, 6) : '0';
    return wholeSecondMs * 1000 + parseInt(frac, 10);
};

const getStepEndTime = (step) =>
    step.updatedAt && asMicros(step.updatedAt) > asMicros(step.createdAt) ? asMs(step.updatedAt) : null;

const getNextJobHistoryStep = (steps, index) =>
    steps.find((step, i) => i > index && step.state !== 'RUN_STEP_ONCE') ?? null;

const getStepEndAt = (step, nextJobHistoryStep, jobInProgress, now) => {
    if (END_STATES.includes(step.state)) {
        return {end: asMs(step.createdAt), active: false};
    }
    if (step.state === 'RUN_STEP_ONCE') {
        const end = getStepEndTime(step);
        return end === null && jobInProgress ? {end: now, active: true} : {end, active: false};
    }
    if (nextJobHistoryStep) return {end: asMs(nextJobHistoryStep.createdAt), active: false};
    if (jobInProgress) return {end: now, active: true};
    return {end: getStepEndTime(step), active: false};
};

const convertStepsToTimeline = (steps, now) => {
    const jobInProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
    let start = Infinity, end = -Infinity;
    const stepEndTimes = [];
    const stepActive = [];
    steps.forEach((step, i) => {
        const stepStart = asMs(step.createdAt);
        if (stepStart < start) start = stepStart;
        const {end: stepEnd, active} = getStepEndAt(step, getNextJobHistoryStep(steps, i), jobInProgress, now);
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndTimes.push(stepEnd);
        stepActive.push(active);
    });
    if (end < start) end = start;
    return {start, end, stepEnds: stepEndTimes, stepActive};
};

const filterOutNonProcessingStates = (executionSteps) =>
    executionSteps.filter(step => !EXCLUDED_STATES.includes(step.state));

const getStepPlacement = (step, stepEndMs, start, end, active) => {
    const stepStart = asMs(step.createdAt);
    const duration = end - start;
    const percentage = (ms) => duration > 0 ? (ms / duration) * 100 : 0;
    const offset = percentage(stepStart - start);
    if (active) {
        let width = Math.max(percentage(stepEndMs - stepStart), 0);
        if (width < MIN_ACTIVE_WIDTH_PERCENTAGE) width = MIN_ACTIVE_WIDTH_PERCENTAGE;
        return {offset, width, isPoint: false};
    }
    if (stepEndMs === null || stepEndMs <= stepStart) return {offset, width: null, isPoint: true};
    return {offset, width: Math.max(percentage(stepEndMs - stepStart), 0), isPoint: false};
};

const getStepLabel = (step) => {
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName;
    return STEP_DEFAULT_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const getRowSeparatorBorder = (step, previousStep, index) => {
    if (index === 0) return undefined;
    return step.state === 'ENQUEUED' && previousStep.state === 'SUCCEEDED' ? '2px solid black' : '1px dashed lightgray';
};

const formatHumanReadableDate = (ms, detailed = true) => {
    const date = new Date(ms);
    const pad = (n) => String(n).padStart(2, '0');
    return pad(date.getDate()) + "/" + pad(date.getMonth() + 1) + "/" + pad(date.getFullYear()) + " "
        + pad(date.getHours()) + ":" + pad(date.getMinutes()) + ":" + pad(date.getSeconds()) + (detailed ? "." + date.getMilliseconds() : "");
}

const buildTooltipTitle = (step, stepEndMs, active) => {
    const startMs = asMs(step.createdAt);
    if (active) {
        return `${formatHumanReadableDate(startMs)} to ${formatHumanReadableDate(stepEndMs)} (in progress)`;
    } else if (stepEndMs === null || stepEndMs === startMs) {
        return `${formatHumanReadableDate(startMs)} ${END_STATES.includes(step.state) ? "" : "(<10 ms)"}`;
    } else {
        return `${formatHumanReadableDate(startMs)} to ${formatHumanReadableDate(stepEndMs)} (${formatDuration(startMs, stepEndMs)})`;
    }
};

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    const durationString = Number.isFinite(ms) && ms > 0 ? `PT${ms / 1000}S` : 'PT0S';
    return humanReadableISO8601Duration(durationString);
};

const animateInProgressBar = keyframes`
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 28px 0;
    }
`;

const GanttBar = styled(LinearProgress, {
    shouldForwardProp: (prop) => prop !== 'active',
})(({theme, active, step}) => {
    const infoLight = theme.palette.info.light;
    const infoLighter = lighten(infoLight, 0.4);
    const warningLight = theme.palette.warning.light;
    const warningLighter = lighten(warningLight, 0.4);
    const errorLight = theme.palette.error.light;

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
            backgroundColor: step.state === 'ENQUEUED' ? warningLight : step.succeeded === false ? errorLight : infoLight,
            ...(active && {
                width: '100%',
                transform: 'none !important',
                animation: `${animateInProgressBar} 1s linear infinite !important`,
                backgroundImage: step.state === 'ENQUEUED'
                    ? `repeating-linear-gradient(45deg, ${warningLight}, ${warningLight} 10px, ${warningLighter} 10px, ${warningLighter} 20px)`
                    : `repeating-linear-gradient(45deg, ${infoLight}, ${infoLight} 10px, ${infoLighter} 10px, ${infoLighter} 20px)`,
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

export const JobProgressDisplay = ({executionSteps}) => {
    const steps = filterOutNonProcessingStates(executionSteps ?? []);
    const jobInProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (!jobInProgress) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [jobInProgress]);

    if (steps.length === 0) return null;

    const {start, end, stepEnds, stepActive} = convertStepsToTimeline(steps, now);
    const midpoint = (start + end) / 2;

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{
                    '& .MuiBox-root[role=gantt-row-label]': {
                        borderRight: "1px dashed lightgray"
                    }
                }}>
                    <Typography variant="h5" gutterBottom>Execution Timeline</Typography>

                    <Box sx={{display: 'grid', gridTemplateColumns: GANTT_GRID_COLUMNS}}>
                        <Box sx={{
                            display: 'grid',
                            gridTemplateColumns: 'subgrid',
                            gridColumn: '1 / -1',
                            alignItems: 'flex-end',
                            height: 18,
                            mb: 1,
                        }}>
                            <Box role="gantt-row-label" sx={{maxWidth: MAX_LABEL_WIDTH, minWidth: 0, pr: 1}}/>
                            <Box sx={{position: 'relative', height: 18}}>
                                <AxisLabel sx={{left: 0}}>{formatHumanReadableDate(start, false)}</AxisLabel>
                                <AxisLabel sx={{left: '50%', transform: 'translateX(-50%)'}}>{formatHumanReadableDate(midpoint, false)}</AxisLabel>
                                <AxisLabel sx={{right: 0}}>{formatHumanReadableDate(end, false)}</AxisLabel>
                            </Box>
                        </Box>

                        {steps.map((step, index) => {
                            const stepEndMs = stepEnds[index];
                            const active = stepActive[index];
                            const {offset, width, isPoint} = getStepPlacement(step, stepEndMs, start, end, active);
                            const tooltipTitle = buildTooltipTitle(step, stepEndMs, active);
                            return (
                                <Box key={index} role="gantt-row"
                                     sx={{
                                         display: 'grid',
                                         gridTemplateColumns: 'subgrid',
                                         gridColumn: '1 / -1',
                                         alignItems: 'center',
                                         minHeight: ROW_HEIGHT,
                                         mb: 0.5,
                                         borderTop: getRowSeparatorBorder(step, index > 0 ? steps[index - 1] : undefined, index),
                                     }}>
                                    <Box sx={{maxWidth: MAX_LABEL_WIDTH, minWidth: MIN_LABEL_WIDTH, pr: 1, overflow: 'hidden', mr: 0.5}} role="gantt-row-label">
                                        <Tooltip title={getStepLabel(step)}>
                                            <Typography variant="body2" noWrap
                                                        sx={{pl: step.state === "RUN_STEP_ONCE" ? 1.5 : 0}}>{getStepLabel(step)}</Typography>
                                        </Tooltip>
                                    </Box>
                                    <Box sx={{position: 'relative', height: 18}}>
                                        <Tooltip title={tooltipTitle}>
                                            {isPoint ? (
                                                <Circle
                                                    sx={{
                                                        position: 'absolute',
                                                        left: `${offset}%`,
                                                        top: '50%',
                                                        transform: 'translate(-50%, -50%)',
                                                    }}
                                                    fontSize="tiny"
                                                    color={step.state === "SUCCEEDED" ? "success"
                                                        : step.state === "FAILED" ? "error" : "info"}
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
                                                        step={step}
                                                    />
                                                </Box>
                                            )}
                                        </Tooltip>
                                    </Box>
                                </Box>
                            );
                        })}
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
}
