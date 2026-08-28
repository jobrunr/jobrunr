import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import {styled} from '@mui/material/styles';
import {convertToBrowserDefaultDateStyle} from '../../utils/helper-functions';
import {useTheme} from "@mui/material";
import {RadioButtonChecked} from "@mui/icons-material";

const asMs = (date) => new Date(date).getTime();

const getNextJobHistoryStep = (steps, index) =>
    steps.find((step, i) => i > index && step.state !== 'RUN_STEP_ONCE') ?? null;

const validEndAt = (step) =>
    step.updatedAt && asMs(step.updatedAt) > asMs(step.createdAt) ? asMs(step.updatedAt) : null;

const getStepEndAt = (step, nextJobHistoryStep) => {
    if (step.state === 'RUN_STEP_ONCE') return validEndAt(step);
    if (step.state === 'SUCCEEDED' || step.state === 'FAILED') return asMs(step.createdAt);
    if (nextJobHistoryStep) return asMs(nextJobHistoryStep.createdAt);
    return validEndAt(step);
};

const getTimeline = (steps) => {
    let start = Infinity, end = -Infinity;
    const stepEnds = steps.map((step, i) => {
        const stepStart = asMs(step.createdAt);
        if (stepStart < start) start = stepStart;
        const stepEnd = getStepEndAt(step, getNextJobHistoryStep(steps, i));
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        return stepEnd;
    });
    if (end < start) end = start;
    return {start, end, stepEnds};
};

const ignoreEnqueuedAndScheduled = (executionSteps) =>
    executionSteps.filter(step => step.state !== 'ENQUEUED' && step.state !== 'SCHEDULED');

const getStepPlacement = (step, stepEndMs, start, end) => {
    const stepStart = asMs(step.createdAt);
    const span = end - start;
    const pct = (ms) => span > 0 ? (ms / span) * 100 : 0;
    const offset = pct(stepStart - start);
    if (stepEndMs === null || stepEndMs <= stepStart) return {offset, width: null, isPoint: true};
    return {offset, width: Math.max(pct(stepEndMs - stepStart), 0), isPoint: false};
};

const STEP_LABELS = {
    SCHEDULED: 'Scheduled',
    ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing',
    SUCCEEDED: 'Succeeded',
    FAILED: 'Failed',
    DELETED: 'Deleted',
    AWAITING: 'Awaiting',
    RUN_STEP_ONCE: 'Run step once',
};

const stepLabel = (step) => {
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName;
    return STEP_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const formatHumanReadableDate = (ms) => convertToBrowserDefaultDateStyle(new Date(ms));

const isInProgressStep = (step) => step.state === 'PROCESSING' || (step.state === 'RUN_STEP_ONCE' && !step.updatedAt);

const buildTooltipTitle = (step, stepEndMs) => {
    const startMs = asMs(step.createdAt);
    if (stepEndMs === null) {
        return isInProgressStep(step)
            ? `${formatHumanReadableDate(startMs)} (in progress)`
            : formatHumanReadableDate(startMs);
    }
    return `${formatHumanReadableDate(startMs)} → ${formatHumanReadableDate(stepEndMs)} (${formatDuration(startMs, stepEndMs)})`;
};

const formatAxisTime = (ms) => {
    const d = new Date(ms);
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const formatDuration = (startMs, endMs) => {
    const ms = Math.max(0, endMs - startMs);
    if (ms < 1000) return `${ms}ms`;
    const seconds = ms / 1000;
    if (seconds < 60) return `${seconds.toFixed(2)}s`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m ${Math.round(seconds - minutes * 60)}s`;
};

const GanttBar = styled(LinearProgress)(({theme}) => ({
    height: '50%',
    alignSelf: 'center',
    borderRadius: 4,
    [`&.${linearProgressClasses.colorPrimary}`]: {
        backgroundColor: 'transparent',
    },
    [`& .${linearProgressClasses.bar}`]: {
        borderRadius: 4,
        backgroundColor: theme.palette.info.light,
    },
}));

const AxisLabel = ({sx, ...props}) => (
    <Typography variant="caption" sx={{position: 'absolute', top: 0, whiteSpace: 'nowrap', ...sx}} {...props}/>
);

const LABEL_WIDTH = 150;
const ROW_HEIGHT = 28;

export const JobProgressDisplay = ({executionSteps}) => {
    const theme = useTheme();
    const steps = ignoreEnqueuedAndScheduled(executionSteps ?? []);
    if (steps.length === 0) return null;

    const {start, end, stepEnds} = getTimeline(steps);
    const mid = (start + end) / 2;

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{
                    '& .MuiBox-root[role=gantt-row]:not(:last-child)': {
                        borderBottom: "1px dashed lightgray"
                    },
                    '& .MuiBox-root[role=gantt-row-label]': {
                        borderRight: "1px dashed lightgray"
                    }
                }}>
                    <Typography variant="h6" gutterBottom>Execution Timeline</Typography>

                    <Box sx={{display: 'flex', alignItems: 'flex-end', mb: 1}}>
                        <Box sx={{width: LABEL_WIDTH, flexShrink: 0}} role="gantt-row-label"/>
                        <Box sx={{
                            flex: 1, position: 'relative', height: 18,
                        }}>
                            <AxisLabel sx={{left: 0}}>{formatAxisTime(start)}</AxisLabel>
                            <AxisLabel sx={{left: '50%', transform: 'translateX(-50%)'}}>{formatAxisTime(mid)}</AxisLabel>
                            <AxisLabel sx={{right: 0}}>{formatAxisTime(end)}</AxisLabel>
                        </Box>
                    </Box>

                    {steps.map((step, index) => {
                        const stepEndMs = stepEnds[index];
                        const {offset, width, isPoint} = getStepPlacement(step, stepEndMs, start, end);
                        const tooltipTitle = buildTooltipTitle(step, stepEndMs);
                        return (
                            <Box key={index} role="gantt-row"
                                 sx={{display: 'flex', alignItems: 'center', height: ROW_HEIGHT, mb: 0.5}}>
                                <Box sx={{width: LABEL_WIDTH, flexShrink: 0, pr: 1, overflow: 'hidden'}} role="gantt-row-label">
                                    <Typography variant="body2" noWrap>{stepLabel(step)}</Typography>
                                </Box>
                                <Box sx={{flex: 1, position: 'relative', height: 18}}>
                                    <Tooltip title={tooltipTitle} enterDelay={0}>
                                        {isPoint ? (
                                            <RadioButtonChecked sx={{
                                                position: 'absolute',
                                                left: `${offset}%`,
                                                top: '50%',
                                                transform: 'translate(-50%, -50%)',
                                                borderRadius: '50%',
                                            }} color="info" fontSize="small"
                                            />
                                        ) : (
                                            <Box sx={{
                                                position: 'absolute',
                                                left: `${offset}%`,
                                                width: `${width}%`,
                                                top: 0, bottom: 0,
                                            }}>
                                                <GanttBar variant="determinate" value={100}/>
                                            </Box>
                                        )}
                                    </Tooltip>
                                </Box>
                            </Box>
                        );
                    })}
                </CardContent>
            </Card>
        </Box>
    );
}
