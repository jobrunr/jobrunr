import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import FormControlLabel from '@mui/material/FormControlLabel';
import Switch from '@mui/material/Switch';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {lighten} from "@mui/material";
import {Circle} from "@mui/icons-material";
import {useEffect, useState} from 'react';

const STEP_DEFAULT_LABELS = {
    ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    RETRYING: 'Waiting for retry',
    RUN_STEP_ONCE: 'Step that ran once',
};
const END_STATES = ['SUCCEEDED', 'FAILED'];
const EXCLUDED_STATES = ['AWAITING', 'SCHEDULED', 'DELETED'];
const MIN_ACTIVE_WIDTH_PERCENTAGE = 1;
const MIN_LABEL_WIDTH = 150;
const MAX_LABEL_WIDTH = 250;
const ROW_HEIGHT = 28;
const GANTT_GRID_COLUMNS = 'minmax(0, max-content) 1fr 90px';

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
    const finalStateMs = jobInProgress ? null : (steps.length > 0 ? asMs(steps[steps.length - 1].createdAt) : null);
    let start = Infinity, end = -Infinity;
    const stepEndMap = new Map();
    steps.forEach((step, i) => {
        const stepStart = asMs(step.createdAt);
        if (stepStart < start) start = stepStart;
        const {end: stepEnd, active} = getStepEndAt(step, getNextJobHistoryStep(steps, i), jobInProgress, now);
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndMap.set(step, {end: stepEnd, active});
    });
    if (end < start) end = start;
    return {start, end, stepEndMap, finalStateMs};
};

const isRetryScheduledStep = (step) => step?.state === 'SCHEDULED' && (step.reason ?? '').includes('Retry');

const toTimelineSteps = (executionSteps) => {
    const steps = [];
    for (let i = 0; i < executionSteps.length; i++) {
        const step = executionSteps[i];
        const scheduledStep = executionSteps[i + 1];
        if (step.state === 'FAILED' && isRetryScheduledStep(scheduledStep)) {
            const nextAfterScheduled = executionSteps[i + 2];
            const retryPickedUp = nextAfterScheduled?.state === 'ENQUEUED';
            const retryAborted = nextAfterScheduled !== undefined && !retryPickedUp;
            if (!retryAborted) {
                steps.push({
                    state: 'RETRYING',
                    createdAt: step.createdAt,
                    barStart: scheduledStep.createdAt,
                    reason: scheduledStep.reason
                });
                i += retryPickedUp ? 2 : 1;
                continue;
            }
        }
        if (!EXCLUDED_STATES.includes(step.state)) steps.push(step);
    }
    return steps;
};

const toCompactSteps = (steps) => {
    const processingSteps = steps.filter((s) => s.state === 'PROCESSING');
    if (processingSteps.length === 0) return steps;
    const consolidated = {
        state: 'PROCESSING',
        isConsolidated: true,
        createdAt: processingSteps[0].createdAt,
    };
    const filtered = steps.filter((s) => s.state !== 'PROCESSING');
    const firstEnqueuedIndex = filtered.findIndex((s) => s.state === 'ENQUEUED');
    const insertIndex = firstEnqueuedIndex === -1 ? 0 : firstEnqueuedIndex + 1;
    const result = [...filtered];
    result.splice(insertIndex, 0, consolidated);
    return result;
};

const getStepPlacement = (step, stepEndMs, start, end, active) => {
    const duration = end - start;
    const percentage = (ms) => duration > 0 ? (ms / duration) * 100 : 0;
    const dotOffset = percentage(asMs(step.createdAt) - start);
    const stepStart = asMs(step.barStart ?? step.createdAt);
    const offset = percentage(stepStart - start);
    if (active) {
        let width = Math.max(percentage(stepEndMs - stepStart), 0);
        if (width < MIN_ACTIVE_WIDTH_PERCENTAGE) width = MIN_ACTIVE_WIDTH_PERCENTAGE;
        return {offset, width, isPoint: false, dotOffset};
    }
    if (stepEndMs === null || stepEndMs <= stepStart) return {offset, width: null, isPoint: true, dotOffset};
    return {offset, width: Math.max(percentage(stepEndMs - stepStart), 0), isPoint: false, dotOffset};
};

