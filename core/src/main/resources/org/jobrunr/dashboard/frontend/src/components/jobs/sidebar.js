import React from 'react';
import {Link} from "react-router-dom";
import Drawer from '@material-ui/core/Drawer';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Chip from '@material-ui/core/Chip';
import {makeStyles} from '@material-ui/core/styles';
import {Schedule} from "@material-ui/icons";
import {AlertCircleOutline, Check, Cogs, Delete, TimerSand} from "mdi-material-ui";
import statsState from "../../StatsStateContext";

const drawerWidth = 320;

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
    },
    content: {
        width: '100%',
    },
    drawerPaper: {
        width: drawerWidth,
    },
    nested: {
        paddingLeft: theme.spacing(4),
    },
    toolbar: theme.mixins.toolbar,
}));

const Sidebar = () => {
    const classes = useStyles();
    const stats = statsState.useStatsState(Sidebar);

    return (
        <Drawer
            className={classes.drawer}
            variant="permanent"
            classes={{
                paper: classes.drawerPaper,
            }}
        >
            <div className={classes.toolbar}/>
            <List>
                <List component="div" disablePadding>
                    <ListItem id="scheduled-menu-btn" button key="Scheduled" component={Link}
                              to="/dashboard/jobs?state=SCHEDULED">
                        <ListItemIcon><Schedule/></ListItemIcon>
                        <ListItemText primary="Scheduled"/><Chip label={stats.scheduled}/>
                    </ListItem>
                    <ListItem id="enqueued-menu-btn" button key="Enqueued"
                              component={Link} to="/dashboard/jobs?state=ENQUEUED">
                        <ListItemIcon><TimerSand/></ListItemIcon>
                        <ListItemText primary="Enqueued"/>
                        <Chip label={stats.enqueued}/>
                    </ListItem>
                    <ListItem id="processing-menu-btn" button key="Processing"
                              component={Link} to="/dashboard/jobs?state=PROCESSING">
                        <ListItemIcon><Cogs/></ListItemIcon>
                        <ListItemText primary="Processing"/><Chip label={stats.processing}/>
                    </ListItem>
                    <ListItem id="succeeded-menu-btn" button key="Succeeded"
                              component={Link} to="/dashboard/jobs?state=SUCCEEDED">
                        <ListItemIcon><Check/></ListItemIcon>
                        <ListItemText primary="Succeeded"/><Chip label={stats.succeeded}/>
                    </ListItem>
                    <ListItem id="failed-menu-btn" button key="Failed" component={Link}
                              to="/dashboard/jobs?state=FAILED">
                        <ListItemIcon><AlertCircleOutline/></ListItemIcon>
                        <ListItemText primary="Failed"/><Chip label={stats.failed}/>
                    </ListItem>
                    <ListItem id="deleted-menu-btn" button key="Deleted" component={Link}
                              to="/dashboard/jobs?state=DELETED">
                        <ListItemIcon><Delete/></ListItemIcon>
                        <ListItemText primary="Deleted"/><Chip label={stats.deleted}/>
                    </ListItem>
                </List>
            </List>
        </Drawer>
    );
};

export default Sidebar;