import {Link} from "react-router";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import {Schedule} from "@mui/icons-material";
import {AlertCircleOutline, Check, Cogs, Delete, LockClock, TimerSand} from "mdi-material-ui";
import {humanReadableNumber} from "../../utils/helper-functions";
import {useJobStats} from "../../hooks/useJobStats";
import {StatChip} from "../ui/StatChip";

const categories = [
    {name: "awaiting", state: "AWAITING", label: "Pending", icon: <LockClock/>},
    {name: "scheduled", state: "SCHEDULED", label: "Scheduled", icon: <Schedule/>},
    {name: "enqueued", state: "ENQUEUED", label: "Enqueued", icon: <TimerSand/>},
    {name: "processing", state: "PROCESSING", label: "Processing", icon: <Cogs/>},
    {name: "succeeded", state: "SUCCEEDED", label: "Succeeded", icon: <Check/>},
    {name: "failed", state: "FAILED", label: "Failed", icon: <AlertCircleOutline/>},
    {name: "deleted", state: "DELETED", label: "Deleted", icon: <Delete/>},
];

const itemSx = {
    minHeight: 48,
    px: 2.5,
};

const iconSx = {
    minWidth: 0,
    mr: 3,
};

const textSx = (collapsed) => ({
    opacity: collapsed ? 0 : 1,
});

const Sidebar = ({collapsed = false}) => {
    const [stats, _] = useJobStats();

    return (
        <List sx={{overflowX: 'hidden'}}>
            {categories.map(({name, state, label, icon}) => (
                <ListItem disablePadding key={label} sx={{display: "block"}}>
                    <ListItemButton id={`${name}-menu-btn`} key={label} title={label} component={Link} to={`/dashboard/jobs?state=${state}`} sx={itemSx}>
                        <ListItemIcon sx={iconSx}>{icon}</ListItemIcon>
                        <ListItemText primary={label} sx={textSx(collapsed)}/>
                        <StatChip label={humanReadableNumber(stats[name])}/>
                    </ListItemButton>
                </ListItem>
            ))}
        </List>
    );
};

export default Sidebar;