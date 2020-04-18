import React from 'react';
import {createMuiTheme, makeStyles, MuiThemeProvider, withStyles} from '@material-ui/core/styles';
import Badge from '@material-ui/core/Badge';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import GitHubIcon from '@material-ui/icons/GitHub';
import IconButton from '@material-ui/core/IconButton';
import Button from '@material-ui/core/Button';
import {Link as RouterLink, Redirect, Route, Switch} from 'react-router-dom';
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import Jobs from '../components/jobs/jobs.js';
import Job from '../components/jobs/job.js';
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

const StatsContext = React.createContext({});

const AdminUI = function () {
    console.log('StatsContext', StatsContext)


    const theme = createMuiTheme({
        palette: {
            primary: {
                main: '#000'
            }
        }
    });
    const classes = useStyles();

    const [stats, setStats] = React.useState({
        enqueued: 0,
        backgroundJobServers: 0,
        estimation: {processingDone: false, estimatedProcessingTimeAvailable: false}
    });
    const oldStatsRef = React.useRef(null);

    React.useEffect(() => {
        let eventSource = new EventSource(process.env.REACT_APP_SSE_URL)
        eventSource.onmessage = e => {
            const newStats = JSON.parse(e.data);

            const oldStats = oldStatsRef.current;
            if (newStats.succeeded != null && newStats.succeeded > 0) {
                if ((newStats.enqueued != null && newStats.enqueued < 1) && (newStats.processing != null && newStats.processing < 1)) {
                    setStats({...newStats, estimation: {processingDone: true}})
                } else if (oldStats == null) {
                    oldStatsRef.current = {...newStats, timestamp: new Date()};
                    setStats({
                        ...newStats,
                        estimation: {processingDone: false, estimatedProcessingTimeAvailable: false}
                    })
                } else {
                    const amountSucceeded = newStats.succeeded - oldStats.succeeded;
                    if (amountSucceeded === 0) {
                        setStats({
                            ...newStats,
                            estimation: {processingDone: false, estimatedProcessingTimeAvailable: false}
                        })
                    } else {
                        const timeDiff = new Date() - oldStats.timestamp;
                        if (!isNaN(timeDiff)) {
                            const amountSucceededPerSecond = amountSucceeded * 1000 / timeDiff;
                            const estimatedProcessingTime = newStats.enqueued / amountSucceededPerSecond
                            const processingTimeDate = (new Date().getTime() + (estimatedProcessingTime * 1000));
                            setStats({
                                ...newStats,
                                estimation: {
                                    processingDone: false,
                                    estimatedProcessingTimeAvailable: true,
                                    estimatedProcessingTime: processingTimeDate
                                }
                            })
                        }
                    }
                }
            }
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    return (
        <MuiThemeProvider theme={theme}>
            <StatsContext.Provider value={stats}>
                <div className={classes.root}>
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
                                    <AppBarButtonBadge badgeContent={stats.backgroundJobServers} showZero
                                                       color="secondary">
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
                    <main className={classes.content}>
                        <Switch>
                            <Route path="/dashboard/overview" component={Overview}/>
                            <Route path="/dashboard/jobs/:queue/:state" component={Jobs}/>
                            <Route path="/dashboard/jobs/:id" component={Job}/>
                            <Route path="/dashboard/servers" component={Servers}/>
                            <Redirect from="/dashboard" to="/dashboard/jobs/default/enqueued"/>
                            <Redirect from="/dashboard/jobs" to="/dashboard/jobs/default/enqueued"/>
                        </Switch>
                    </main>
                </div>
            </StatsContext.Provider>
        </MuiThemeProvider>
    );
};

function useStatsContext() {
    const statsState = React.useContext(StatsContext)
    if (typeof statsState === undefined) {
        throw new Error('useStatsContext must be used within a StatsProvider')
    }
    return statsState
}

export {AdminUI, useStatsContext}