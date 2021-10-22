import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import Drawer from "@material-ui/core/Drawer";
import {IconButton} from "@material-ui/core";
import {ChevronLeft, ChevronRight} from "mdi-material-ui";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    toolbar: theme.mixins.toolbar,
    drawer: {
        width: 260,
    },
    drawerCollapsed: {
        width: theme.spacing(7) + 1,
        overflowX: 'hidden',
    },
    toggle: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end',
        paddingRight: theme.spacing(0.5),
        marginTop: 'auto'
    },
}));

const WithSidebar = (Sidebar, Component) => {
    const classes = useStyles();
    const [collapsed, setCollapsed] = React.useState(false);
    const className = classes[collapsed ? 'drawerCollapsed' : 'drawer'];
    return (props) => (
        <div className={classes.root}>
            <Drawer variant="permanent" className={className} classes={{paper: className}}>
                <div className={classes.toolbar}/>
                <Sidebar {...props} />
                <div className={classes.toggle}>
                    <IconButton onClick={() => setCollapsed(!collapsed)} title="Toggle sidebar">
                        {collapsed ? <ChevronRight/> : <ChevronLeft/>}
                    </IconButton>
                </div>
            </Drawer>
            <Component {...props} />
        </div>
    );
}

export default WithSidebar;