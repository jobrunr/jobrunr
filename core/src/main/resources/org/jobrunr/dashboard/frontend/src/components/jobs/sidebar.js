import {useEffect, useState} from 'react';
import {Link} from "react-router-dom";
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Chip from '@mui/material/Chip';
import {Schedule} from "@mui/icons-material";
import {AlertCircleOutline, Check, Cogs, Delete, LockClock, TimerSand} from "mdi-material-ui";
import statsState from "../../StatsStateContext";

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
    const [stats, setStats] = useState(statsState.getStats());
    useEffect(() => {
        statsState.addListener(setStats);
        return () => statsState.removeListener(setStats);
    }, [])

    return (
        <List>
            <List component="div" disablePadding>
                {categories.map(({name, state, label, icon}) => (
                    <ListItem id={`${name}-menu-btn`} button key={label} title={label}
                              component={Link} to={`/dashboard/jobs?state=${state}`}>
                        <ListItemIcon>{icon}</ListItemIcon>
                        <ListItemText primary={label}/>
                        <Chip label={stats[name]}/>
                    </ListItem>
                ))}
            </List>
        </List>
    );
};

export default Sidebar;