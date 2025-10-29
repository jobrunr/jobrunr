import {styled} from "@mui/material/styles";
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import GitHubIcon from '@mui/icons-material/GitHub';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import {Link as RouterLink} from 'react-router';
import logo from '../assets/jobrunr-logo-white.png';
import {StatChip} from "../components/ui/StatChip";
import {useJobStats} from "../hooks/useJobStats";
import {TopAppBarNotificationCenter} from "../components/notifications/top-app-bar-notification-center";
import {Preferences} from "../components/settings/preferences.js";

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

const OverviewButton = () => {
    return (
        <Button id="dashboard-btn" color="inherit" component={RouterLink} to="/dashboard/overview">
            Dashboard
        </Button>
    )
}

const MenuButtonWithStat = ({text, stat, ...rest}) => {
    return (
        <Button id="jobs-btn" color="inherit" component={RouterLink} {...rest}>
            {text} <StatChip color="secondary" label={stat}/>
        </Button>
    )
}

const TopAppBar = () => {
    const [stats, _] = useJobStats();

    return (
        <StyledAppBar position="fixed">
            <Toolbar style={{display: "flex", alignItems: "center"}}>
                <img style={{width: 'auto', height: '35px'}} src={logo} alt="JobRunr"/>
                <Buttons>
                    <OverviewButton/>
                    <MenuButtonWithStat text="Jobs" stat={stats.enqueued} id="jobs-btn" to="/dashboard/jobs"/>
                    <MenuButtonWithStat text="Recurring Jobs" stat={stats.recurringJobs} id="recurring-jobs-btn" to="/dashboard/recurring-jobs"/>
                    <MenuButtonWithStat text="Servers" stat={stats.backgroundJobServers} id="servers-btn" to="/dashboard/servers"/>
                </Buttons>
                <TopAppBarNotificationCenter/>
                <Preferences/>
                <IconButton
                    edge="start"
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