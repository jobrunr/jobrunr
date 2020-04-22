import React from 'react';
import {CircularProgress} from "@material-ui/core";

const LoadingIndicator = () => {
    return (
        <div style={{display: 'flex', justifyContent: 'center'}}>
            <CircularProgress/>
        </div>
    );
}

export default LoadingIndicator;