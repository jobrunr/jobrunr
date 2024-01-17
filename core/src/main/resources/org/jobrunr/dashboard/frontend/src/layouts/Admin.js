import { createTheme, ThemeProvider, StyledEngineProvider } from '@mui/material/styles';
import makeStyles from '@mui/styles/makeStyles';
import { Navigate, Route, Routes } from 'react-router-dom';
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
    const JobViewWithSideBar = WithSidebar(Sidebar, JobView);
    const JobsViewWithSidebar = WithSidebar(Sidebar, JobsView);

    return (
        <div className={classes.root}>
            <GithubStarPopup/>
            <TopAppBar/>
            <main className={classes.content}>
                <Routes>
                    <Route path="overview" element={<Overview />}/>
                    <Route path="jobs/:jobId" element={<JobViewWithSideBar />}/>
                    <Route path="jobs" element={<JobsViewWithSidebar />}/>
                    <Route path="recurring-jobs" element={<RecurringJobs />}/>
                    <Route path="servers" element={<Servers />}/>
                    <Route path="*" element={<Navigate to="overview" replace/>} />
                </Routes>
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