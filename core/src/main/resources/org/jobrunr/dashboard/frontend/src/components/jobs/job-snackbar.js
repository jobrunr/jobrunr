import React from 'react';
import Alert from '@material-ui/lab/Alert';
import {Snackbar} from "@material-ui/core";


const JobSnackbar = (props) => {
    const [openSnackbar, setOpenSnackbar] = React.useState(false);

    const handleCloseAlert = (event, reason) => {
        setOpenSnackbar(false);
    };

    return (
        <Snackbar open={openSnackbar} autoHideDuration={3000} onClose={handleCloseAlert}>
            <Alert severity={props.severity}>
                {props.message}
            </Alert>
        </Snackbar>
    );
};

export default JobSnackbar;