import React from 'react';
import {makeStyles} from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Chip from '@material-ui/core/Chip';
import Toolbar from '@material-ui/core/Toolbar';
import GitHubIcon from '@material-ui/icons/GitHub';
import IconButton from '@material-ui/core/IconButton';
import Button from '@material-ui/core/Button';
import {Link as RouterLink} from 'react-router-dom';
import statsState from "StatsStateContext.js";
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
        '& div.MuiChip-root': {
            height: 'initial',
            marginLeft: '6px',
            fontSize: '0.75rem'
        },
        '& div span.MuiChip-label': {
            padding: '0 8px'
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

const TopAppBar = () => {
    const classes = useStyles();

    const stats = statsState.useStatsState(TopAppBar);

    return (
        <AppBar position="fixed" className={classes.appBar}>
            <Toolbar>
                <img className={classes.logo} src={logo} alt="JobRunr"/>
                <div className={classes.buttons}>
                    <Button id="dashboard-btn" color="inherit" component={RouterLink} to="/dashboard/overview">
                        Dashboard
                    </Button>
                    <Button id="jobs-btn" color="inherit" component={RouterLink} to="/dashboard/jobs">
                        Jobs <Chip color="secondary" label={stats.enqueued}/>
                    </Button>
                    <Button id="recurring-jobs-btn" color="inherit" component={RouterLink}
                            to="/dashboard/recurring-jobs">
                        Recurring Jobs <Chip color="secondary" label={stats.recurringJobs}/>
                    </Button>
                    <Button id="servers-btn" color="inherit" component={RouterLink} to="/dashboard/servers">
                        Servers <Chip color="secondary" label={stats.backgroundJobServers}/>
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