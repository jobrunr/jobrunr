import {BrowserRouter, Route, Routes} from "react-router";
import {createRoot} from 'react-dom/client';
import AdminUI from "layouts/Admin.js";
import "assets/css/material-dashboard-react.css?v=1.8.1";
import "assets/css/androidstudio.css";

const root = createRoot(document.getElementById("root")); // createRoot(container!) if you use TypeScript

root.render(
    <BrowserRouter>
        <Routes>
            <Route path="/dashboard/*" element={<AdminUI/>}/>
        </Routes>
    </BrowserRouter>
);