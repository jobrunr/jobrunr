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
import {HorizontalRetrySeparator, VerticalRetrySeparator} from "./gantt-components/retry-separator.js";
import {FAILED} from "../../utils/state-names.js";
import {GanttTooltipTitle} from "./gantt-components/gantt-tooltip-title.js";
import {GanttRow} from "./gantt-components/gantt-row.js";
import {GanttRowDuration} from "./gantt-components/gantt-row-duration.js";
import {GanttTimelineTickLabel} from "./gantt-components/gantt-timeline-tick-label.js";
import {GanttRowLabel} from "./gantt-components/gantt-row-label.js";
import {styled} from "@mui/material/styles";
import {TIMELINE_MODES} from "./timeline-data-components/timeline-data.js";

export const MIN_LABEL_WIDTH = 150;
export const MAX_LABEL_WIDTH = 250;
export const ROW_HEIGHT = 28;

const renderBarOrMilestone = (item, theme, reverse) => {
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

const GanttRowTimeline = styled("div")({
    position: 'relative',
    height: 18
});

function renderCompactGanttChartRow(row, theme, reverse) {
    return <GanttRow key={row.key} label={row.label}>
        <GanttRowLabel label={row.label} isStep={row.isStep}/>
        <GanttRowTimeline>
            {row.items.map((item, idx) => (
                <Fragment key={row.key + idx}>{renderBarOrMilestone(item, theme, reverse)}</Fragment>
            ))}
        </GanttRowTimeline>
        <GanttRowDuration duration={formatDuration(0, row.totalMs)}/>
    </GanttRow>
}

function renderSimpleGanttChartRow(row, theme, reverse) {
    const {item, label, isStep} = row;
    return <Fragment key={item.state + item.startedAt + label}>
        <GanttRow label={label}>
            <GanttRowLabel label={label} isStep={isStep}/>
            <GanttRowTimeline>
                {renderBarOrMilestone(item, theme, reverse)}
            </GanttRowTimeline>
            {!item.active && <GanttRowDuration duration={item.isSkipped ? 'skipped' : formatDuration(item.startMs, item.endMs)}/>}
        </GanttRow>
    </Fragment>;
}

export const TimelineGanttChart = ({model, timelineMode, reverse = false}) => {
    const theme = useTheme();
    const {ticks, retryEvents, compactRows, orderedDetailedRows} = model;
    const pos = (pct) => reverse ? 100 - pct : pct;

    return (
        <>
            <Box sx={{display: 'grid', gridTemplateColumns: 'minmax(0, max-content) 1fr 90px', position: 'relative'}}>
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
                        {ticks.map((t) => <GanttTimelineTickLabel time={t} position={pos} key={t.ms}/>)}
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
                        <VerticalRetrySeparator key={retry.count} label={retry.label} position={pos(retry.pct)}/>
                    ))}
                </Box>

                {/* Gantt Rows */}
                {timelineMode === TIMELINE_MODES.COMPACT && compactRows.map((row) => renderCompactGanttChartRow(row, theme, reverse))}
                {timelineMode === TIMELINE_MODES.DETAILED &&
                    orderedDetailedRows.map((row) => row.isSeparator
                        ? <HorizontalRetrySeparator key={`separator-${row.label}`} label={row.label}/>
                        : renderSimpleGanttChartRow(row, theme, reverse))
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
