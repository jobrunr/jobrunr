import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import {SwitchableTimeFormatter} from "../../../utils/time-ago.js";
import {END_STATES} from "../../../utils/state-names.js";
import {formatDuration} from "../../../../utils/helper-functions.js";

export const GanttTooltipTitle = ({item}) => {
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