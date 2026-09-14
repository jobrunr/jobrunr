import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {ToggleButton, ToggleButtonGroup} from "@mui/material";
import {useEffect, useState} from 'react';
import {SwitchableTimeFormatter} from "../../utils/time-ago.js";
import {TimelineGanttChart} from "./timeline-gantt-chart.js";
import {
    buildTimelineModel,
    EXCLUDED_STATES,
    removeInitialScheduled,
    TIMELINE_COMPRESSION_MODES,
    TIMELINE_MODES
} from "./timeline-data-components/timeline-data.js";
import {createJobExecutionTimelineEntries} from "./utils/timeline-entries.js";
import {ItemsNotFound} from "../../utils/items-not-found.js";

const TIMELINE_MODE_STORAGE_KEY = "executionTimelineMode";
const TIMELINE_COMPRESSION_STORAGE_KEY = "executionTimelineCompression";

export const JobHistoryChart = ({job, reverse = false}) => {
    const [timelineMode, setTimelineMode] = useState(() => localStorage.getItem(TIMELINE_MODE_STORAGE_KEY) ?? TIMELINE_MODES.COMPACT);
    const [compressionMode, setCompressionMode] = useState(() => localStorage.getItem(TIMELINE_COMPRESSION_STORAGE_KEY) ?? TIMELINE_COMPRESSION_MODES.COMPRESSED);

    const timelineEntries = createJobExecutionTimelineEntries(job);
    const steps = removeInitialScheduled(timelineEntries.filter((entry) => !EXCLUDED_STATES.includes(entry.state)));
    const hasCompleted = steps.every((entry) => entry.finishedAt);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (hasCompleted) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [hasCompleted]);

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

    const timelineModel = buildTimelineModel({steps, timelineMode, compressionMode, reverse, now});

    return timelineModel && (
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
                                <ToggleButton value={TIMELINE_COMPRESSION_MODES.COMPRESSED} sx={{fontSize: "12px"}}>Compressed</ToggleButton>
                                <ToggleButton value={TIMELINE_COMPRESSION_MODES.LINEAR} sx={{fontSize: "12px"}}>Linear</ToggleButton>
                            </ToggleButtonGroup>
                        </Box>
                    </Box>

                    {steps.length
                        ? <TimelineGanttChart model={timelineModel} timelineMode={timelineMode} reverse={reverse}/>
                        : <ItemsNotFound>Waiting for the job to move to the <code>ENQUEUED</code> state.</ItemsNotFound>
                    }
                </CardContent>
            </Card>
        </Box>
    );
};
