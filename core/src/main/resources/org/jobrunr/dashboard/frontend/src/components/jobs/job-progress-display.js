import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {IconButton, lighten, ToggleButton, ToggleButtonGroup} from "@mui/material";
import {Circle, MoreHoriz} from "@mui/icons-material";
import {HelpCircleOutline} from "mdi-material-ui";
import {Fragment, useEffect, useState} from 'react';

const STEP_LABELS = {
    AWAITING: 'Awaiting', SCHEDULED: 'Scheduled', ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing', SUCCEEDED: 'Succeeded', FAILED: 'Failed',
    RETRYING: 'Waiting for retry', RUN_STEP_ONCE: 'Step (runStepOnce)',
};

const END_STATES = ['SUCCEEDED', 'FAILED'];
const EXCLUDED_COMPACT = ['AWAITING', 'SCHEDULED', 'DELETED'];
const EXCLUDED_NON_COMPACT = ['AWAITING', 'DELETED'];
const MIN_LABEL_WIDTH = 150, MAX_LABEL_WIDTH = 250, ROW_HEIGHT = 28;
const GANTT_COLUMNS = 'minmax(0, max-content) 1fr 90px';
const MIN_COMPRESSION_THRESHOLD_MS = 60000, COMPRESSION_THRESHOLD = 0.15;

const asMs = (date) => new Date(date).getTime();

const asMicros = (date) => {
    const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$/.exec(String(date));
    if (!match) return asMs(date) * 1000;
    const frac = match[2] ? (match[2] + '000000').slice(0, 6) : '0';
    return Date.parse(match[1] + (match[3] || 'Z')) * 1000 + parseInt(frac, 10);
};

const getStepEndTime = (step) => step.updatedAt && asMicros(step.updatedAt) > asMicros(step.createdAt) ? asMs(step.updatedAt) : null;

const convertStepsToTimeline = (steps, now) => {
    const inProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
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
        } else {
            stepEnd = nextStep ? asMs(nextStep.createdAt) : (inProgress ? now : getStepEndTime(step));
            active = !nextStep && inProgress;
        }

        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndMap.set(step, {end: stepEnd, active});
    });

    return {start: Math.min(start, end), end: Math.max(start, end), stepEndMap};
};

const createTimeCompressor = (longRanges, totalDuration) => {
    const totalLongGapDuration = longRanges.reduce((sum, r) => sum + (r.endMs - r.startMs), 0);
    const uncompressedDuration = Math.max(totalDuration - totalLongGapDuration, 1000);
    const compressionFactor = Math.min(Math.max((0.25 * uncompressedDuration) / Math.max(totalLongGapDuration, 1), 0.01), 0.10);

    return (timeMs) => {
        let compressedTimeSaved = 0;
        for (const r of longRanges) {
            if (timeMs <= r.startMs) break;
            const spanInGap = Math.min(timeMs, r.endMs) - r.startMs;
            compressedTimeSaved += spanInGap * (1 - compressionFactor);
        }
        return timeMs - compressedTimeSaved;
    };
};

