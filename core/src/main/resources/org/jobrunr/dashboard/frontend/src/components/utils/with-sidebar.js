import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

const useStyles = makeStyles(() => ({
    root: {
        display: 'flex',
    }
}));

const WithSidebar = (Sidebar, Component) => {
    const classes = useStyles();
    return (props) => {
        return (
            <div className={classes.root}>
                <Sidebar {...props} />
                <Component {...props} />
            </div>
        );
    };
}

export default WithSidebar;