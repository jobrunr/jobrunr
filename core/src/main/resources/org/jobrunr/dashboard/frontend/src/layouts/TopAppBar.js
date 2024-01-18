import { useState, useEffect } from 'react';
import { styled } from "@mui/material/styles";
import AppBar from '@mui/material/AppBar';
import Chip from '@mui/material/Chip';
import Toolbar from '@mui/material/Toolbar';
import GitHubIcon from '@mui/icons-material/GitHub';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import {Link as RouterLink} from 'react-router-dom';
import statsState from "StatsStateContext.js";
import logo from '../assets/jobrunr-logo-white.png';

const StyledAppBar = styled(AppBar)(({theme}) => ({
    zIndex: theme.zIndex.drawer + 1
}));

const Buttons = styled("div")(({theme}) => ({
    '& > *': {
        margin: `${theme.spacing(2)}!important`,
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
}));

const classes = {
    menuButton: {
        marginRight: 2,
    },
    logo: {
        width: 'auto',
        height: '35px'
    }
};

const TopAppBar = () => {
    const [stats, setStats] = useState(statsState.getStats());
    useEffect(() => {
        statsState.addListener(setStats);
        return () => statsState.removeListener(setStats);
    }, [])

    return (
        <StyledAppBar position="fixed">
            <Toolbar style={{display: "flex", alignItems: "center"}}>
                <img style={classes.logo} src={logo} alt="JobRunr"/>
                <Buttons>
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
                </Buttons>
                <IconButton
                    edge="start"
                    sx={classes.menuButton}
                    color="inherit"
                    aria-label="menu"
                    target="_blank"
                    href="https://github.com/jobrunr/jobrunr"
                    size="large">
                    <GitHubIcon/>
                </IconButton>
            </Toolbar>
        </StyledAppBar>
    );
}

export default TopAppBar;