const toTimelineSteps = (executionSteps, compact) => {
    if (!compact) return executionSteps.filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));

    const steps = [];
    for (let i = 0; i < executionSteps.length; i++) {
        const step = executionSteps[i], next = executionSteps[i + 1];
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

const getStepLabel = (step) => {
    if (step.isConsolidated) return 'Execution time';
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName.split('__')[0];
    return STEP_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const formatDate = (ms, detailed = true) => {
    const d = new Date(ms), p = (n) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${detailed ? 'at' : ''} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}${detailed ? '.' + d.getMilliseconds() : ''}`;
};

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    if (!Number.isFinite(ms) || ms <= 0) return '<10 ms';
    const s = ms / 1000, d = Math.floor(s / 86400), h = Math.floor((s % 86400) / 3600), m = Math.floor((s % 3600) / 60),
        sec = Math.round((s % 60) * 1000) / 1000;
    return [d && `${d}d`, h && `${h}h`, m && `${m}m`, sec && `${sec}s`].filter(Boolean).slice(0, 2).join(' ');
};

const buildTooltipTitle = (step, stepEndMs, active, isCompressed = false) => {
    const startMs = asMs(step.barStart ?? step.createdAt), includeEnd = !(stepEndMs === null) && !(stepEndMs === startMs);
    return (
        <Box>
            <Typography variant="caption" component="p">{includeEnd ? "Started" : "Completed"} on {formatDate(startMs)}</Typography>
            {includeEnd && <Typography variant="caption" component="p">Ended on {formatDate(stepEndMs)}</Typography>}
            {!END_STATES.includes(step.state) && (
                <Typography variant="caption" component="p">
                    {active ? "Still in progress..." : `Took ${includeEnd ? formatDuration(startMs, stepEndMs) : "<10 ms"}`}{isCompressed && " (visually shortened)"}
                </Typography>
            )}
            {step.result && <Typography variant="caption" component="p">Result: {step.result}</Typography>}
        </Box>
    );
};

const generateTimeTicks = (durationMs, compressTime, timelineStartMs, visualDurationMs, longRanges = []) => {
    if (!durationMs || durationMs <= 0) return [{ms: 0, pct: 0, label: '0'}];

    const visualTimelineStartMs = compressTime(timelineStartMs);
    const breakTicks = longRanges.map((r) => {
        const midRealMs = r.startMs + (r.endMs - r.startMs) / 2;
        const pct = visualDurationMs > 0 ? ((compressTime(midRealMs) - visualTimelineStartMs) / visualDurationMs) * 100 : 0;
        const startRel = r.startMs - timelineStartMs, endRel = r.endMs - timelineStartMs;
        const startLabel = startRel <= 0 ? '0' : `+${formatDuration(0, startRel)}`;
        return {ms: startRel, pct, label: `${startLabel} ... +${formatDuration(0, endRel)}`, isBreak: true};
    });

    const sec = durationMs / 1000;
    let stepSec = Math.round((sec / 4) / 5) * 5 || (sec < 5 ? 1 : 5);
    if (stepSec > 60) stepSec = Math.round(stepSec / 60) * 60;
    else if (stepSec > 20) stepSec = Math.round(stepSec / 15) * 15;

    const regularTicks = [];
    for (let ms = 0; ms <= durationMs; ms += stepSec * 1000) {
        const realTime = timelineStartMs + ms;
        if (!longRanges.some(r => realTime > r.startMs && realTime < r.endMs)) {
            const pct = visualDurationMs > 0 ? ((compressTime(realTime) - visualTimelineStartMs) / visualDurationMs) * 100 : 0;
            regularTicks.push({ms, pct, label: ms === 0 ? '0' : `+${formatDuration(0, ms)}`, isBreak: false});
        }
    }

    const candidates = [...breakTicks];
    regularTicks.forEach(rt => {
        if (!breakTicks.some(bt => Math.abs(bt.pct - rt.pct) < 8)) candidates.push(rt);
    });
    candidates.sort((a, b) => a.pct - b.pct);

    const ticks = [];
    for (const t of candidates) {
        const prev = ticks[ticks.length - 1];
        if (!prev || t.isBreak || prev.isBreak || (t.pct - prev.pct >= 6) || t.pct >= 98) ticks.push(t);
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

const BreakIndicator = ({leftPct = 50}) => (
    <Box sx={{
        position: 'absolute', left: `${leftPct}%`, top: '50%', transform: 'translate(-50%, -50%)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 3,
        bgcolor: 'background.paper', px: 0.5, maxHeight: "12px", borderRadius: 2, border: "1px solid lightgray",
    }}>
        <MoreHoriz sx={{p: 0, m: 0}}/>
    </Box>
);

const RetrySeparator = ({label}) => (
    <Box aria-hidden="true" sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, zIndex: 1}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600}}>{label}</Typography>
        <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}}/>
    </Box>
);

const Legend = () => (
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
    </Box>
);

const groupStepsSequentially = (executionSteps, stepEndMap, now) => {
    const rows = [
        {key: 'SCHEDULED', label: 'Scheduled', isStep: false, items: []},
        {key: 'ENQUEUED', label: 'Enqueued', isStep: false, items: []},
        {key: 'PROCESSING', label: 'Processing', isStep: false, items: []},
    ];
    const stepMap = new Map();

    executionSteps.forEach((step, idx) => {
        const info = stepEndMap.get(step), startMs = asMs(step.barStart ?? step.createdAt);
        const endMs = info?.end ?? (info?.active ? now : startMs);
        const nextStep = executionSteps.slice(idx + 1).find((s) => s.state !== 'RUN_STEP_ONCE');

        if (step.state === 'RUN_STEP_ONCE') {
            const name = getStepLabel(step);
            if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
            stepMap.get(name).items.push({...step, startMs, endMs, active: info?.active, isSkipped: step.skipped || step.isSkipped});
        } else if (['SCHEDULED', 'ENQUEUED', 'PROCESSING'].includes(step.state)) {
            const row = rows.find(r => r.key === step.state);
            if (row) {
                let outcome = null;
                if (step.state === 'PROCESSING') {
                    if (nextStep?.state === 'FAILED' || step.succeeded === false) outcome = 'FAILED';
                    if (nextStep?.state === 'SUCCEEDED' || step.succeeded === true) outcome = 'SUCCEEDED';
                }
                row.items.push({...step, startMs, endMs, active: info?.active, outcome});
            }
        }
    });

    return [...rows.filter(r => r.items.length > 0), ...Array.from(stepMap.values())].map(row => ({
        ...row,
        totalMs: row.items.reduce((sum, item) => sum + Math.max(0, (item.endMs ?? item.startMs) - item.startMs), 0)
    }));
};

export const JobProgressDisplay = ({executionSteps}) => {
    const [timelineMode, setTimelineMode] = useState(localStorage.getItem("executionTimelineMode") ?? "compact");
    const rawSteps = (executionSteps ?? []).filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));
    const detailedSteps = toTimelineSteps(executionSteps ?? [], false);
    const inProgress = rawSteps.length > 0 && !END_STATES.includes(rawSteps[rawSteps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (!inProgress) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [inProgress]);

    if (rawSteps.length === 0) return null;

    const {start, end, stepEndMap} = convertStepsToTimeline(rawSteps, now);
    const duration = end - start, compressionThresholdMs = Math.max(MIN_COMPRESSION_THRESHOLD_MS, duration * COMPRESSION_THRESHOLD);

    const timestamps = new Set([start, end]);
    rawSteps.forEach((step) => {
        const sMs = asMs(step.barStart ?? step.createdAt), info = stepEndMap.get(step), eMs = info?.end ?? (info?.active ? now : sMs);
        if (Number.isFinite(sMs)) timestamps.add(sMs);
        if (Number.isFinite(eMs)) timestamps.add(eMs);
    });

    const sortedTs = Array.from(timestamps).sort((a, b) => a - b), longRanges = [];
    for (let i = 0; i < sortedTs.length - 1; i++) {
        if (sortedTs[i + 1] - sortedTs[i] > compressionThresholdMs) longRanges.push({startMs: sortedTs[i], endMs: sortedTs[i + 1]});
    }

    const compressTime = createTimeCompressor(longRanges, duration);
    const vStart = compressTime(start), vEnd = compressTime(end), vDuration = vEnd - vStart;
    const ticks = generateTimeTicks(duration, compressTime, start, vDuration, longRanges);
    let retryCount = 0;

    const groupedRows = timelineMode === "compact" ? groupStepsSequentially(rawSteps, stepEndMap, now) : [];

    const changeMode = (event, mode) => {
        if (!mode) return;
        localStorage.setItem("executionTimelineMode", mode);
        setTimelineMode(mode);
    };

    const getPlacement = (startMs, endMs, state) => {
        const itemStartMs = startMs, itemEndMs = endMs ?? startMs;
        const vS = compressTime(itemStartMs), vE = compressTime(itemEndMs);
        const pct = (v) => vDuration > 0 ? ((v - vStart) / vDuration) * 100 : 0;

        const offset = pct(vS), calculatedWidth = pct(vE) - offset, isEndState = END_STATES.includes(state);
        const itemBreaks = longRanges
            .filter(r => r.startMs >= itemStartMs && r.endMs <= itemEndMs)
            .map(r => {
                const barVDuration = vE - vS, breakVPos = compressTime(r.startMs + (r.endMs - r.startMs) / 2);
                return barVDuration > 0 ? ((breakVPos - vS) / barVDuration) * 100 : 50;
            });

        const isCompressed = itemBreaks.length > 0;
        return {
            offset: isEndState ? offset : (calculatedWidth === 0 ? offset - 0.5 : offset),
            width: isEndState ? 0 : Math.max(calculatedWidth, isCompressed ? 3.0 : 0.5),
            isPoint: isEndState,
            isCompressed,
            breakOffsets: itemBreaks,
        };
    };

    const renderBarOrCircle = (item, isPoint, offset, width, isCompressed, breakOffsets = []) => (
        <Tooltip title={buildTooltipTitle(item, item.endMs ?? item.stepEndMs, item.active, isCompressed)}>
            {item.isSkipped ? (
                <Circle fontSize="tiny" sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)', color: 'grey.500'}}/>
            ) : isPoint ? (
                <Circle fontSize="tiny" color={item.succeeded === false || item.state === 'FAILED' ? 'error' : 'success'}
                        sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)'}}/>
            ) : (
                <Box sx={{position: 'absolute', left: `${offset}%`, width: `${width}%`, top: 0, bottom: 0, display: 'flex', alignItems: 'center'}}>
                    <GanttBar active={item.active} variant={item.active ? 'indeterminate' : 'determinate'} value={item.active ? undefined : 100} step={item}/>
                    {isCompressed && breakOffsets.map((bOffset, bIdx) => <BreakIndicator key={bIdx} leftPct={bOffset}/>)}
                    {item.outcome && (
                        <Circle fontSize="tiny" color={item.outcome === 'FAILED' ? 'error' : 'success'}
                                sx={{position: 'absolute', right: -6, top: '50%', transform: 'translateY(-50%)', zIndex: 2}}/>
                    )}
                </Box>
            )}
        </Tooltip>
    );

    const renderRowLabel = (label, isStep) => (
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
            <Typography variant={isStep ? 'caption' : 'body2'} noWrap
                        sx={{pl: isStep ? 2 : 0, color: isStep ? 'text.secondary' : 'text.primary', display: 'flex', alignItems: 'center'}}>
                {isStep && <Box component="span" sx={{opacity: 0.6, mr: 0.5}}>└</Box>}
                {label}
            </Typography>
        </Box>
    );

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{position: 'relative'}}>
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2}}>
                        <Box>
                            <Typography variant="h5">
                                Execution Timeline
                                <Tooltip title={"Learn more about Durable Executions"}>
                                    <IconButton size="small" target="_blank" href="https://www.jobrunr.io/en/blog/what-is-durable-execution-java/">
                                        <HelpCircleOutline sx={{fontSize: '1.25rem'}}/>
                                    </IconButton>
                                </Tooltip>
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Started at {formatDate(start, false)}, took {formatDuration(asMs(start), asMs(end))}
                            </Typography>
                        </Box>
                        <ToggleButtonGroup onChange={changeMode} value={timelineMode} exclusive size="small">
                            <ToggleButton value="compact">Compact</ToggleButton>
                            <ToggleButton value="detailed">Detailed</ToggleButton>
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

                        {/* COMPACT MODE */}
                        {timelineMode === "compact" && groupedRows.map((row) => (
                            <Box key={row.key} role="gantt-row"
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
                                {renderRowLabel(row.label, row.isStep)}
                                <Box sx={{position: 'relative', height: 18}}>
                                    {row.items.map((item, idx) => {
                                        const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(item.startMs, item.endMs, item.state);
                                        return <Fragment key={idx}>{renderBarOrCircle(item, isPoint, offset, width, isCompressed, breakOffsets)}</Fragment>;
                                    })}
                                </Box>
                                <Box>
                                    <Typography sx={{fontSize: '11px', textAlign: 'right', color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
                                        {formatDuration(0, row.totalMs)}
                                    </Typography>
                                </Box>
                            </Box>
                        ))}

                        {/* DETAILED MODE */}
                        {timelineMode === "detailed" && detailedSteps.map((step, index) => {
                            const isRetry = index > 0 && (step.state === 'RETRYING' || step.state === 'SCHEDULED');
                            if (isRetry) retryCount += 1;

                            const info = stepEndMap.get(step);
                            const stepStartMs = asMs(step.barStart ?? step.createdAt);
                            const stepEndMs = info?.end ?? null;
                            const active = info?.active ?? false;
                            const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(stepStartMs, stepEndMs, step.state);
                            const item = {...step, stepStartMs, stepEndMs, active};

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
                                        {renderRowLabel(getStepLabel(step), step.state === 'RUN_STEP_ONCE')}
                                        <Box sx={{position: 'relative', height: 18}}>
                                            {renderBarOrCircle(item, isPoint, offset, width, isCompressed, breakOffsets)}
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

                    <Legend/>
                </CardContent>
            </Card>
        </Box>
    );
};