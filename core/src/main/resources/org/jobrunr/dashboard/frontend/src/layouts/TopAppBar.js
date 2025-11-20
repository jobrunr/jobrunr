import {styled} from "@mui/material/styles";
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import GitHubIcon from '@mui/icons-material/GitHub';
import Badge from '@mui/material/Badge';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import {Link as RouterLink} from 'react-router';
import Logo from '../assets/logo.svg?react';
import {StatChip} from "../components/ui/StatChip";
import {useJobStats} from "../hooks/useJobStats";
import {TopAppBarNotificationCenter} from "../components/notifications/top-app-bar-notification-center";
import {Preferences} from "../components/settings/preferences.js";
import DashboardIcon from '@mui/icons-material/Dashboard';
import WorkIcon from '@mui/icons-material/Work';
import EventRepeatIcon from '@mui/icons-material/EventRepeat';
import DnsIcon from '@mui/icons-material/Dns';
import useMediaQuery from "@mui/material/useMediaQuery";

const StyledAppBar = styled(AppBar)(({theme}) => ({
    zIndex: theme.zIndex.drawer + 1
}));

const StyledLogo = styled(Logo)(({theme}) => ({
    height: '28px',
    fill: 'currentColor',
    flexShrink: 0,
    [theme.breakpoints.down('md')]: {
        height: '24px',
    }
}));

const Buttons = styled("div")(({theme}) => ({
    display: 'flex',
    flexWrap: 'nowrap',
    flexShrink: 0,
    alignItems: 'center',

    '& div.MuiChip-root': {
        height: 'initial',
        marginLeft: '4px',
        fontSize: '0.75rem'
    },
    '& div span.MuiChip-label': {
        padding: '0 8px'
    },
    margin: "0 16px",
    flexGrow: 1,
}));

const MobileFriendlyButton = styled(Button)(({theme}) => ({
    padding: `${theme.spacing(1.25)}`,
    [theme.breakpoints.down('md')]: {
        flexDirection: 'column',
        fontSize: theme.typography.pxToRem(10),
        padding: `${theme.spacing(1)}`
    }
}));

const OverviewButton = () => {
    const isMobile = useMediaQuery((theme) => theme.breakpoints.down('md'));

    return (
        <MobileFriendlyButton id="dashboard-btn" color="inherit" component={RouterLink} to="/dashboard/overview">
            {isMobile && <DashboardIcon fontSize="small"/>}
            Dashboard
        </MobileFriendlyButton>
    )
}

const MenuButtonWithStat = ({text, mobileText, stat, icon, ...rest}) => {
    const isMobile = useMediaQuery((theme) => theme.breakpoints.down('md'));

    const label = isMobile ? mobileText || text : text;

    return (
        <MobileFriendlyButton color="inherit" component={RouterLink} {...rest}>
            {isMobile ? (
                <>
                    <Badge badgeContent={stat} color="secondary">
                        {icon}
                    </Badge>
                    {label}
                </>
            ) : (
                <>{label} <StatChip color="secondary" label={stat}/></>
            )}
        </MobileFriendlyButton>
    )
}

const TopAppBar = () => {
    const [stats, _] = useJobStats();

    return (
        <StyledAppBar position="fixed" sx={{overflowX: "hidden"}}>
            <Toolbar sx={{display: "flex", alignItems: "center", overflowX: "auto", scrollbarWidth: "thin"}}>
                <StyledLogo aria-label="JobRunr"/>
                <Buttons>
                    <OverviewButton/>
                    <MenuButtonWithStat
                        text="Jobs"
                        stat={stats.enqueued}
                        id="jobs-btn"
                        to="/dashboard/jobs"
                        icon={<WorkIcon fontSize="small"/>}
                    />
                    <MenuButtonWithStat
                        text="Recurring Jobs"
                        mobileText="Recurring"
                        stat={stats.recurringJobs}
                        id="recurring-jobs-btn"
                        to="/dashboard/recurring-jobs"
                        icon={<EventRepeatIcon fontSize="small"/>}
                    />
                    <MenuButtonWithStat
                        text="Servers"
                        stat={stats.backgroundJobServers}
                        id="servers-btn"
                        to="/dashboard/servers"
                        icon={<DnsIcon fontSize="small"/>}
                    />
                </Buttons>
                <TopAppBarNotificationCenter/>
                <Preferences/>
                <IconButton
                    edge="start"
                    color="inherit"
                    aria-label="menu"
                    target="_blank"
                    href="https://github.com/jobrunr/jobrunr"
                >
                    <GitHubIcon fontSize="small"/>
                </IconButton>
            </Toolbar>
        </StyledAppBar>
    );
}

export default TopAppBar;