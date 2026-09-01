import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import {alpha, keyframes, styled} from '@mui/material/styles';
import {Fragment, useEffect, useMemo, useState} from 'react';
import {
    buildJobTimeline,
    formatDateTime,
    formatDuration,
    formatOffset,
    getTicks,
    STATES,
    STEP_ROW,
    toMicros
} from './job-timeline.js';

const REFRESH_INTERVAL_IN_MS = 1000;
const MIN_BAR_WIDTH_IN_PX = 3;
const STATE_ROW_HEIGHT = 26;
const STEP_ROW_HEIGHT = 22;
const STATE_BAR_HEIGHT = 12;
const STEP_BAR_HEIGHT = 8;

const LEGEND_LABELS = {
    [STATES.AWAITING]: 'Awaiting',
    [STATES.SCHEDULED]: 'Scheduled',
    [STATES.ENQUEUED]: 'Enqueued',
    [STATES.PROCESSING]: 'Processing',
    [STATES.SUCCEEDED]: 'Succeeded',
    [STATES.FAILED]: 'Failed',
    [STATES.DELETED]: 'Deleted',
    [STEP_ROW]: 'Step (runStepOnce)',
};

const getRowColor = (theme, row) => {
    if (row.type === STEP_ROW) {
        if (row.isRunning) return theme.palette.warning.main;
        if (row.succeeded === false) return theme.palette.error.main;
        if (row.succeeded === undefined || row.hasUnknownEnd) return theme.palette.grey[500];
        return theme.palette.success.main;
    }
    switch (row.type) {
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

const getRowLabel = (row) => (row.type === STEP_ROW ? row.label : LEGEND_LABELS[row.type] ?? row.label);

const getDurationOf = (row) => {
    if (row.isMoment) return '';
    if (row.hasUnknownEnd) return '?';
    return formatDuration(row.end - row.start);
};

const getRowDescription = (row) => {
    const {jobState} = row;
    switch (row.type) {
        case STEP_ROW:
            if (row.isRunning) return 'Step is running';
            if (row.hasUnknownEnd) return 'Step did not report an end time (was the job server interrupted?)';
            if (row.succeeded === false) return 'Step failed and will run again on the next retry';
            if (row.succeeded === undefined) return 'Step outcome unknown';
            return 'Step completed successfully and will be skipped on a retry';
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

const getRowSummary = (row, attempts) => {
    const attempt = attempts > 1 && row.attempt ? `Attempt ${row.attempt} · ` : '';
    const period = row.isMoment
        ? `at ${formatDateTime(row.start)}`
        : `from ${formatDateTime(row.start)} to ${row.isRunning ? 'now' : formatDateTime(row.end)}`;
    const duration = getDurationOf(row);
    return `${attempt}${getRowLabel(row)}: ${period}${duration ? ` (${duration})` : ''}`;
};

const RowTooltip = ({row}) => (
    <Box sx={{display: 'grid', gridTemplateColumns: 'auto auto', columnGap: 1, rowGap: 0.25}}>
        <Box sx={{gridColumn: '1 / -1', fontWeight: 600}}>{getRowLabel(row)}</Box>
        <Box sx={{gridColumn: '1 / -1', opacity: 0.8}}>{getRowDescription(row)}</Box>
        <Box>Start</Box>
        <Box>{formatDateTime(row.start)}</Box>
        {!row.isMoment && <>
            <Box>{row.isRunning ? 'Now' : 'End'}</Box>
            <Box>{row.hasUnknownEnd ? 'unknown' : formatDateTime(row.end)}</Box>
            <Box>Duration</Box>
            <Box>{getDurationOf(row)}{row.isRunning ? ' and counting' : ''}</Box>
        </>}
        {row.type === STATES.PROCESSING && row.isRunning && row.jobState?.updatedAt && <>
            <Box>Last sign of life</Box>
            <Box>{formatDateTime(toMicros(row.jobState.updatedAt))}</Box>
        </>}
        {row.type === STATES.SCHEDULED && row.plannedEnd && <>
            <Box>Scheduled at</Box>
            <Box>{formatDateTime(row.plannedEnd)}</Box>
        </>}
        {row.type === STATES.FAILED && row.jobState?.exceptionType && <>
            <Box>Exception</Box>
            <Box>{row.jobState.exceptionType}</Box>
        </>}
        {row.type === STEP_ROW && row.result !== undefined && <>
            <Box>Result</Box>
            <Box sx={{wordBreak: 'break-all'}}>{String(row.result)}</Box>
        </>}
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
    shouldForwardProp: (prop) => prop !== 'row',
})(({theme, row}) => {
    const color = getRowColor(theme, row);
    return {
        position: 'absolute',
        top: '50%',
        transform: 'translateY(-50%)',
        minWidth: MIN_BAR_WIDTH_IN_PX,
        height: row.type === STEP_ROW ? STEP_BAR_HEIGHT : STATE_BAR_HEIGHT,
        borderRadius: 4,
        backgroundColor: color,
        ...(row.isRunning && {
            backgroundImage: `repeating-linear-gradient(45deg, ${color}, ${color} 7px, ${alpha(color, 0.45)} 7px, ${alpha(color, 0.45)} 14px)`,
            backgroundSize: '28px 28px',
            animation: `${movingStripes} 1s linear infinite`,
            '@media (prefers-reduced-motion: reduce)': {animation: 'none'},
        }),
        ...(row.hasUnknownEnd && {
            backgroundImage: `linear-gradient(to right, ${color}, ${alpha(color, 0)})`,
        }),
    };
});

const Moment = styled('div', {
    shouldForwardProp: (prop) => prop !== 'row',
})(({theme, row}) => ({
    position: 'absolute',
    top: '50%',
    width: 10,
    height: 10,
    transform: 'translate(-50%, -50%) rotate(45deg)',
    backgroundColor: getRowColor(theme, row),
    border: `1px solid ${theme.palette.background.paper}`,
}));

const TimelineRow = ({row, timeline}) => {
    const offset = ((row.start - timeline.start) / timeline.duration) * 100;
    const width = ((row.end - row.start) / timeline.duration) * 100;
    const isStep = row.type === STEP_ROW;

    return (
        <Box role="row" sx={{display: 'flex', alignItems: 'center', height: isStep ? STEP_ROW_HEIGHT : STATE_ROW_HEIGHT}}>
            <Box role="rowheader"
                 sx={{
                     width: 'var(--jr-label-width)',
                     flexShrink: 0,
                     pr: 1,
                     pl: isStep ? 2 : 0,
                     boxSizing: 'border-box',
                 }}>
                <Tooltip title={getRowLabel(row)}>
                    <Typography noWrap variant={isStep ? 'caption' : 'body2'} component="div"
                                sx={{color: isStep ? 'text.secondary' : 'text.primary'}}>
                        {isStep && <Box component="span" sx={{opacity: 0.6, mr: 0.5}}>└</Box>}
                        {getRowLabel(row)}
                    </Typography>
                </Tooltip>
            </Box>
            <Box role="cell" aria-label={getRowSummary(row, timeline.attempts)} sx={{position: 'relative', flexGrow: 1, height: '100%'}}>
                <Tooltip title={<RowTooltip row={row}/>} placement="top" followCursor>
                    {row.isMoment
                        ? <Moment row={row} style={{left: `${offset}%`}}/>
                        : <Bar row={row} style={{left: `${offset}%`, width: `${width}%`}}/>}
                </Tooltip>
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
                    {getDurationOf(row)}
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

const Legend = ({rows}) => {
    const types = [...new Set(rows.map((row) => row.type))];
    return (
        <Box sx={{display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 1.5}}>
            {types.map((type) => (
                <Box key={type} sx={{display: 'flex', alignItems: 'center', gap: 0.5}}>
                    <Box sx={(theme) => ({
                        width: 10,
                        height: 10,
                        borderRadius: '2px',
                        backgroundColor: getRowColor(theme, {type, succeeded: true}),
                    })}/>
                    <Typography variant="caption" sx={{color: 'text.secondary'}}>{LEGEND_LABELS[type] ?? type}</Typography>
                </Box>
            ))}
        </Box>
    );
};

export const JobProgressDisplay = ({job}) => {
    const [now, setNow] = useState(() => Date.now());
    const timeline = useMemo(() => buildJobTimeline(job, now), [job, now]);
    const isRunning = timeline?.isRunning ?? false;

    useEffect(() => {
        if (!isRunning) return undefined;
        const interval = setInterval(() => setNow(Date.now()), REFRESH_INTERVAL_IN_MS);
        return () => clearInterval(interval);
    }, [isRunning]);

    if (!timeline) return null;

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
                <Typography variant="h5" component="h2">Execution Timeline</Typography>
                <Typography variant="body2" sx={{color: 'text.secondary', mb: 2}}>
                    Started at {formatDateTime(timeline.start)} · {formatDuration(timeline.duration)}
                    {isRunning ? ' and counting' : ''}
                    {timeline.attempts > 1 && ` · ${timeline.attempts} attempts`}
                </Typography>

                <Box role="table" aria-label="Job execution timeline" sx={{position: 'relative'}}>
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

                        {timeline.rows.map((row) => (
                            <Fragment key={row.key}>
                                {row.startsNewAttempt && timeline.attempts > 1 && <AttemptSeparator attempt={row.attempt}/>}
                                <TimelineRow row={row} timeline={timeline}/>
                            </Fragment>
                        ))}
                    </Box>
                </Box>

                <Legend rows={timeline.rows}/>
            </CardContent>
        </Card>
    );
};
