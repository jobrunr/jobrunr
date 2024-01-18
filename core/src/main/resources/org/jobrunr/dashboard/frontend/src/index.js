import ReactDOM from 'react-dom';
import { BrowserRouter, Route, Routes } from "react-router-dom";

import "assets/css/material-dashboard-react.css?v=1.8.1";
import "assets/css/androidstudio.css";

import AdminUI from "layouts/Admin.js";

ReactDOM.render(
    <BrowserRouter>
        <Routes>
            <Route path="/dashboard/*" element={<AdminUI />} />
        </Routes>
    </BrowserRouter>,
    document.getElementById("root")
);