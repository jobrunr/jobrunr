import { createTheme, ThemeProvider, StyledEngineProvider } from '@mui/material/styles';
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

const theme = createTheme({
    palette: {
        primary: {
            main: '#000'
        },
        secondary: {
            main: '#f50057'
        }
    },
    components: {
        MuiLink: {
            styleOverrides: {
                root: {
                    color: "#26a8ed"
                }
            }
        }
    }
});

const App = () => {
    const classes = useStyles();
    return (
        <div className={classes.root}>
            <GithubStarPopup/>
            <TopAppBar/>
            <main className={classes.content}>
                <Switch>
                    <Route path="/dashboard/overview" children={<Overview />}/>
                    <Route path="/dashboard/jobs/:jobId" children={WithSidebar(Sidebar, JobView)}/>
                    <Route path="/dashboard/jobs" children={WithSidebar(Sidebar, JobsView)}/>
                    <Route path="/dashboard/recurring-jobs" children={<RecurringJobs />}/>
                    <Route path="/dashboard/servers" children={<Servers />}/>
                    <Route path="/dashboard" render={() => <Redirect to="/dashboard/overview" />} />
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