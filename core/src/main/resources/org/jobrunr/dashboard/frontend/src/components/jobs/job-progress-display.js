import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {lighten, ToggleButton, ToggleButtonGroup} from "@mui/material";
import {Circle} from "@mui/icons-material";
import {Fragment, useEffect, useState} from 'react';

const STEP_LABELS = {
    AWAITING: 'Awaiting',
    SCHEDULED: 'Scheduled',
    ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    RETRYING: 'Waiting for retry',
    RUN_STEP_ONCE: 'Step (runStepOnce)',
};

const END_STATES = ['SUCCEEDED', 'FAILED'];
const EXCLUDED_COMPACT = ['AWAITING', 'SCHEDULED', 'DELETED'];
const EXCLUDED_NON_COMPACT = ['AWAITING', 'DELETED'];
const MIN_LABEL_WIDTH = 150;
const MAX_LABEL_WIDTH = 250;
const ROW_HEIGHT = 28;
const GANTT_COLUMNS = 'minmax(0, max-content) 1fr 90px';

const asMs = (date) => new Date(date).getTime();

const asMicros = (date) => {
    const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$/.exec(String(date));
    if (!match) return asMs(date) * 1000;
    const frac = match[2] ? (match[2] + '000000').slice(0, 6) : '0';
    return Date.parse(match[1] + (match[3] || 'Z')) * 1000 + parseInt(frac, 10);
};

const getStepEndTime = (step) =>
    step.updatedAt && asMicros(step.updatedAt) > asMicros(step.createdAt) ? asMs(step.updatedAt) : null;

const convertStepsToTimeline = (steps, now) => {
    const inProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
    const finalStateMs = inProgress ? null : (steps.length > 0 ? asMs(steps[steps.length - 1].createdAt) : null);
    let start = Infinity, end = -Infinity;
    const stepEndMap = new Map();

    steps.forEach((step, i) => {
        const stepStart = asMs(step.createdAt);
        if (stepStart < start) start = stepStart;
        const nextStep = steps.slice(i + 1).find((s) => s.state !== 'RUN_STEP_ONCE');

        let stepEnd, active = false;
        if (END_STATES.includes(step.state)) {
            stepEnd = stepStart;
        } else if (step.state === 'RUN_STEP_ONCE') {
            const e = getStepEndTime(step);
            stepEnd = e ?? (inProgress ? now : null);
            active = e === null && inProgress;
        } else if (nextStep) {
            stepEnd = asMs(nextStep.createdAt);
        } else {
            stepEnd = inProgress ? now : getStepEndTime(step);
            active = inProgress;
        }

        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndMap.set(step, {end: stepEnd, active});
    });

    return {start: Math.min(start, end), end: Math.max(start, end), stepEndMap, finalStateMs};
};

const toTimelineSteps = (executionSteps, compact) => {
    if (!compact) return executionSteps.filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));

    const steps = [];
    for (let i = 0; i < executionSteps.length; i++) {
        const step = executionSteps[i];
        const next = executionSteps[i + 1];
        if (step.state === 'FAILED' && next?.state === 'SCHEDULED' && (next.reason ?? '').includes('Retry')) {
            const afterNext = executionSteps[i + 2];
            if (afterNext === undefined || afterNext.state === 'ENQUEUED') {
                steps.push({state: 'RETRYING', createdAt: step.createdAt, barStart: next.createdAt, reason: next.reason});
                i += afterNext?.state === 'ENQUEUED' ? 2 : 1;
                continue;
            }
        }
        if (!EXCLUDED_COMPACT.includes(step.state)) steps.push(step);
    }
    return steps;
};

const toCompactSteps = (steps) => {
    const processing = steps.filter((s) => s.state === 'PROCESSING');
    if (processing.length === 0) return steps;
    const result = steps.filter((s) => s.state !== 'PROCESSING');
    const idx = result.findIndex((s) => s.state === 'ENQUEUED');
    result.splice(idx === -1 ? 0 : idx + 1, 0, {state: 'PROCESSING', isConsolidated: true, createdAt: processing[0].createdAt});
    return result;
};

