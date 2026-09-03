import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {keyframes, styled} from '@mui/material/styles';
import {darken, lighten, ToggleButton, ToggleButtonGroup, useColorScheme, useTheme} from "@mui/material";
import {Rhombus} from "mdi-material-ui";
import {Fragment, useEffect, useState} from 'react';
import {humanReadableMillis} from "../../utils/helper-functions.js";
import {SwitchableTimeFormatter} from "../utils/time-ago.js";

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

const createTimeCompressor = (longRanges, totalDuration, thresholdMs) => {
    const baseScale = Math.max(thresholdMs / 2, 1);
    const floor = 0.15 * baseScale;
    const scaleFor = (r) => {
        const d = r.endMs - r.startMs;
        const share = d / Math.max(totalDuration, 1);
        return Math.min(Math.max(baseScale * (1 - share), floor), baseScale);
    };
    const gapFactor = (r) => {
        const scale = scaleFor(r), d = r.endMs - r.startMs;
        return (scale * Math.log(1 + d / scale)) / d;
    };

    return (timeMs) => {
        let compressedTimeSaved = 0;
        for (const r of longRanges) {
            if (timeMs <= r.startMs) break;
            const spanInGap = Math.min(timeMs, r.endMs) - r.startMs;
            compressedTimeSaved += spanInGap * (1 - gapFactor(r));
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

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    if (!Number.isFinite(ms) || ms <= 0) return '<1 ms';
    return humanReadableMillis(ms);
};

const buildTooltipTitle = (step, stepEndMs, active, isCompressed = false) => {
    const startMs = asMs(step.barStart ?? step.createdAt), includeEnd = !(stepEndMs === null) && !(stepEndMs === startMs);
    return (
        <Box>
            <Typography variant="caption" component="p">{includeEnd ? "Started" : "Completed"}: <SwitchableTimeFormatter date={new Date(startMs)}/></Typography>
            {includeEnd && <Typography variant="caption" component="p">Ended: <SwitchableTimeFormatter date={new Date(stepEndMs)}/></Typography>}
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
        return {ms: (startRel + endRel) / 2, pct, label: `${startLabel} ... +${formatDuration(0, endRel)}`, isBreak: true};
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

const BreakIndicator = ({leftPct = 50, color}) => {
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

const RetrySeparator = ({label}) => (
    <Box aria-hidden="true" sx={{gridColumn: '1 / -1', display: 'flex', alignItems: 'center', gap: 1, pl: 0.5, zIndex: 1}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0, fontWeight: 600, opacity: 0.6}}>{label}</Typography>
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
        <Box sx={{display: 'flex', alignItems: 'center', gap: 0.75}}>
            <Typography sx={{fontWeight: 600}}>//</Typography>
            <Typography variant="caption" color="text.secondary">Compressed</Typography>
        </Box>
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

export const JobProgressDisplay = ({executionSteps, reverse = false}) => {
    const theme = useTheme();
    if (executionSteps[0].state === 'SCHEDULED') executionSteps.shift();

    const [timelineMode, setTimelineMode] = useState(localStorage.getItem("executionTimelineMode") ?? "compact");
    const [compressionMode, setCompressionMode] = useState(localStorage.getItem("executionTimelineCompression") ?? "compressed");
    const rawSteps = (executionSteps ?? []).filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));
    const detailedSteps = toTimelineSteps(executionSteps ?? [], timelineMode === "compact");

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

    const protectedSpans = [];
    rawSteps.forEach((step, i) => {
        if (step.state !== 'PROCESSING') return;
        let hasSubStep = false;
        for (let j = i + 1; j < rawSteps.length && rawSteps[j].state === 'RUN_STEP_ONCE'; j++) hasSubStep = true;
        if (!hasSubStep) return;
        const pStart = asMs(step.createdAt), pEnd = stepEndMap.get(step)?.end ?? pStart;
        if (pEnd > pStart) protectedSpans.push({startMs: pStart, endMs: pEnd});
    });

    const sortedTs = Array.from(timestamps).sort((a, b) => a - b), longRanges = [];
    for (let i = 0; i < sortedTs.length - 1; i++) {
        const s = sortedTs[i], e = sortedTs[i + 1];
        if (e - s <= compressionThresholdMs) continue;
        if (protectedSpans.some((p) => s >= p.startMs && e <= p.endMs)) continue;
        longRanges.push({startMs: s, endMs: e});
    }

    const compressRanges = compressionMode === "actual" ? [] : longRanges;
    const compressTime = createTimeCompressor(compressRanges, duration, compressionThresholdMs);
    const vStart = compressTime(start), vEnd = compressTime(end), vDuration = vEnd - vStart;
    const ticks = generateTimeTicks(duration, compressTime, start, vDuration, compressRanges);

    const compactRetryEvents = [];
    let compactRetryCount = 0;
    rawSteps.forEach((step, idx) => {
        if (idx > 0 && (step.state === 'RETRYING' || step.state === 'SCHEDULED')) {
            compactRetryCount += 1;
            const retryMs = asMs(step.barStart ?? step.createdAt);
            const vMs = compressTime(retryMs);
            const pct = vDuration > 0 ? ((vMs - vStart) / vDuration) * 100 : 0;
            compactRetryEvents.push({count: compactRetryCount, pct, ms: retryMs});
        }
    });

    const groupedRows = timelineMode === "compact" ? groupStepsSequentially(rawSteps, stepEndMap, now) : [];
    const compactRows = reverse ? groupedRows.slice().reverse() : groupedRows;

    let chronologicalRetry = 0;
    const detailedRows = detailedSteps.map((step, index) => {
        const isRetry = index > 0 && (step.state === 'RETRYING' || step.state === 'SCHEDULED');
        if (isRetry) chronologicalRetry += 1;
        return {step, isRetry, retryNumber: chronologicalRetry};
    });
    const orderedDetailedRows = reverse ? detailedRows.slice().reverse() : detailedRows;

    const changeMode = (event, mode) => {
        if (!mode) return;
        localStorage.setItem("executionTimelineMode", mode);
        setTimelineMode(mode);
    };

    const changeCompression = (event, mode) => {
        if (!mode) return;
        localStorage.setItem("executionTimelineCompression", mode);
        setCompressionMode(mode);
    };

    const getPlacement = (startMs, endMs, state) => {
        const itemStartMs = startMs, itemEndMs = endMs ?? startMs;
        const vS = compressTime(itemStartMs), vE = compressTime(itemEndMs);
        const pct = (v) => vDuration > 0 ? ((v - vStart) / vDuration) * 100 : 0;

        const offset = pct(vS), calculatedWidth = pct(vE) - offset, isEndState = END_STATES.includes(state);
        const itemBreaks = compressRanges
            .filter(r => r.startMs >= itemStartMs && r.endMs <= itemEndMs)
            .map(r => {
                const barVDuration = vE - vS, breakVPos = compressTime(r.startMs + (r.endMs - r.startMs) / 2);
                return barVDuration > 0 ? ((breakVPos - vS) / barVDuration) * 100 : 50;
            });

        const isCompressed = itemBreaks.length > 0;
        const baseOffset = isEndState ? offset : (calculatedWidth === 0 ? offset - 0.5 : offset);
        const baseWidth = isEndState ? 0 : Math.max(calculatedWidth, isCompressed ? 3.0 : 0.5);
        return {
            offset: reverse ? 100 - baseOffset - baseWidth : baseOffset,
            width: baseWidth,
            isPoint: isEndState,
            isCompressed,
            breakOffsets: reverse ? itemBreaks.map((b) => 100 - b) : itemBreaks,
        };
    };

    const renderBarOrCircle = (item, isPoint, offset, width, isCompressed, breakOffsets = []) => (
        <Tooltip title={buildTooltipTitle(item, item.endMs ?? item.stepEndMs, item.active, isCompressed)}>
            {item.isSkipped ? (
                <Rhombus fontSize="tiny" sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)', color: 'grey.500'}}/>
            ) : isPoint ? (
                <Rhombus fontSize="tiny" color={item.succeeded === false || item.state === 'FAILED' ? 'error' : 'success'}
                         sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)'}}/>
            ) : (
                <Box sx={{position: 'absolute', left: `${offset}%`, width: `${width}%`, top: 0, bottom: 0, display: 'flex', alignItems: 'center'}}>
                    <GanttBar active={item.active} variant={item.active ? 'indeterminate' : 'determinate'} value={item.active ? undefined : 100} step={item}/>
                    {isCompressed && breakOffsets.map((bOffset, bIdx) => <BreakIndicator key={bIdx} leftPct={bOffset} color={getBarColor(item, theme)}/>)}
                    {item.outcome && (
                        <Rhombus fontSize="tiny" color={item.outcome === 'FAILED' ? 'error' : 'success'}
                                 sx={{position: 'absolute', [reverse ? 'left' : 'right']: -6, top: '50%', transform: 'translateY(-50%)', zIndex: 2}}/>
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
                            <Typography variant="body1" color="text.secondary">
                                Created: <SwitchableTimeFormatter date={new Date(start)}/>
                            </Typography>
                        </Box>
                        <Box sx={{display: 'flex', gap: 1, alignItems: 'center'}}>
                            <ToggleButtonGroup onChange={changeMode} value={timelineMode} exclusive size="small" sx={{maxHeight: "32px"}}>
                                <ToggleButton value="compact" sx={{fontSize: "12px"}}>Compact</ToggleButton>
                                <ToggleButton value="detailed" sx={{fontSize: "12px"}}>Detailed</ToggleButton>
                            </ToggleButtonGroup>
                            <ToggleButtonGroup onChange={changeCompression} value={compressionMode} exclusive size="small" sx={{maxHeight: "32px"}}>
                                <ToggleButton value="actual" sx={{fontSize: "12px"}}>Linear</ToggleButton>
                                <ToggleButton value="compressed" sx={{fontSize: "12px"}}>Compressed</ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                    </Box>

                    <Box sx={{display: 'grid', gridTemplateColumns: GANTT_COLUMNS, position: 'relative'}}>
                        <Box sx={{
                            display: 'grid',
                            gridTemplateColumns: 'subgrid',
                            gridColumn: '1 / -1',
                            alignItems: 'flex-end',
                            height: 18,
                            mb: (timelineMode === "compact" ? 2 : 1)
                        }}>
                            <Box role="gantt-row-label" sx={{maxWidth: MAX_LABEL_WIDTH, minWidth: MIN_LABEL_WIDTH, pr: 1}}/>
                            <Box sx={{position: 'relative', height: 18}}>
                                {ticks.map((t) => (
                                    <Typography key={t.ms} variant="caption" sx={{
                                        position: 'absolute',
                                        top: 0,
                                        left: `${reverse ? 100 - t.pct : t.pct}%`,
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
                             sx={{position: 'absolute', top: 26, bottom: 0, gridColumn: '2 / 3', width: '100%', pointerEvents: 'none', zIndex: 0,}}>
                            {ticks.map((t) => (
                                <Box key={t.ms + "-divider"} sx={{
                                    position: 'absolute',
                                    top: 0,
                                    bottom: 0,
                                    left: `${reverse ? 100 - t.pct : t.pct}%`,
                                    borderLeft: '1px solid',
                                    borderColor: 'divider',
                                    opacity: 0.6,
                                }}/>
                            ))}
                            {timelineMode === "compact" && compactRetryEvents.map((retry) => (
                                <Box key={retry.count} sx={{
                                    position: 'absolute', top: 0, bottom: 0, left: `${reverse ? 100 - retry.pct : retry.pct}%`,
                                    borderLeft: '1px dashed', borderColor: 'divider', opacity: 0.6,
                                }}>
                                    <Typography variant="caption" sx={{
                                        position: 'absolute', top: -7, left: '50%', transform: 'translateX(-50%)',
                                        fontWeight: 600, fontSize: '10px', whiteSpace: 'nowrap', bgcolor: 'background.paper', px: 0.5,
                                    }}>
                                        Retry {retry.count}
                                    </Typography>
                                </Box>
                            ))}
                        </Box>

                        {/* COMPACT MODE */}
                        {timelineMode === "compact" && compactRows.map((row) => (
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
                        {timelineMode === "detailed" && orderedDetailedRows.map(({step, isRetry, retryNumber}, index) => {
                            const info = stepEndMap.get(step);
                            const stepStartMs = asMs(step.barStart ?? step.createdAt);
                            const stepEndMs = info?.end ?? null;
                            const active = info?.active ?? false;
                            const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(stepStartMs, stepEndMs, step.state);
                            const item = {...step, stepStartMs, stepEndMs, active};

                            return (
                                <Fragment key={index}>
                                    {isRetry && <RetrySeparator label={`Retry ${retryNumber}`}/>}
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

                    <Typography variant="caption" align="right" component="p" sx={{opacity: 0.8}} color="text.secondary">
                        Monitor job progress along a visual timeline. When leveraging <a
                        target="_blank" href="https://www.jobrunr.io/en/guides/advanced/durable-executions/"> durable executions</a>,
                        you can inspect how long each individual step takes to complete.
                    </Typography>
                </CardContent>
            </Card>
        </Box>
    );
};