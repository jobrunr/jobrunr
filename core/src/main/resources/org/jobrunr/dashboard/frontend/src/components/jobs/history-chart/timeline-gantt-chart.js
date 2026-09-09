import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import {useTheme} from "@mui/material";
import {Rhombus, RhombusOutline} from "mdi-material-ui";
import {Fragment} from 'react';
import {Legend} from "./gantt-components/legend.js";
import {formatDuration} from "../../../utils/helper-functions.js";
import {GanttBar, getBarColor} from "./gantt-components/gantt-bar.js";
import {BreakIndicator} from "./gantt-components/break-indicator.js";
import {RetrySeparator} from "./gantt-components/retry-separator.js";
import {FAILED} from "../../utils/state-names.js";
import {TIMELINE_MODES} from "./job-history-chart.js";
import {GanttTooltipTitle} from "./gantt-components/gantt-tooltip-title.js";
import {GanttRow} from "./gantt-components/gantt-row.js";
import {GanttRowDuration} from "./gantt-components/gantt-row-duration.js";
import {GanttTimelineEntry} from "./gantt-components/gantt-timeline-entry.js";

export const MIN_LABEL_WIDTH = 150;
export const MAX_LABEL_WIDTH = 250;
export const ROW_HEIGHT = 28;
const GANTT_COLUMNS = 'minmax(0, max-content) 1fr 90px';

const drawBarOrShapeOnRow = (item, theme, reverse) => {
    const {offset, width, isPoint, isCompressed, breakOffsets} = item.placement;
    return (
        <Tooltip title={<GanttTooltipTitle item={item}/>}>
            {item.isSkipped ? (
                <RhombusOutline fontSize="tiny"
                                sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)', color: 'grey.500'}}/>
            ) : isPoint ? (
                <Rhombus fontSize="tiny" color={item.succeeded === false || item.state === FAILED ? 'error' : 'success'}
                         sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)'}}/>
            ) : (
                <Box sx={{position: 'absolute', left: `${offset}%`, width: `${width}%`, top: 0, bottom: 0, display: 'flex', alignItems: 'center'}}>
                    <GanttBar active={item.active} variant={item.active ? 'indeterminate' : 'determinate'} value={item.active ? undefined : 100} step={item}/>
                    {isCompressed && breakOffsets.map((bOffset, bIdx) => <BreakIndicator key={bIdx} leftPct={bOffset} color={getBarColor(item, theme)}/>)}
                    {item.outcome && (
                        <Rhombus fontSize="tiny" color={item.outcome === FAILED ? 'error' : 'success'}
                                 sx={{position: 'absolute', [reverse ? 'left' : 'right']: -6, top: '50%', transform: 'translateY(-50%)', zIndex: 2}}/>
                    )}
                </Box>
            )}
        </Tooltip>
    );
};

function ganttChartCompactModeRow(row, theme, reverse) {
    return <GanttRow key={row.key} label={row.label} isStep={row.isStep}>
        <Box sx={{position: 'relative', height: 18}}>
            {row.items.map((item, idx) => (
                <Fragment key={row.key + idx}>{drawBarOrShapeOnRow(item, theme, reverse)}</Fragment>
            ))}
        </Box>
        <GanttRowDuration>
            {formatDuration(0, row.totalMs)}
        </GanttRowDuration>
    </GanttRow>
}

function ganttChartDetailedModeRow(isRetry, retryNumber, label, isStep, item, theme, reverse) {
    return <Fragment key={item.state + item.createdAt + label}>
        {isRetry && <RetrySeparator label={`Retry ${retryNumber}`} vertical={false}/>}
        <GanttRow label={label} isStep={isStep}>
            <Box sx={{position: 'relative', height: 18}}>
                {drawBarOrShapeOnRow(item, theme, reverse)}
            </Box>
            {!item.active && (
                <GanttRowDuration>
                    {item.isSkipped ? 'skipped' : formatDuration(item.startMs, item.endMs)}
                </GanttRowDuration>
            )}
        </GanttRow>
    </Fragment>;
}

export const TimelineGanttChart = ({model, timelineMode, reverse = false}) => {
    const theme = useTheme();
    const {ticks, retryEvents, compactRows, orderedDetailedRows} = model;
    const pos = (pct) => reverse ? 100 - pct : pct;

    return (
        <>
            <Box sx={{display: 'grid', gridTemplateColumns: GANTT_COLUMNS, position: 'relative'}}>
                {/* Top Row */}
                <Box sx={{
                    display: 'grid',
                    gridTemplateColumns: 'subgrid',
                    gridColumn: '1 / -1',
                    alignItems: 'flex-end',
                    height: 18,
                    mb: (timelineMode === TIMELINE_MODES.COMPACT ? 2 : 1)
                }}>
                    <Box sx={{maxWidth: MAX_LABEL_WIDTH, minWidth: MIN_LABEL_WIDTH, pr: 1}}/>
                    <Box sx={{position: 'relative', height: 18}}>
                        {ticks.map((t) => <GanttTimelineEntry time={t} position={pos} key={t.ms}/>)}
                    </Box>
                    <div/>
                </Box>

                {/* Vertical Dividers */}
                <Box aria-hidden="true"
                     sx={{position: 'absolute', top: 26, bottom: 0, gridColumn: '2 / 3', width: '100%', pointerEvents: 'none', zIndex: 0,}}>
                    {ticks.map((t) => (
                        <Box key={t.ms} sx={{
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: `${pos(t.pct)}%`,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            opacity: 0.6,
                        }}/>
                    ))}
                    {timelineMode === TIMELINE_MODES.COMPACT && retryEvents.map((retry) => (
                        <RetrySeparator key={retry.count} label={`Retry ${retry.count}`} position={pos(retry.pct)} vertical={true}/>
                    ))}
                </Box>

                {/* Gantt Rows */}
                {timelineMode === TIMELINE_MODES.COMPACT && compactRows.map((row) => ganttChartCompactModeRow(row, theme, reverse))}
                {timelineMode === TIMELINE_MODES.DETAILED &&
                    orderedDetailedRows.map(({item, label, isStep, isRetry, retryNumber}) => ganttChartDetailedModeRow(
                        isRetry, retryNumber, label, isStep, item, theme, reverse)
                    )
                }
            </Box>

            <Legend/>

            <Typography variant="caption" align="right" component="p" sx={{opacity: 0.8}} color="text.secondary">
                Monitor job progress along a visual timeline. When leveraging <a
                target="_blank" href="https://www.jobrunr.io/en/guides/advanced/durable-executions/"> durable executions</a>,
                you can inspect how long each individual step takes to complete.
            </Typography>
        </>
    );
};
