import React from 'react';
import {createMuiTheme, makeStyles, MuiThemeProvider} from '@material-ui/core/styles';
import {Redirect, Route, Switch} from 'react-router-dom';
import TopAppBar from "./TopAppBar";
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import RecurringJobs from "../components/recurring-jobs/recurring-jobs";
import WithSidebar from "../components/utils/with-sidebar";
import JobView from "../components/jobs/job-view";
import JobsView from "../components/jobs/jobs-view";
import Sidebar from "../components/jobs/sidebar";

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

const AdminUI = function () {
    const theme = createMuiTheme({
        palette: {
            primary: {
                main: '#000'
            }
        }
    });
    const classes = useStyles();

    return (
        <MuiThemeProvider theme={theme}>
            <div className={classes.root}>
                <TopAppBar/>
                <main className={classes.content}>
                    <Switch>
                        <Route path="/dashboard/overview" component={Overview}/>
                        <Route path="/dashboard/jobs/:id" component={WithSidebar(Sidebar, JobView)}/>
                        <Route path="/dashboard/jobs" component={WithSidebar(Sidebar, JobsView)}/>
                        <Route path="/dashboard/recurring-jobs" component={RecurringJobs}/>
                        <Route path="/dashboard/servers" component={Servers}/>
                        <Redirect from="/dashboard" to="/dashboard/overview"/>
                    </Switch>
                </main>
            </div>
        </MuiThemeProvider>
    );
};

export default AdminUI;