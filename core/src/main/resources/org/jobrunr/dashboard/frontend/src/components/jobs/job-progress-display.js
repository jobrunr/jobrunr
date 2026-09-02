import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import {alpha, keyframes, styled} from '@mui/material/styles';
import {Fragment, useEffect, useMemo, useState} from 'react';
import {timelineViewModes, useTimelineViewMode} from '../../hooks/useTimelineViewMode.js';
import {
    buildJobTimeline,
    formatDateTime,
    formatDuration,
    formatOffset,
    getTicks,
    STATES,
    STEP,
    toMicros
} from './job-timeline.js';

const REFRESH_INTERVAL_IN_MS = 1000;
const MIN_BAR_WIDTH_IN_PX = 3;
const MARKER_SIZE = 9;

const STATE_LABELS = {
    [STATES.AWAITING]: 'Awaiting',
    [STATES.SCHEDULED]: 'Scheduled',
    [STATES.ENQUEUED]: 'Enqueued',
    [STATES.PROCESSING]: 'Processing',
    [STATES.SUCCEEDED]: 'Succeeded',
    [STATES.FAILED]: 'Failed',
    [STATES.DELETED]: 'Deleted',
    [STEP]: 'Step',
};

const getSpanColor = (theme, span) => {
    if (span.type === STEP) {
        if (span.isRunning) return theme.palette.warning.main;
        if (span.succeeded === false) return theme.palette.error.main;
        if (span.succeeded === undefined || span.hasUnknownEnd) return theme.palette.grey[500];
        return theme.palette.success.main;
    }
    switch (span.type) {
        case STATES.AWAITING:
            return theme.palette.grey[500];
        case STATES.SCHEDULED:
            return theme.palette.grey[600];
        case STATES.ENQUEUED:
            return theme.palette.info.main;
        case STATES.PROCESSING:
            return theme.palette.warning.main;
        case STATES.SUCCEEDED:
            return theme.palette.success.main;
        case STATES.FAILED:
            return theme.palette.error.main;
        case STATES.DELETED:
            return theme.palette.grey[700];
        default:
            return theme.palette.info.main;
    }
};

const getSpanLabel = (span) => (span.type === STEP ? span.label : STATE_LABELS[span.type] ?? span.label);

const getSpanDuration = (span) => {
    if (span.isMoment) return '';
    if (span.hasUnknownEnd) return '?';
    return formatDuration(span.end - span.start);
};

const getSpanDescription = (span) => {
    const {jobState} = span;
    switch (span.type) {
        case STEP:
            if (span.isRunning) return 'Step is running';
            if (span.hasUnknownEnd) return 'Step did not report an end time (was the job server interrupted?)';
            if (span.succeeded === false) return 'Step failed and will run again on the next retry';
            if (span.succeeded === undefined) return 'Step outcome unknown';
            return 'Step completed successfully and is skipped on a retry';
        case STATES.AWAITING:
            return jobState?.reason ?? 'Job is awaiting an optimal low-carbon execution window';
        case STATES.SCHEDULED:
            return jobState?.reason ?? 'Job is waiting to be enqueued';
        case STATES.ENQUEUED:
            return 'Job is waiting for a free worker';
        case STATES.PROCESSING:
            return `Processing on server ${jobState?.serverName ?? jobState?.serverId ?? 'unknown'}`;
        case STATES.SUCCEEDED:
            return 'Job processing succeeded';
        case STATES.FAILED:
            return jobState?.message ?? 'Job processing failed';
        case STATES.DELETED:
            return jobState?.reason ?? 'Job was deleted';
        default:
            return '';
    }
};

const getSpanSummary = (span, attempts) => {
    const attempt = attempts > 1 && span.attempt ? `Attempt ${span.attempt} · ` : '';
    const period = span.isMoment
        ? `at ${formatDateTime(span.start)}`
        : `from ${formatDateTime(span.start)} to ${span.isRunning ? 'now' : formatDateTime(span.end)}`;
    const duration = getSpanDuration(span);
    return `${attempt}${getSpanLabel(span)}: ${period}${duration ? ` (${duration})` : ''}`;
};

const getLaneSummary = (lane) => {
    if (lane.didNotRun) return `${lane.label} · did not run`;
    if (!(lane.duration > 0)) return lane.label;
    if (lane.count > 1) return `${lane.label} · ${lane.count} spans · ${formatDuration(lane.duration)} in total`;
    return `${lane.label} · ${formatDuration(lane.duration)}`;
};

const getMarkerSummary = (marker) =>
    `${marker.stepName} skipped on attempt ${marker.attempt}: already completed during attempt ${marker.completedDuringAttempt}`;

