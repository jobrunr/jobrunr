import {useContext, useEffect, useState} from 'react';
import {styled} from "@mui/material/styles";
import AppBar from '@mui/material/AppBar';
import Chip from '@mui/material/Chip';
import Toolbar from '@mui/material/Toolbar';
import GitHubIcon from '@mui/icons-material/GitHub';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import {Link as RouterLink} from 'react-router-dom';
import statsState from "StatsStateContext.js";
import logo from '../assets/jobrunr-logo-white.png';
import {ProblemsContext} from "../ProblemsContext";
import {Badge} from "@mui/material";

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

const StyledBadge = styled(Badge)(({theme}) => ({
    '& .MuiBadge-badge': {
        right: -6,
        top: 6,
    },
}));

const OverviewButton = () => {
    const {problems} = useContext(ProblemsContext);

    const hasProblems = problems?.length > 0;

    return (
        <Button id="dashboard-btn" color="inherit" component={RouterLink}
                to={'/dashboard/overview'}>
            <StyledBadge color="info" variant="dot" badgeContent={hasProblems ? " " : 0}>
                Dashboard
            </StyledBadge>
        </Button>
    )
}

const TopAppBar = () => {
    const [stats, setStats] = useState(statsState.getStats());
    useEffect(() => {
        statsState.addListener(setStats);
        return () => statsState.removeListener(setStats);
    }, [])

    return (
        <StyledAppBar position="fixed">
            <Toolbar style={{display: "flex", alignItems: "center"}}>
                <img style={{width: 'auto', height: '35px'}} src={logo} alt="JobRunr"/>
                <Buttons>
                    <OverviewButton/>
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
                    sx={{marginRight: 2}}
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