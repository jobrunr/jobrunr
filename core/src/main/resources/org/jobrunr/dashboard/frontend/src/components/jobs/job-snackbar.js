import { useState } from 'react';
import Alert from '@mui/material/Alert';
import {Snackbar} from "@mui/material";


const JobSnackbar = (props) => {
    const [openSnackbar, setOpenSnackbar] = useState(false);

    const handleCloseAlert = (event, reason) => {
        setOpenSnackbar(false);
    };

    return (
        <Snackbar open={openSnackbar}
            autoHideDuration={3000}
            onClose={handleCloseAlert}
            anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
         >
            <Alert severity={props.severity}>
                {props.message}
            </Alert>
        </Snackbar>
    );
};

export default JobSnackbar;