const SpanTooltip = ({span, attempts}) => (
    <Box sx={{display: 'grid', gridTemplateColumns: 'auto auto', columnGap: 1, rowGap: 0.25}}>
        <Box sx={{gridColumn: '1 / -1', fontWeight: 600}}>
            {getSpanLabel(span)}{attempts > 1 && span.attempt ? ` · attempt ${span.attempt} of ${attempts}` : ''}
        </Box>
        <Box sx={{gridColumn: '1 / -1', opacity: 0.8}}>{getSpanDescription(span)}</Box>
        <Box>Start</Box>
        <Box>{formatDateTime(span.start)}</Box>
        {!span.isMoment && <>
            <Box>{span.isRunning ? 'Now' : 'End'}</Box>
            <Box>{span.hasUnknownEnd ? 'unknown' : formatDateTime(span.end)}</Box>
            <Box>Duration</Box>
            <Box>{getSpanDuration(span)}{span.isRunning ? ' and counting' : ''}</Box>
        </>}
        {span.type === STATES.PROCESSING && span.isRunning && span.jobState?.updatedAt && <>
            <Box>Last sign of life</Box>
            <Box>{formatDateTime(toMicros(span.jobState.updatedAt))}</Box>
        </>}
        {span.type === STATES.SCHEDULED && span.plannedEnd && <>
            <Box>Scheduled at</Box>
            <Box>{formatDateTime(span.plannedEnd)}</Box>
        </>}
        {span.type === STATES.FAILED && span.jobState?.exceptionType && <>
            <Box>Exception</Box>
            <Box>{span.jobState.exceptionType}</Box>
        </>}
        {span.type === STEP && span.result !== undefined && <>
            <Box>Result</Box>
            <Box sx={{wordBreak: 'break-all'}}>{String(span.result)}</Box>
        </>}
    </Box>
);

const MarkerTooltip = ({marker}) => (
    <Box>
        <Box sx={{fontWeight: 600}}>Skipped on attempt {marker.attempt}</Box>
        <Box sx={{opacity: 0.8}}>
            {marker.stepName} already completed during attempt {marker.completedDuringAttempt} and did not run again
        </Box>
    </Box>
);

const movingStripes = keyframes`
    from {
        background-position: 0 0;
    }
    to {
        background-position: 28px 0;
    }
`;

const Bar = styled('div', {
    shouldForwardProp: (prop) => !['span', 'barHeight'].includes(prop),
})(({theme, span, barHeight}) => {
    const color = getSpanColor(theme, span);
    return {
        position: 'absolute',
        top: '50%',
        transform: 'translateY(-50%)',
        minWidth: MIN_BAR_WIDTH_IN_PX,
        height: barHeight,
        borderRadius: 4,
        backgroundColor: color,
        boxSizing: 'border-box',
        ...(span.isRunning && {
            backgroundImage: `repeating-linear-gradient(45deg, ${color}, ${color} 7px, ${alpha(color, 0.45)} 7px, ${alpha(color, 0.45)} 14px)`,
            backgroundSize: '28px 28px',
            animation: `${movingStripes} 1s linear infinite`,
            '@media (prefers-reduced-motion: reduce)': {animation: 'none'},
        }),
        ...(span.hasUnknownEnd && {
            backgroundImage: `linear-gradient(to right, ${color}, ${alpha(color, 0)})`,
        }),
    };
});

const Moment = styled('div', {
    shouldForwardProp: (prop) => prop !== 'span',
})(({theme, span}) => ({
    position: 'absolute',
    top: '50%',
    width: MARKER_SIZE,
    height: MARKER_SIZE,
    transform: 'translate(-50%, -50%) rotate(45deg)',
    backgroundColor: getSpanColor(theme, span),
    border: `1px solid ${theme.palette.background.paper}`,
}));

/** A step that did not run again because it already completed during an earlier attempt. */
const SkippedMarker = styled('div')(({theme}) => ({
    position: 'absolute',
    top: '50%',
    width: MARKER_SIZE,
    height: MARKER_SIZE,
    transform: 'translate(-50%, -50%) rotate(45deg)',
    backgroundColor: theme.palette.background.paper,
    border: `1.5px solid ${theme.palette.grey[500]}`,
}));

