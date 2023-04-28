import React from 'react';
import ReactDOM from 'react-dom';

import {createBrowserHistory} from "history";
import {Route, Router, Switch} from "react-router-dom";

import "assets/css/material-dashboard-react.css?v=1.8.1";
import "assets/css/androidstudio.css";

import AdminUI from "layouts/Admin.js";

const hist = createBrowserHistory();

ReactDOM.render(
    <Router history={hist}>
        <Switch>
            <Route path="/dashboard" component={AdminUI} />
        </Switch>
    </Router>,
    document.getElementById("root")
);