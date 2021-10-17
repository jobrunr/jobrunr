import React from 'react';
import {Link} from "react-router-dom";
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Chip from '@material-ui/core/Chip';
import {Schedule} from "@material-ui/icons";
import {AlertCircleOutline, Check, Cogs, Delete, TimerSand} from "mdi-material-ui";
import statsState from "../../StatsStateContext";

const categories = [
    {name: "scheduled", state: "SCHEDULED", label: "Scheduled", icon: <Schedule/>},
    {name: "enqueued", state: "ENQUEUED", label: "Enqueued", icon: <TimerSand/>},
    {name: "processing", state: "PROCESSING", label: "Processing", icon: <Cogs/>},
    {name: "succeeded", state: "SUCCEEDED", label: "Succeeded", icon: <Check/>},
    {name: "failed", state: "FAILED", label: "Failed", icon: <AlertCircleOutline/>},
    {name: "deleted", state: "DELETED", label: "Deleted", icon: <Delete/>},
];

const Sidebar = () => {
    const [stats, setStats] = React.useState(statsState.getStats());
    React.useEffect(() => {
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