const TimelineLane = ({lane, timeline}) => {
    const position = (micros) => ((micros - timeline.start) / timeline.duration) * 100;
    const barHeight = lane.isStepLane ? 8 : 12;

    return (
        <Box role="row" sx={{display: 'flex', alignItems: 'center', height: lane.isStepLane ? 22 : 26}}>
            <Box role="rowheader"
                 sx={{
                     width: 'var(--jr-label-width)',
                     flexShrink: 0,
                     pr: 1,
                     pl: lane.isStepLane ? 2 : 0,
                     boxSizing: 'border-box',
                 }}>
                <Tooltip title={getLaneSummary(lane)}>
                    <Typography noWrap variant={lane.isStepLane ? 'caption' : 'body2'} component="div"
                                sx={{color: lane.didNotRun ? 'text.disabled' : lane.isStepLane ? 'text.secondary' : 'text.primary'}}>
                        {lane.isStepLane && <Box component="span" sx={{opacity: 0.6, mr: 0.5}}>└</Box>}
                        {lane.label}
                    </Typography>
                </Tooltip>
            </Box>

            <Box role="cell" sx={{position: 'relative', flexGrow: 1, height: '100%'}}>
                {lane.spans.map((span) => (
                    <Tooltip key={span.key} title={<SpanTooltip span={span} attempts={timeline.attempts}/>} placement="top" followCursor>
                        {span.isMoment
                            ? <Moment span={span} role="img" aria-label={getSpanSummary(span, timeline.attempts)}
                                      style={{left: `${position(span.start)}%`}}/>
                            : <Bar span={span} barHeight={barHeight} role="img" aria-label={getSpanSummary(span, timeline.attempts)}
                                   style={{left: `${position(span.start)}%`, width: `${position(span.end) - position(span.start)}%`}}/>}
                    </Tooltip>
                ))}
                {lane.markers.map((marker) => (
                    <Tooltip key={marker.key} title={<MarkerTooltip marker={marker}/>} placement="top" followCursor>
                        <SkippedMarker role="img" aria-label={getMarkerSummary(marker)} style={{left: `${position(marker.at)}%`}}/>
                    </Tooltip>
                ))}
            </Box>

            <Box role="cell"
                 sx={{
                     width: 'var(--jr-duration-width)',
                     flexShrink: 0,
                     pl: 1,
                     textAlign: 'right',
                     display: 'var(--jr-duration-display)',
                     boxSizing: 'border-box',
                 }}>
                <Typography noWrap variant="caption" component="div"
                            sx={{color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
                    {lane.duration > 0 ? formatDuration(lane.duration) : ''}
                </Typography>
            </Box>
        </Box>
    );
};

const AttemptSeparator = ({attempt}) => (
    <Box aria-hidden="true" sx={{display: 'flex', alignItems: 'center', gap: 1, mt: 1, mb: 0.5}}>
        <Typography variant="caption" sx={{color: 'text.secondary', flexShrink: 0}}>Attempt {attempt}</Typography>
        <Box sx={{flexGrow: 1, borderTop: '1px dashed', borderColor: 'divider'}}/>
    </Box>
);

const LEGEND_STATES = [STATES.AWAITING, STATES.SCHEDULED, STATES.ENQUEUED, STATES.PROCESSING, STATES.SUCCEEDED, STATES.FAILED, STATES.DELETED];
const LEGEND_STEPS = [
    {key: 'step-succeeded', label: 'Step succeeded', span: {type: STEP, succeeded: true}, matches: (span) => span.succeeded === true && !span.isRunning},
    {key: 'step-failed', label: 'Step failed', span: {type: STEP, succeeded: false}, matches: (span) => span.succeeded === false},
    {key: 'step-running', label: 'Step running', span: {type: STEP, isRunning: true}, matches: (span) => span.isRunning},
    {key: 'step-unknown', label: 'Step outcome unknown', span: {type: STEP}, matches: (span) => span.succeeded === undefined || span.hasUnknownEnd},
];

const buildLegend = (lanes) => {
    const spans = lanes.flatMap((lane) => lane.spans);
    const entries = LEGEND_STATES
        .filter((type) => spans.some((span) => span.type === type))
        .map((type) => ({key: type, label: STATE_LABELS[type], span: {type}}));

    LEGEND_STEPS
        .filter((entry) => spans.some((span) => span.type === STEP && entry.matches(span)))
        .forEach((entry) => entries.push(entry));

    if (lanes.some((lane) => lane.markers.length > 0)) {
        entries.push({key: 'step-skipped', label: 'Step skipped', outlined: true});
    }
    return entries;
};

const Legend = ({lanes}) => (
    <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 1.5}}>
        {buildLegend(lanes).map((entry) => (
            <Box key={entry.key} sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                <Box sx={(theme) => ({
                    width: 10,
                    height: 10,
                    borderRadius: '2px',
                    boxSizing: 'border-box',
                    ...(entry.outlined
                        ? {border: `1.5px solid ${theme.palette.grey[500]}`}
                        : {backgroundColor: getSpanColor(theme, entry.span)}),
                })}/>
                <Typography variant="caption" sx={{color: 'text.secondary'}}>{entry.label}</Typography>
            </Box>
        ))}
    </Box>
);

