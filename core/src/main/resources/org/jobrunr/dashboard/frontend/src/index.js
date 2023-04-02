import React from 'react';
import ReactDOM from 'react-dom';

import {createBrowserHistory} from "history";
import {Route, Router, Switch} from "react-router-dom";

import "assets/css/material-dashboard-react.css?v=1.8.1";
import "assets/css/androidstudio.css";

import AdminUI from "layouts/Admin.js";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from '@date-io/date-fns';

const hist = createBrowserHistory();

ReactDOM.render(
    <MuiPickersUtilsProvider utils={DateFnsUtils}>
        <Router history={hist}>
            <Switch>
                <Route path="/dashboard" component={AdminUI}/>
            </Switch>
        </Router>
    </MuiPickersUtilsProvider>,
    document.getElementById("root")
);