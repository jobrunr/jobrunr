import { useState } from 'react';
import makeStyles from '@mui/styles/makeStyles';
import Drawer from "@mui/material/Drawer";
import {IconButton} from "@mui/material";
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
        width: `calc(${theme.spacing(7)} + 1px)`,
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
    const [collapsed, setCollapsed] = useState(false);
    const className = classes[collapsed ? 'drawerCollapsed' : 'drawer'];
    return (props) => (
        <div className={classes.root}>
            <Drawer variant="permanent" className={className} classes={{paper: className}}>
                <div className={classes.toolbar}/>
                <Sidebar {...props} />
                <div className={classes.toggle}>
                    <IconButton
                        onClick={() => setCollapsed(!collapsed)}
                        title="Toggle sidebar"
                        size="large">
                        {collapsed ? <ChevronRight/> : <ChevronLeft/>}
                    </IconButton>
                </div>
            </Drawer>
            <Component />
        </div>
    );
}

export default WithSidebar;