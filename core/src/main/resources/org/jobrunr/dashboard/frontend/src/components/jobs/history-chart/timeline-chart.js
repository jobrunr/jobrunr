import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Tooltip from '@mui/material/Tooltip';
import {useTheme} from "@mui/material";
import {Rhombus, RhombusOutline} from "mdi-material-ui";
import {Fragment} from 'react';
import {END_STATES} from "./timeline-data.js";
import {SwitchableTimeFormatter} from "../../utils/time-ago.js";
import {Legend} from "./legend.js";
import {formatDuration} from "../../../utils/helper-functions.js";
import {GanttBar, getBarColor} from "./gantt-bar.js";
import {BreakIndicator} from "./break-indicator.js";
import {RetrySeparator} from "./retry-separator.js";

const MIN_LABEL_WIDTH = 150;
const MAX_LABEL_WIDTH = 250;
const ROW_HEIGHT = 28;
const GANTT_COLUMNS = 'minmax(0, max-content) 1fr 90px';

const buildTooltipTitle = (item) => {
    const {startMs, endMs, active, state, result, placement, isSkipped} = item;
    const isCompressed = placement?.isCompressed ?? false;
    const includeEnd = endMs !== null && endMs !== startMs;
    return (
        <Box>
            {!isSkipped && <Typography variant="caption" component="p">{includeEnd ? "Started" : "Completed"}: <SwitchableTimeFormatter
                date={new Date(startMs)}/></Typography>}
            {includeEnd && <Typography variant="caption" component="p">Ended: <SwitchableTimeFormatter date={new Date(endMs)}/></Typography>}
            {!END_STATES.includes(state) && (
                <Typography variant="caption" component="p">
                    {active ? "Still in progress..." : isSkipped ? "Skipped" : `Took ${includeEnd ? formatDuration(startMs, endMs) : "<1 ms"}`}{isCompressed && " (visually shortened)"}
                </Typography>
            )}
            {result && <Typography variant="caption" component="p">Result: {result}</Typography>}
        </Box>
    );
};

const renderBarOrCircle = (item, theme, reverse) => {
    const {offset, width, isPoint, isCompressed, breakOffsets} = item.placement;
    return (
        <Tooltip title={buildTooltipTitle(item)}>
            {item.isSkipped ? (
                <RhombusOutline fontSize="tiny"
                                sx={{position: 'absolute', left: `${offset}%`, top: '50%', transform: 'translate(-50%, -50%)', color: 'grey.500'}}/>
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
};

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

export const TimelineChart = ({model, timelineMode, reverse = false}) => {
    const theme = useTheme();
    const {ticks, retryEvents, compactRows, orderedDetailedRows} = model;
    const pos = (pct) => reverse ? 100 - pct : pct;

    return (
        <>
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
                            <Tooltip title={<>
                                <SwitchableTimeFormatter date={new Date(t.startMs)}/>
                                {t.endMs && <> - <SwitchableTimeFormatter date={new Date(t.endMs)}/></>}
                            </>}>
                                <Typography key={t.ms} variant="caption" sx={{
                                    position: 'absolute',
                                    top: 0,
                                    left: `${pos(t.pct)}%`,
                                    transform: 'translateX(-50%)',
                                    whiteSpace: 'nowrap',
                                    color: 'text.secondary'
                                }}>
                                    {t.label}
                                </Typography>
                            </Tooltip>
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
                            left: `${pos(t.pct)}%`,
                            borderLeft: '1px solid',
                            borderColor: 'divider',
                            opacity: 0.6,
                        }}/>
                    ))}
                    {timelineMode === "compact" && retryEvents.map((retry) => (
                        <Box key={retry.count} sx={{
                            position: 'absolute', top: 0, bottom: 0, left: `${pos(retry.pct)}%`,
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
                            {row.items.map((item, idx) => (
                                <Fragment key={idx}>{renderBarOrCircle(item, theme, reverse)}</Fragment>
                            ))}
                        </Box>
                        <Box>
                            <Typography sx={{fontSize: '11px', textAlign: 'right', color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
                                {formatDuration(0, row.totalMs)}
                            </Typography>
                        </Box>
                    </Box>
                ))}

                {timelineMode === "detailed" && orderedDetailedRows.map(({item, label, isStep, isRetry, retryNumber}, index) => (
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
                            {renderRowLabel(label, isStep)}
                            <Box sx={{position: 'relative', height: 18}}>
                                {renderBarOrCircle(item, theme, reverse)}
                            </Box>
                            {!item.active && (
                                <Box>
                                    <Typography
                                        sx={{fontSize: '11px', textAlign: 'right', color: 'text.secondary', fontVariantNumeric: 'tabular-nums'}}>
                                        {item.isSkipped ? 'skipped' : formatDuration(item.startMs, item.endMs)}
                                    </Typography>
                                </Box>
                            )}
                        </Box>
                    </Fragment>
                ))}
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
