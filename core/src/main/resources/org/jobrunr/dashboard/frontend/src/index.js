import { createRoot } from 'react-dom/client';
import { BrowserRouter, Route, Routes } from "react-router-dom";

import "assets/css/material-dashboard-react.css?v=1.8.1";
import "assets/css/androidstudio.css";

import AdminUI from "layouts/Admin.js";

const root = createRoot(document.getElementById("root"));

root.render(
    <BrowserRouter>
        <Routes>
            <Route path="/dashboard/*" element={<AdminUI />} />
        </Routes>
    </BrowserRouter>
);