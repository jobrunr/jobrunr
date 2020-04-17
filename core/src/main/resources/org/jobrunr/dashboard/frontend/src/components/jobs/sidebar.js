import React, {useContext} from 'react';
import {Link} from "react-router-dom";
import Drawer from '@material-ui/core/Drawer';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import Collapse from '@material-ui/core/Collapse';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Chip from '@material-ui/core/Chip';

import ExpandLess from '@material-ui/icons/ExpandLess';
import ExpandMore from '@material-ui/icons/ExpandMore';
import {makeStyles} from '@material-ui/core/styles';
import {Schedule} from "@material-ui/icons";
import {AlertCircleOutline, Check, Cogs, TimerSand, TrayFull} from "mdi-material-ui";
import {StatsContext} from '../../layouts/Admin';

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

const Sidebar = (props) => {
    const classes = useStyles();
    const statsContext = useContext(StatsContext);
    const { stats } = statsContext;

    const [open, setOpen] = React.useState(true);

    const handleClick = () => {
        setOpen(!open);
    };

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
                <ListItem button onClick={handleClick}>
                    <ListItemIcon>
                        <TrayFull/>
                    </ListItemIcon>
                    <ListItemText primary="Default queue"/>
                    {open ? <ExpandLess/> : <ExpandMore/>}
                </ListItem>
                <Collapse in={open} timeout="auto" unmountOnExit>
                    <List component="div" disablePadding>
                        <ListItem id="scheduled-menu-btn" button key="Scheduled" className={classes.nested} component={Link} to="/dashboard/jobs/default/scheduled">
                            <ListItemIcon><Schedule/></ListItemIcon>
                            <ListItemText primary="Scheduled"/><Chip label={stats.scheduled}/>
                        </ListItem>
                        <ListItem id="enqueued-menu-btn" button key="Enqueued" className={classes.nested} component={Link} to="/dashboard/jobs/default/enqueued">
                            <ListItemIcon><TimerSand/></ListItemIcon>
                            <ListItemText primary="Enqueued"/><Chip label={stats.enqueued}/>
                        </ListItem>
                        <ListItem id="processing-menu-btn" button key="Processing" className={classes.nested} component={Link} to="/dashboard/jobs/default/processing">
                            <ListItemIcon><Cogs/></ListItemIcon>
                            <ListItemText primary="Processing"/><Chip label={stats.processing}/>
                        </ListItem>
                        <ListItem id="succeeded-menu-btn" button key="Succeeded" className={classes.nested} component={Link} to="/dashboard/jobs/default/succeeded">
                            <ListItemIcon><Check/></ListItemIcon>
                            <ListItemText primary="Succeeded"/><Chip label={stats.succeeded}/>
                        </ListItem>
                        <ListItem id="failed-menu-btn" button key="Failed" className={classes.nested} component={Link} to="/dashboard/jobs/default/failed">
                            <ListItemIcon><AlertCircleOutline/></ListItemIcon>
                            <ListItemText primary="Failed"/><Chip label={stats.failed}/>
                        </ListItem>
                    </List>
                </Collapse>
            </List>
        </Drawer>
    );
};

export default Sidebar;