const getStepLabel = (step) => {
    if (step.isConsolidated) return 'Execution time';
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName.split('__')[0];
    return STEP_DEFAULT_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const buildStepLabelTooltip = (step) =>
    step.state === 'RETRYING' && step.reason ? `${getStepLabel(step)} - ${step.reason}` : getStepLabel(step);

const getRowSeparatorBorder = (step, previousStep, index) => {
    if (index === 0) return undefined;
    return step.state === 'RETRYING' ? '1px solid lightgray' : 'none';
};

const formatHumanReadableDate = (ms, detailed = true) => {
    const date = new Date(ms);
    const pad = (n) => String(n).padStart(2, '0');
    return pad(date.getDate()) + "/" + pad(date.getMonth() + 1) + "/" + pad(date.getFullYear()) + " "
        + pad(date.getHours()) + ":" + pad(date.getMinutes()) + ":" + pad(date.getSeconds()) + (detailed ? "." + date.getMilliseconds() : "");
}

const formatElapsed = (ms, totalMs) => {
    if (ms <= 0) return '+0s';
    const s = ms / 1000;
    if (totalMs < 60000) return totalMs < 10000 ? `+${s.toFixed(1)}s` : `+${Math.round(s)}s`;
    if (totalMs < 3600000) {
        const m = Math.floor(s / 60);
        return `+${m}:${String(Math.round(s % 60)).padStart(2, '0')}`;
    }
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    return `+${h}:${String(m).padStart(2, '0')}:${String(Math.round(s % 60)).padStart(2, '0')}`;
};

const buildTooltipTitle = (step, stepEndMs, active) => {
    const startMs = asMs(step.barStart ?? step.createdAt);
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
    if (!Number.isFinite(ms) || ms <= 0) return '';
    const totalSeconds = ms / 1000;
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = Math.round((totalSeconds % 60) * 1000) / 1000;
    const parts = [];
    if (days) parts.push(`${days}d`);
    if (hours) parts.push(`${hours}h`);
    if (minutes) parts.push(`${minutes}m`);
    if (seconds) parts.push(`${seconds}s`);
    return parts.join(' ');
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
    const successLight = lighten(theme.palette.success.light, 0.2);

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
            backgroundColor: step.state === 'ENQUEUED' || step.state === 'RETRYING' ? infoLight
                : step.succeeded === false ? errorLight
                    : step.succeeded === true ? successLight
                        : warningLight,
            ...(active && {
                width: '100%',
                transform: 'none !important',
                animation: `${animateInProgressBar} 1s linear infinite !important`,
                backgroundImage: step.state === 'ENQUEUED' || step.state === 'RETRYING'
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
    const [compact, setCompact] = useState(true);
    const rawSteps = toTimelineSteps(executionSteps ?? []);
    const jobInProgress = rawSteps.length > 0 && !END_STATES.includes(rawSteps[rawSteps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (!jobInProgress) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [jobInProgress]);

    if (rawSteps.length === 0) return null;

    const {start, end, stepEndMap, finalStateMs} = convertStepsToTimeline(rawSteps, now);
    const steps = compact ? toCompactSteps(rawSteps) : rawSteps;
    const duration = end - start;

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{
                    '& .MuiBox-root[role=gantt-row-label]': {
                        borderRight: "1px solid lightgray"
                    }
                }}>
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2}}>
                        <Typography variant="h5">Execution Timeline</Typography>
                        <FormControlLabel
                            control={<Switch size="small" checked={compact} onChange={(e) => setCompact(e.target.checked)}/>}
                            label="Compact"
                            labelPlacement="start"
                        />
                    </Box>

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
                                <AxisLabel sx={{left: 0}}>{formatElapsed(0, duration)}</AxisLabel>
                                <AxisLabel sx={{left: '25%', transform: 'translateX(-50%)'}}>{formatElapsed(duration / 4, duration)}</AxisLabel>
                                <AxisLabel sx={{left: '50%', transform: 'translateX(-50%)'}}>{formatElapsed(duration / 2, duration)}</AxisLabel>
                                <AxisLabel sx={{left: '75%', transform: 'translateX(-50%)'}}>{formatElapsed(3 * duration / 4, duration)}</AxisLabel>
                                <AxisLabel sx={{right: 0}}>{formatElapsed(duration, duration)}</AxisLabel>
                            </Box>
                        </Box>

                        {steps.map((step, index) => {
                            const info = step.isConsolidated
                                ? {end: finalStateMs ?? now, active: finalStateMs === null}
                                : stepEndMap.get(step);
                            const stepEndMs = info.end;
                            const active = info.active;
                            const {offset, width, isPoint, dotOffset} = getStepPlacement(step, stepEndMs, start, end, active);
                            const tooltipTitle = buildTooltipTitle(step, stepEndMs, active);
                            return (
                                <Box key={index} role="gantt-row"
                                     onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f0f0f0'}
                                     onMouseOut={(e) => e.currentTarget.style.backgroundColor = ''}
                                     sx={{
                                         display: 'grid',
                                         gridTemplateColumns: 'subgrid',
                                         gridColumn: '1 / -1',
                                         alignItems: 'center',
                                         minHeight: ROW_HEIGHT,
                                         px: 0.5,
                                         borderTop: getRowSeparatorBorder(step, index > 0 ? steps[index - 1] : undefined, index),
                                     }}>
                                    <Box sx={{
                                        maxWidth: MAX_LABEL_WIDTH,
                                        minWidth: MIN_LABEL_WIDTH,
                                        height: ROW_HEIGHT,
                                        alignContent: "center",
                                        pr: 1,
                                        overflow: 'hidden',
                                        mr: 0.5
                                    }}
                                         role="gantt-row-label">
                                        <Tooltip title={buildStepLabelTooltip(step)}>
                                            <Typography variant="body2" noWrap
                                                        sx={{
                                                            pl: step.state === "RUN_STEP_ONCE" ? 1.5 : 0,
                                                            fontWeight: step.state === "RUN_STEP_ONCE" ? 'normal' : 'bold'
                                                        }}>
                                                {getStepLabel(step)}
                                            </Typography>
                                        </Tooltip>
                                    </Box>
                                    <Box sx={{position: 'relative', height: 18}}>
                                        {step.state === 'RETRYING' ? (
                                            <>
                                                <Tooltip title={tooltipTitle}>
                                                    <Box sx={{
                                                        position: 'absolute',
                                                        left: `${offset}%`,
                                                        width: `${width ?? 0}%`,
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
                                                </Tooltip>
                                            </>
                                        ) : (
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
                                                        color={step.state === "SUCCEEDED" || step.succeeded === true ? "success"
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
                                        )}
                                    </Box>
                                    {!info.active && <Box>
                                        <Typography
                                            sx={{fontSize: "11px", textAlign: "right", alignSelf: "center"}}>{formatDuration(start, stepEndMs)}</Typography>
                                    </Box>}
                                </Box>
                            );
                        })}
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
}