export const JobProgressDisplay = ({job}) => {
    const [viewMode, setViewMode] = useTimelineViewMode();
    const [now, setNow] = useState(() => Date.now());
    const timeline = useMemo(() => buildJobTimeline(job, now), [job, now]);
    const isRunning = timeline?.isRunning ?? false;
    const isCompact = viewMode === timelineViewModes.compact;

    useEffect(() => {
        if (!isRunning) return undefined;
        const interval = setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_IN_MS);
        return () => clearInterval(interval);
    }, [isRunning]);

    if (!timeline) return null;

    const lanes = isCompact ? timeline.compactLanes : timeline.detailedLanes;
    const ticks = getTicks(timeline.duration);
    const nowPosition = ((timeline.now - timeline.start) / timeline.duration) * 100;

    return (
        <Card sx={{
            width: '100%',
            '--jr-label-width': {xs: '110px', sm: '180px', md: '220px'},
            '--jr-duration-width': {xs: '0px', sm: '80px'},
            '--jr-duration-display': {xs: 'none', sm: 'block'},
        }}>
            <CardContent>
                <Box sx={{display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap'}}>
                    <Box>
                        <Typography variant="h5" component="h2">Execution Timeline</Typography>
                        <Typography variant="body2" sx={{color: 'text.secondary'}}>
                            Started at {formatDateTime(timeline.start)} · {formatDuration(timeline.duration)}
                            {isRunning ? ' and counting' : ''}
                            {timeline.attempts > 1 && ` · ${timeline.attempts} attempts`}
                        </Typography>
                    </Box>
                    <ToggleButtonGroup exclusive size="small" value={viewMode} aria-label="Execution timeline view"
                                       onChange={(event, mode) => mode && setViewMode(mode)}>
                        <ToggleButton value={timelineViewModes.compact}>Compact</ToggleButton>
                        <ToggleButton value={timelineViewModes.detailed}>Detailed</ToggleButton>
                    </ToggleButtonGroup>
                </Box>

                <Box role="table" aria-label="Job execution timeline" sx={{position: 'relative', mt: 2}}>
                    <Box sx={{display: 'flex', alignItems: 'flex-end', height: 20}}>
                        <Box sx={{width: 'var(--jr-label-width)', flexShrink: 0}}/>
                        <Box sx={{position: 'relative', flexGrow: 1, height: '100%'}}>
                            {ticks.map((tick, index) => (
                                <Typography key={tick.offset} variant="caption" component="div"
                                            sx={{
                                                position: 'absolute',
                                                bottom: 0,
                                                left: `${tick.position}%`,
                                                // keep the first and last label within the bounds of the timeline
                                                transform: index === 0 ? 'none' : tick.position > 95 ? 'translateX(-100%)' : 'translateX(-50%)',
                                                color: 'text.secondary',
                                                whiteSpace: 'nowrap',
                                            }}>
                                    {formatOffset(tick.offset)}
                                </Typography>
                            ))}
                        </Box>
                        <Box sx={{width: 'var(--jr-duration-width)', flexShrink: 0, display: 'var(--jr-duration-display)'}}/>
                    </Box>

                    <Box sx={{position: 'relative'}}>
                        <Box aria-hidden="true"
                             sx={{
                                 position: 'absolute',
                                 top: 0,
                                 bottom: 0,
                                 left: 'var(--jr-label-width)',
                                 right: 'var(--jr-duration-width)',
                                 pointerEvents: 'none',
                             }}>
                            {ticks.map((tick) => (
                                <Box key={tick.offset}
                                     sx={{
                                         position: 'absolute',
                                         top: 0,
                                         bottom: 0,
                                         left: `${tick.position}%`,
                                         borderLeft: '1px solid',
                                         borderColor: 'divider',
                                         opacity: 0.6,
                                     }}/>
                            ))}
                            {nowPosition > 0.5 && nowPosition < 99.5 &&
                                <Box sx={{
                                    position: 'absolute',
                                    top: 0,
                                    bottom: 0,
                                    left: `${nowPosition}%`,
                                    borderLeft: '1px dashed',
                                    borderColor: 'warning.main',
                                }}/>
                            }
                        </Box>

                        {lanes.map((lane) => (
                            <Fragment key={lane.key}>
                                {lane.startsNewAttempt && timeline.attempts > 1 && <AttemptSeparator attempt={lane.attempt}/>}
                                <TimelineLane lane={lane} timeline={timeline}/>
                            </Fragment>
                        ))}
                    </Box>
                </Box>

                <Legend lanes={lanes}/>
            </CardContent>
        </Card>
    );
};
