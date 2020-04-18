import React from 'react';
import {makeStyles, withStyles} from '@material-ui/core/styles';
import Badge from '@material-ui/core/Badge';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import GitHubIcon from '@material-ui/icons/GitHub';
import IconButton from '@material-ui/core/IconButton';
import Button from '@material-ui/core/Button';
import {Link as RouterLink} from 'react-router-dom';
import state from "../StateContext";
import logo from '../assets/jobrunr-logo-white.png';

const useStyles = makeStyles(theme => ({
    root: {
        flexGrow: 1,
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1
    },
    logo: {
        width: 'auto',
        height: '35px'
    },
    buttons: {
        '& > *': {
            margin: theme.spacing(2),
        },
        '& > Badge': {
            right: -10
        },
        margin: "0 50px",
        flexGrow: 1,
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing(3),
        marginTop: 56
    },
    paper: {
        padding: theme.spacing(2),
        textAlign: 'center',
        color: theme.palette.text.secondary,
    },
}));

const AppBarButtonBadge = withStyles(theme => ({
    badge: {
        right: -10,
    },
}))(Badge);

const TopAppBar = () => {
    const classes = useStyles();

    const stats = state.useStatsState(TopAppBar);

    return (
        <AppBar position="fixed" className={classes.appBar}>
            <Toolbar>
                <img className={classes.logo} src={logo} alt="JobRunr"/>
                <div className={classes.buttons}>
                    <Button color="inherit" component={RouterLink} to="/dashboard/overview">
                        Dashboard
                    </Button>
                    <Button id="jobs-btn" color="inherit" component={RouterLink} to="/dashboard/jobs">
                        <AppBarButtonBadge badgeContent={stats.enqueued} max={99999} color="secondary">
                            Jobs
                        </AppBarButtonBadge>
                    </Button>
                    <Button id="servers-btn" color="inherit" component={RouterLink} to="/dashboard/servers">
                        <AppBarButtonBadge badgeContent={stats.backgroundJobServers} showZero color="secondary">
                            Servers
                        </AppBarButtonBadge>
                    </Button>
                </div>
                <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu"
                            target="_blank" href="https://github.com/jobrunr/jobrunr">
                    <GitHubIcon/>
                </IconButton>
            </Toolbar>
        </AppBar>
    );
}

export default TopAppBar;