const getStepPlacement = (step, stepEndMs, start, end, active) => {
    const duration = end - start;
    const pct = (ms) => duration > 0 ? (ms / duration) * 100 : 0;
    const stepStart = asMs(step.barStart ?? step.createdAt);
    const offset = pct(stepStart - start);

    if (active) return {offset, width: Math.max(pct(stepEndMs - stepStart), 1), isPoint: false};
    if (stepEndMs === null || stepEndMs <= stepStart) return {offset, width: null, isPoint: true};
    return {offset, width: Math.max(pct(stepEndMs - stepStart), 0), isPoint: false};
};

const getStepLabel = (step) => {
    if (step.isConsolidated) return 'Execution time';
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName.split('__')[0];
    return STEP_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const formatDate = (ms, detailed = true) => {
    const d = new Date(ms);
    const p = (n) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${detailed ? 'at' : ''} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}${detailed ? '.' + d.getMilliseconds() : ''}`;
};

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    if (!Number.isFinite(ms) || ms <= 0) return '0s';
    const s = ms / 1000;
    const d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60), sec = Math.round((s % 60) * 1000) / 1000;
    return [d && `${d}d`, h && `${h}h`, m && `${m}m`, sec && `${sec}s`].filter(Boolean).join(' ');
};

const buildTooltipTitle = (step, stepEndMs, active) => {
    const startMs = asMs(step.barStart ?? step.createdAt);
    const includeEnd = !(stepEndMs === null) && !(stepEndMs === startMs);
    return (
        <Box>
            <Typography variant="caption" component="p">{includeEnd ? "Started" : "Completed"} on {formatDate(startMs)}</Typography>
            {includeEnd && <Typography variant="caption" component="p">Ended on {formatDate(stepEndMs)}</Typography>}
            {!END_STATES.includes(step.state) && (active ? <Typography variant="caption" component="p">Still in progress...</Typography> :
                    <Typography variant="caption" component="p">
                        Took {includeEnd ? formatDuration(startMs, stepEndMs) : "<10 ms"}
                    </Typography>
            )}
            {step.result && <Typography variant="caption" component="p">Result: {step.result}</Typography>}
        </Box>
    )
};

const generateTimeTicks = (durationMs) => {
    if (!durationMs || durationMs <= 0) return [{ms: 0, pct: 0, label: '0'}];
    const sec = durationMs / 1000;
    let stepSec = Math.round((sec / 4) / 5) * 5 || (sec < 5 ? 1 : 5);
    if (stepSec > 60) stepSec = Math.round(stepSec / 60) * 60;
    else if (stepSec > 20) stepSec = Math.round(stepSec / 15) * 15;

    const ticks = [];
    for (let ms = 0; ms <= durationMs; ms += stepSec * 1000) {
        ticks.push({ms, pct: (ms / durationMs) * 100, label: ms === 0 ? '0' : `+${formatDuration(0, ms)}`});
    }
    return ticks;
};

const animateInProgressBar = keyframes`
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 28px 0;
    }
