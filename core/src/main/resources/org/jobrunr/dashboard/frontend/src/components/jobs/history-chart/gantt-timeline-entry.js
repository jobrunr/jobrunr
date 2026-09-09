import Tooltip from "@mui/material/Tooltip";
import {SwitchableTimeFormatter} from "../../utils/time-ago.js";
import Typography from "@mui/material/Typography";

export const GanttTimelineEntry = ({time, position}) => {
    return <Tooltip key={time.ms} title={<>
        <SwitchableTimeFormatter date={new Date(time.startMs)}/>
        {time.endMs && <> - <SwitchableTimeFormatter date={new Date(time.endMs)}/></>}
    </>}>
        <Typography variant="caption" sx={{
            position: 'absolute',
            top: 0,
            left: `${position(time.pct)}%`,
            transform: 'translateX(-50%)',
            whiteSpace: 'nowrap',
            color: 'text.secondary'
        }}>
            {time.label}
        </Typography>
    </Tooltip>;
}