import React from 'react';
import {makeStyles} from '@material-ui/core/styles';

import Sidebar from "./sidebar";
import JobsTable from "./jobs-table";

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
    },
    content: {
        width: '100%',
    },
}));

const Jobs = (props) => {
    const classes = useStyles();

    return (
        <div className={classes.root}>
            <Sidebar />
            <main className={classes.content}>
                <JobsTable {...props} />
            </main>
        </div>
    );
};

export default Jobs;