import React from 'react';
import Typography from '@material-ui/core/Typography';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';
import TimeAgo from "react-timeago/lib";
import Box from "@material-ui/core/Box";
import {makeStyles} from '@material-ui/core/styles';
import {CircularProgress} from "@material-ui/core";
import {CogClockwise} from "mdi-material-ui";
import NotInterestedIcon from '@material-ui/icons/NotInterested';

const useStyles = makeStyles(theme => ({
    table: {
        width: '100%',
    },
    root: {
        width: '100%',
        //maxWidth: 360,
        backgroundColor: theme.palette.background.paper,
    },
    noItemsFound: {
        padding: '1rem'
    },
    idColumn: {
        maxWidth: 0,
        width: '15%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
    },
    inline: {
        display: 'inline',
    },
    spin: {
        animationName: '$spin',
        animationDuration: '5000ms',
        animationIterationCount: 'infinite',
        animationTimingFunction: 'linear'
    },
    "@keyframes spin": {
        from: {
            transform: 'rotate(0deg)'
        },
        to: {
            transform: 'rotate(360deg)'
        }
    }
}));

const Servers = (props) => {
    const classes = useStyles();

    const [isLoading, setIsLoading] = React.useState(true);
    const [servers, setServers] = React.useState({total: 0, limit: 20, currentPage: 0, items: []});


    React.useEffect(() => {
        fetch(`/api/servers`)
            .then(res => res.json())
            .then(response => {
                setServers(response);
                setIsLoading(false);
            })
            .catch(error => console.log(error));
    }, []);

    return (
        <div>
            <Box my={3}>
                <Typography variant="h4">Background Job Servers</Typography>
            </Box>
            <Paper className={classes.paper}>
                {isLoading
                    ? <CircularProgress/>
                    : <>
                        {servers.length < 1
                            ? <Typography variant="body1" className={classes.noItemsFound}>No servers found</Typography>
                            : <>
                                <TableContainer>
                                    <Table className={classes.table} aria-label="simple table">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell className={classes.idColumn}>Id</TableCell>
                                                <TableCell>Workers</TableCell>
                                                <TableCell>Created</TableCell>
                                                <TableCell>Last heartbeat</TableCell>
                                                <TableCell>Running?</TableCell>
                                                {/*<TableCell>Actions</TableCell>*/}
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {servers.map(server => (
                                                <TableRow key={server.id}>
                                                    <TableCell component="th" scope="row" className={classes.idColumn}>
                                                        {server.id}
                                                    </TableCell>
                                                    <TableCell>
                                                        {server.workerPoolSize}
                                                    </TableCell>
                                                    <TableCell>
                                                        <TimeAgo date={new Date(server.firstHeartbeat)}/>
                                                    </TableCell>
                                                    <TableCell>
                                                        <TimeAgo date={new Date(server.lastHeartbeat)}/>
                                                    </TableCell>
                                                    <TableCell>
                                                        {server.running
                                                            ? <CogClockwise className={classes.spin}/>
                                                            : <NotInterestedIcon />
                                                        }
                                                    </TableCell>
                                                    {/*<TableCell>*/}
                                                    {/*    {server.running*/}
                                                    {/*        ? <Pause />*/}
                                                    {/*        : <Play />*/}
                                                    {/*    }*/}
                                                    {/*</TableCell>*/}
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </>
                        }
                    </>
                }

            </Paper>
        </div>
    );
}

export default Servers;