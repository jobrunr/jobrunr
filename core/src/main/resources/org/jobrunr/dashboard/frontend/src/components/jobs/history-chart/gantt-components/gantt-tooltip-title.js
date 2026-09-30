import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {SwitchableTimeFormatter} from "../../../utils/time-ago.js";
import {END_STATES} from "../../../utils/state-names.js";
import {formatDuration} from "../../../../utils/helper-functions.js";
import {Divider, Stack} from "@mui/material";

export const GanttTooltipTitle = ({item}) => {
    const {startMs, endMs, active, state, placement, isSkipped} = item;
    const isCompressed = placement?.isCompressed ?? false;
    const includeEnd = endMs !== null && endMs !== startMs;
    const result = item.result ?? item.outcome?.result;
    
    return (
        <Stack spacing={1} sx={{p: 0.5, maxWidth: 500}}>
            {(!isSkipped || includeEnd || !END_STATES.includes(state)) &&
                <Box sx={{display: "grid", gridTemplateColumns: "75px 1fr", rowGap: 0.4, columnGap: 1, alignItems: "baseline"}}>
                    {!isSkipped && <>
                        <Typography variant="caption" sx={{opacity: 0.7, fontWeight: 600}}>
                            {includeEnd ? "Started:" : "Completed:"}
                        </Typography>
                        <Typography variant="caption">
                            <SwitchableTimeFormatter date={new Date(startMs)}/>
                        </Typography>
                    </>}

                    {includeEnd && <>
                        <Typography variant="caption" sx={{opacity: 0.7, fontWeight: 600}}>
                            {active ? "Status:" : "Ended:"}
                        </Typography>
                        <Typography variant="caption">
                            {active ? (`Taking > ${formatDuration(startMs, endMs)}`) : (<SwitchableTimeFormatter date={new Date(endMs)}/>)}
                        </Typography>
                    </>}

                    {!END_STATES.includes(state) && <>
                        <Typography variant="caption" sx={{opacity: 0.7, fontWeight: 600}}>
                            Duration:
                        </Typography>
                        <Typography variant="caption">
                            {active ? "In progress..." : isSkipped ? "Skipped" : `Took ${includeEnd ? formatDuration(startMs, endMs) : "<1 ms"}`}
                            {isCompressed && " (visually shortened)"}
                        </Typography>
                    </>}
                </Box>
            }

            {!isSkipped && result && <>
                <Divider sx={{borderColor: "rgba(255, 255, 255, 0.12)"}}/>
                <Box>
                    <Typography variant="caption" sx={{opacity: 0.7, fontWeight: 600, display: "block", mb: 0.5}}>
                        Step Result
                    </Typography>
                    <Box
                        component="pre"
                        sx={{
                            m: 0, p: 1,
                            backgroundColor: "rgba(0, 0, 0, 0.35)",
                            borderRadius: 1,
                            fontSize: "0.7rem", fontFamily: "monospace",
                            whiteSpace: "pre",
                            maxHeight: 120,
                            overflow: "auto",
                            color: "#ffab91",
                            "&::-webkit-scrollbar": {height: 4, width: 4},
                            "&::-webkit-scrollbar-thumb": {backgroundColor: "#30363d", borderRadius: 2},
                            "&::-webkit-scrollbar-corner": {backgroundColor: "transparent",},
                        }}
                    >
                        {result}
                    </Box>
                </Box>
            </>}
        </Stack>
    );
};