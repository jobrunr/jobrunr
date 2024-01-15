import { createTheme, ThemeProvider, StyledEngineProvider, adaptV4Theme } from '@mui/material/styles';
import makeStyles from '@mui/styles/makeStyles';
import {Redirect, Route, Switch} from 'react-router-dom';
import TopAppBar from "./TopAppBar";
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import RecurringJobs from "../components/recurring-jobs/recurring-jobs";
import WithSidebar from "../components/utils/with-sidebar";
import JobView from "../components/jobs/job-view";
import JobsView from "../components/jobs/jobs-view";
import Sidebar from "../components/jobs/sidebar";
import GithubStarPopup from "../components/utils/github-star-popup";

const useStyles = makeStyles(theme => ({
    root: {
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

const theme = createTheme(adaptV4Theme({
    palette: {
        primary: {
            main: '#000'
        }
    }
}));

const App = () => {
    const classes = useStyles();
    return (
        <div className={classes.root}>
            <GithubStarPopup/>
            <TopAppBar/>
            <main className={classes.content}>
                <Switch>
                    <Route path="/dashboard/overview" component={Overview}/>
                    <Route path="/dashboard/jobs/:jobId" component={WithSidebar(Sidebar, JobView)}/>
                    <Route path="/dashboard/jobs" component={WithSidebar(Sidebar, JobsView)}/>
                    <Route path="/dashboard/recurring-jobs" component={RecurringJobs}/>
                    <Route path="/dashboard/servers" component={Servers}/>
                    <Redirect from="/dashboard" to="/dashboard/overview"/>
                </Switch>
            </main>
        </div>
    );
}

const AdminUI = function () {
    return (
        <StyledEngineProvider injectFirst>
            <ThemeProvider theme={theme}>
                <App />
            </ThemeProvider>
        </StyledEngineProvider>
    );
};

export default AdminUI;