import {Link} from "react-router-dom";
import List from '@mui/material/List';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import {Schedule} from "@mui/icons-material";
import {AlertCircleOutline, Check, Cogs, Delete, LockClock, TimerSand} from "mdi-material-ui";
import {ListItemButton} from "@mui/material";
import {StatChip} from "../ui/StatChip";
import {useJobStats} from "../../hooks/useJobStats";

const categories = [
    {name: "awaiting", state: "AWAITING", label: "Pending", icon: <LockClock/>},
    {name: "scheduled", state: "SCHEDULED", label: "Scheduled", icon: <Schedule/>},
    {name: "enqueued", state: "ENQUEUED", label: "Enqueued", icon: <TimerSand/>},
    {name: "processing", state: "PROCESSING", label: "Processing", icon: <Cogs/>},
    {name: "succeeded", state: "SUCCEEDED", label: "Succeeded", icon: <Check/>},
    {name: "failed", state: "FAILED", label: "Failed", icon: <AlertCircleOutline/>},
    {name: "deleted", state: "DELETED", label: "Deleted", icon: <Delete/>},
];

const Sidebar = () => {
    const [stats, _] = useJobStats();

    return (
        <List>
            <List component="div" disablePadding>
                {categories.map(({name, state, label, icon}) => (
                    <ListItemButton id={`${name}-menu-btn`} key={label} title={label} component={Link} to={`/dashboard/jobs?state=${state}`}>
                        <ListItemIcon>{icon}</ListItemIcon>
                        <ListItemText primary={label}/>
                        <StatChip label={stats[name]}/>
                    </ListItemButton>
                ))}
            </List>
        </List>
    );
};

export default Sidebar;