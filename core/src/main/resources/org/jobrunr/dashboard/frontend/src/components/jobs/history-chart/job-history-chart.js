import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {ToggleButton, ToggleButtonGroup} from "@mui/material";
import {useEffect, useState} from 'react';
import {SwitchableTimeFormatter} from "../../utils/time-ago.js";
import {TimelineChart} from "./timeline-chart.js";
import {buildTimelineModel, END_STATES, EXCLUDED_NON_COMPACT, removeInitialScheduled} from "./timeline-data.js";

export const JobHistoryChart = ({executionSteps, reverse = false}) => {
    const [timelineMode, setTimelineMode] = useState(() => localStorage.getItem("executionTimelineMode") ?? "compact");
    const [compressionMode, setCompressionMode] = useState(() => localStorage.getItem("executionTimelineCompression") ?? "compressed");

    const steps = removeInitialScheduled(executionSteps);
    const rawSteps = steps.filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));
    const inProgress = rawSteps.length > 0 && !END_STATES.includes(rawSteps[rawSteps.length - 1].state);

    const [now, setNow] = useState(() => Date.now());
    useEffect(() => {
        if (!inProgress) return undefined;
        const id = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(id);
    }, [inProgress]);

    const timelineModel = buildTimelineModel({steps, mode: timelineMode, compression: compressionMode, reverse, now});
    if (!timelineModel) return null;

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

    return (
        <Box sx={{width: '100%'}}>
            <Card>
                <CardContent sx={{position: 'relative'}}>
                    <Box sx={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2}}>
                        <Box>
                            <Typography variant="body2" color="text.secondary" sx={{opacity: 0.8}}>
                                Created <SwitchableTimeFormatter date={new Date(timelineModel.start)}/>
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

                    <TimelineChart model={timelineModel} timelineMode={timelineMode} reverse={reverse}/>
                </CardContent>
            </Card>
        </Box>
    );
};
