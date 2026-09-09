import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {ToggleButton, ToggleButtonGroup} from "@mui/material";
import {useEffect, useState} from 'react';
import {SwitchableTimeFormatter} from "../../utils/time-ago.js";
import {TimelineGanttChart} from "./timeline-gantt-chart.js";
import {buildTimelineModel, EXCLUDED_STATES, removeInitialScheduled} from "./timeline-data-components/timeline-data.js";
import {END_STATES} from "../../utils/state-names.js";

export const TIMELINE_MODES = {
    COMPACT: "compact",
    DETAILED: "detailed",
}
export const TIMELINE_COMPRESSION_MODES = {
    LINEAR: "linear",
    COMPRESSED: "compressed",
}
const TIMELINE_MODE_STORAGE_KEY = "executionTimelineMode";
const TIMELINE_COMPRESSION_STORAGE_KEY = "executionTimelineCompression";

const JOBRUNR_STEP_PREFIX = "jr_step_";
const JOBRUNR_STEP_START_PREFIX = JOBRUNR_STEP_PREFIX + "start_";
const JOBRUNR_STEP_END_PREFIX = JOBRUNR_STEP_PREFIX + "end_";
const JOBRUNR_STEP_RESULT_PREFIX = JOBRUNR_STEP_PREFIX + "result_";
const JOBRUNR_STEP_RESULT_CLASS_PREFIX = JOBRUNR_STEP_RESULT_PREFIX + "class_";

export const JobHistoryChart = ({jobMetadata, jobHistory, reverse = false}) => {
    const [timelineMode, setTimelineMode] = useState(() => localStorage.getItem(TIMELINE_MODE_STORAGE_KEY) ?? TIMELINE_MODES.COMPACT);
    const [compressionMode, setCompressionMode] = useState(() => localStorage.getItem(TIMELINE_COMPRESSION_STORAGE_KEY) ?? TIMELINE_COMPRESSION_MODES.COMPRESSED);

    const getExecutionSteps = () => {
        if (jobMetadata) {
            const runStepOnceMetadata = processRunStepOnceMetadata(jobMetadata);
            const executionSteps = [...jobHistory, ...runStepOnceMetadata];
            executionSteps.sort((a, b) => a.createdAt < b.createdAt ? -1 : a.createdAt > b.createdAt ? 1 : 0);
            return executionSteps;
        }
        return [];
    }

    const processRunStepOnceMetadata = (metadata) => {
        const starts = [];
        const ends = new Map();
        const results = new Map();
        const completed = new Map();
        for (const [key, value] of Object.entries(metadata)) {
            if (key.startsWith(JOBRUNR_STEP_RESULT_CLASS_PREFIX)) continue;
            if (key.startsWith(JOBRUNR_STEP_START_PREFIX)) starts.push([key.slice(JOBRUNR_STEP_START_PREFIX.length), value]);
            else if (key.startsWith(JOBRUNR_STEP_END_PREFIX)) ends.set(key.slice(JOBRUNR_STEP_END_PREFIX.length), value);
            else if (key.startsWith(JOBRUNR_STEP_RESULT_PREFIX)) results.set(key.slice(JOBRUNR_STEP_RESULT_PREFIX.length), value);
            else if (key.startsWith(JOBRUNR_STEP_PREFIX)) completed.set(key.slice(JOBRUNR_STEP_PREFIX.length), value);
        }

        return starts.map(([name, start]) => ({
            state: 'RUN_STEP_ONCE',
            stepName: name,
            createdAt: start,
            updatedAt: ends.get(name),
            succeeded: completed.get(name),
            result: completed.get(name) ? results.get(name.split('__')[0]) : undefined,
        }));
    };

    const executionSteps = getExecutionSteps();

    const steps = removeInitialScheduled(executionSteps);
    const filteredSteps = steps.filter((step) => !EXCLUDED_STATES.includes(step.state));
    const hasCompleted = filteredSteps.length === 0 || END_STATES.includes(filteredSteps[filteredSteps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (hasCompleted) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [hasCompleted]);

    const timelineModel = buildTimelineModel({steps, mode: timelineMode, compression: compressionMode, reverse, now});
    if (!timelineModel) return null;

    const changeMode = (event, mode) => {
        if (!mode) return;
        localStorage.setItem(TIMELINE_MODE_STORAGE_KEY, mode);
        setTimelineMode(mode);
    };

    const changeCompression = (event, compression) => {
        if (!compression) return;
        localStorage.setItem(TIMELINE_COMPRESSION_STORAGE_KEY, compression);
        setCompressionMode(compression);
    };

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{position: 'relative'}}>
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2, gap: 2}}>
                        <Box>
                            <Typography variant="body2" color="text.secondary" sx={{opacity: 0.8}}>
                                Created <SwitchableTimeFormatter date={new Date(timelineModel.start)}/>
                            </Typography>
                        </Box>
                        <Box sx={{display: 'flex', gap: 1, alignItems: 'center'}}>
                            <ToggleButtonGroup onChange={changeMode} value={timelineMode} exclusive size="small" sx={{maxHeight: "32px"}}>
                                <ToggleButton value={TIMELINE_MODES.COMPACT} sx={{fontSize: "12px"}}>Compact</ToggleButton>
                                <ToggleButton value={TIMELINE_MODES.DETAILED} sx={{fontSize: "12px"}}>Detailed</ToggleButton>
                            </ToggleButtonGroup>
                            <ToggleButtonGroup onChange={changeCompression} value={compressionMode} exclusive size="small" sx={{maxHeight: "32px"}}>
                                <ToggleButton value={TIMELINE_COMPRESSION_MODES.LINEAR} sx={{fontSize: "12px"}}>Linear</ToggleButton>
                                <ToggleButton value={TIMELINE_COMPRESSION_MODES.COMPRESSED} sx={{fontSize: "12px"}}>Compressed</ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                    </Box>

                    {executionSteps.length > 0 && <TimelineGanttChart model={timelineModel} timelineMode={timelineMode} reverse={reverse}/>}
                </CardContent>
            </Card>
        </Box>
    );
};
