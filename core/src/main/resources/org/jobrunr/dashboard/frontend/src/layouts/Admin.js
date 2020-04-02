import React from 'react';
import {makeStyles, withStyles} from '@material-ui/core/styles';
import Badge from '@material-ui/core/Badge';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import GitHubIcon from '@material-ui/icons/GitHub';
import IconButton from '@material-ui/core/IconButton';
import Button from '@material-ui/core/Button';
import {Link as RouterLink, Redirect, Route, Switch} from 'react-router-dom';
import Servers from "../components/servers/servers";
import Jobs from '../components/jobs/jobs.js';
import Job from '../components/jobs/job.js';

const useStyles = makeStyles(theme => ({
    root: {
        flexGrow: 1,
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
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

const StatsContext = React.createContext({});

const AdminUI = function () {
    const classes = useStyles();

    const [stats, setStats] = React.useState({enqueued: 0, backgroundJobServers: 0});

    React.useEffect(() => {
        let eventSource = new EventSource(process.env.REACT_APP_SSE_URL)
        eventSource.onmessage = e => {
            console.log(e.data);
            setStats(JSON.parse(e.data));
        }
    }, [])

    return (
        <StatsContext.Provider value={{stats}}>
            <div className={classes.root}>
                <AppBar position="fixed" className={classes.appBar}>
                    <Toolbar>
                        <Typography variant="h6" className={classes.title}>
                            JobRunr
                        </Typography>
                        <div className={classes.buttons}>
                            {/*<Button color="inherit" component={RouterLink} to="/dashboard/overview">*/}
                            {/*    Dashboard*/}
                            {/*</Button>*/}
                            <Button id="jobs-btn" color="inherit" component={RouterLink} to="/dashboard/jobs">
                                <AppBarButtonBadge badgeContent={stats.enqueued} color="secondary">
                                    Jobs
                                </AppBarButtonBadge>
                            </Button>
                            <Button id="servers-btn" color="inherit" component={RouterLink} to="/dashboard/servers">
                                <AppBarButtonBadge badgeContent={stats.backgroundJobServers} showZero color="secondary">
                                    Servers
                                </AppBarButtonBadge>
                            </Button>
                        </div>
                        <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu">
                            <GitHubIcon/>
                        </IconButton>
                    </Toolbar>
                </AppBar>
                <main className={classes.content}>
                    <Switch>
                        {/*<Route path="/dashboard/overview" component={Overview} />*/}
                        <Route path="/dashboard/jobs/:queue/:state" component={Jobs} />
                        <Route path="/dashboard/jobs/:id" component={Job} />
                        <Route path="/dashboard/servers" component={Servers} />
                        <Redirect from="/dashboard" to="/dashboard/jobs/default/enqueued"/>
                        <Redirect from="/dashboard/jobs" to="/dashboard/jobs/default/enqueued"/>
                    </Switch>
                </main>
            </div>
        </StatsContext.Provider>
    );
};

export {AdminUI, StatsContext}