`;

const getBarColor = (step, theme) => {
    if (['ENQUEUED', 'RETRYING'].includes(step.state)) return theme.palette.info.light;
    if (step.state === 'SCHEDULED') return theme.palette.grey[600];
    if (step.succeeded === false || step.state === 'FAILED') return theme.palette.error.light;
    if (step.succeeded === true || step.state === 'SUCCEEDED') return theme.palette.success.light;
    return theme.palette.warning.light;
};

const GanttBar = styled(LinearProgress, {
    shouldForwardProp: (prop) => prop !== 'active',
})(({theme, active, step}) => {
    const color = getBarColor(step, theme);
    const lighter = lighten(color, 0.4);

    return {
        height: '50%',
        width: '100%',
        alignSelf: 'center',
        borderRadius: 4,
        [`&.${linearProgressClasses.colorPrimary}`]: {backgroundColor: active ? '#f0f4f8' : 'transparent'},
        [`& .${linearProgressClasses.bar}`]: {
            borderRadius: 4,
            backgroundColor: color,
            ...(active && {
                width: '100%',
                transform: 'none !important',
                animation: `${animateInProgressBar} 1s linear infinite !important`,
                backgroundImage: `repeating-linear-gradient(45deg, ${color}, ${color} 10px, ${lighter} 10px, ${lighter} 20px)`,
                backgroundSize: '28px 28px',
            }),
        },
        ...(active && {[`& .${linearProgressClasses.bar2Indeterminate}`]: {display: 'none'}}),
    };
});

const RetrySeparator = ({label}) => (
    <Box aria-hidden="true" sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pt: 0, pb: 0, zIndex: 1}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600}}>{label}</Typography>
        <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}}/>
    </Box>
);

export const JobProgressDisplay = ({executionSteps}) => {
    const [timelineMode, setTimelineMode] = useState(localStorage.getItem("executionTimelineMode") ?? "compact");
    const rawSteps = toTimelineSteps(executionSteps ?? [], timelineMode === "compact");
    const inProgress = rawSteps.length > 0 && !END_STATES.includes(rawSteps[rawSteps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (!inProgress) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [inProgress]);

    if (rawSteps.length === 0) return null;

    const {start, end, stepEndMap, finalStateMs} = convertStepsToTimeline(rawSteps, now);
    const steps = timelineMode ? toCompactSteps(rawSteps) : rawSteps;
    const duration = end - start;
    const ticks = generateTimeTicks(duration);
    let retryCount = 0;

    const changeMode = (event, compact) => {
        localStorage.setItem("executionTimelineMode", compact);
        setTimelineMode(compact);
    }

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{position: 'relative'}}>
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2}}>
                        <Box>
                            <Typography variant="h5">Execution Timeline</Typography>
                            <Typography variant="body2">Started at {formatDate(start, false)}, took {formatDuration(asMs(start), asMs(end))}</Typography>
                        </Box>
                        <ToggleButtonGroup
                            onChange={changeMode}
                            value={timelineMode}
                            exclusive
                            size="small"
                        >
                            <ToggleButton value={"compact"}>Compact</ToggleButton>
                            <ToggleButton value={"detailed"}>Detailed</ToggleButton>
                        </ToggleButtonGroup>
                    </Box>

                    <Box sx={{display: 'grid', gridTemplateColumns: GANTT_COLUMNS, position: 'relative'}}>
                        <Box sx={{display: 'grid', gridTemplateColumns: 'subgrid', gridColumn: '1 / -1', alignItems: 'flex-end', height: 18, mb: 1}}>
                            <Box role="gantt-row-label" sx={{maxWidth: MAX_LABEL_WIDTH, minWidth: MIN_LABEL_WIDTH, pr: 1}}/>
                            <Box sx={{position: 'relative', height: 18}}>
                                {ticks.map((t) => (
                                    <Typography key={t.ms} variant="caption" sx={{
                                        position: 'absolute',
                                        top: 0,
                                        left: `${t.pct}%`,
                                        transform: 'translateX(-50%)',
                                        whiteSpace: 'nowrap',
                                        color: 'text.secondary'
                                    }}>
                                        {t.label}
                                    </Typography>
                                ))}
                            </Box>
                            <Box/>
                        </Box>
                        <Box aria-hidden="true"
                             sx={{position: 'absolute', top: 26, bottom: 0, gridColumn: '2 / 3', width: '100%', pointerEvents: 'none', zIndex: 0}}>
                            {ticks.map((t) => (
                                <Box key={t.ms} sx={{
                                    position: 'absolute',
                                    top: 0,
                                    bottom: 0,
                                    left: `${t.pct}%`,
                                    borderLeft: '1px solid',
                                    borderColor: 'divider',
                                    opacity: 0.6
                                }}/>
                            ))}
                        </Box>

                        {steps.map((step, index) => {
                            const isRetry = index > 0 && (step.state === 'RETRYING' || step.state === 'SCHEDULED');
                            if (isRetry) retryCount += 1;

                            const info = step.isConsolidated ? {end: finalStateMs ?? now, active: finalStateMs === null} : stepEndMap.get(step);
                            const stepStartMs = asMs(step.createdAt) ?? null;
                            const stepEndMs = info?.end ?? null;
                            const active = info?.active ?? false;
                            const {offset, width, isPoint} = getStepPlacement(step, stepEndMs, start, end, active);
                            const isStep = step.state === 'RUN_STEP_ONCE';

                            return (
                                <Fragment key={index}>
                                    {isRetry && <RetrySeparator label={`Retry ${retryCount}`}/>}
                                    <Box role="gantt-row"
                                         onMouseOver={(e) => e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.03)'}
                                         onMouseOut={(e) => e.currentTarget.style.backgroundColor = ''}
                                         sx={{
                                             display: 'grid',
                                             gridTemplateColumns: 'subgrid',
                                             gridColumn: '1 / -1',
                                             alignItems: 'center',
                                             minHeight: ROW_HEIGHT,
                                             px: 0.5,
                                             zIndex: 1
                                         }}>
                                        <Box sx={{
                                            maxWidth: MAX_LABEL_WIDTH,
                                            minWidth: MIN_LABEL_WIDTH,
                                            height: ROW_HEIGHT,
                                            display: 'flex',
                                            alignItems: 'center',
                                            pr: 1,
                                            overflow: 'hidden',
                                            mr: 0.5
                                        }} role="gantt-row-label">
                                            <Tooltip
                                                title={step.state === 'RETRYING' && step.reason ? `${getStepLabel(step)} - ${step.reason}` : getStepLabel(step)}>
                                                <Typography variant={isStep ? 'caption' : 'body2'} noWrap sx={{
                                                    pl: isStep ? 2 : 0,
                                                    color: isStep ? 'text.secondary' : 'text.primary',
                                                    display: 'flex',
                                                    alignItems: 'center'
                                                }}>
                                                    {isStep && <Box component="span" sx={{opacity: 0.6, mr: 0.5}}>└</Box>}
                                                    {getStepLabel(step)}
                                                </Typography>
                                            </Tooltip>
                                        </Box>

                                        <Box sx={{position: 'relative', height: 18}}>
                                            <Tooltip title={buildTooltipTitle(step, stepEndMs, active)}>
                                                {isPoint ? (
                                                    <Circle
                                                        sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)'}}
                                                        fontSize="tiny"
                                                        color={step.state === 'SUCCEEDED' || step.succeeded === true ? 'success' : step.state === 'FAILED' || step.succeeded === false ? 'error' : 'info'}
                                                    />
                                                ) : (
                                                    <Box sx={{
                                                        position: 'absolute',
                                                        left: `${offset}%`,
                                                        width: `${width}%`,
                                                        top: 0,
                                                        bottom: 0,
                                                        display: 'flex',
                                                        alignItems: 'center'
                                                    }}>
                                                        <GanttBar active={active} variant={active ? 'indeterminate' : 'determinate'}
                                                                  value={active ? undefined : 100} step={step}/>
                                                    </Box>
                                                )}
                                            </Tooltip>
                                        </Box>

                                        {!info?.active && (
                                            <Box>
                                                <Typography
                                                    sx={{fontSize: '11px', textAlign: 'right', color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
                                                    {formatDuration(stepStartMs, stepEndMs)}
                                                </Typography>
                                            </Box>
                                        )}
                                    </Box>
                                </Fragment>
                            );
                        })}
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
};