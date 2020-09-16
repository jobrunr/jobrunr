import React from 'react';
import Link from '@material-ui/core/Link';
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
import {CogClockwise} from "mdi-material-ui";
import NotInterestedIcon from '@material-ui/icons/NotInterested';
import Dialog from '@material-ui/core/Dialog';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import MuiDialogContent from '@material-ui/core/DialogContent';
import {humanFileSize} from "../../utils/helper-functions";
import LoadingIndicator from "../LoadingIndicator";

const useStyles = makeStyles(theme => ({
    table: {
        width: '100%',
    },
    root: {
        width: '100%',
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

const Servers = React.memo(() => {
    const classes = useStyles();

    const [isLoading, setIsLoading] = React.useState(true);
    const [servers, setServers] = React.useState({total: 0, limit: 20, currentPage: 0, items: []});
    const [open, setOpen] = React.useState(false);
    const serverRef = React.useRef(null);

    const handleOpen = (serverId) => {
        serverRef.current = servers.find(server => server.id = serverId);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        serverRef.current = null;
    };


    React.useEffect(() => {
        fetch(`/api/servers`)
            .then(res => res.json())
            .then(response => {
                setServers(sortServers(response));
                setIsLoading(false);
            })
            .catch(error => console.log(error));

        const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/servers");
        eventSource.addEventListener('message', e => setServers(sortServers(JSON.parse(e.data))));
        eventSource.addEventListener('close', e => eventSource.close());
        return function cleanUp() {
            eventSource.close();
        }
    }, []);

    const sortServers = (servers) => {
        servers.sort((a, b) => a.firstHeartbeat > b.firstHeartbeat)
        return servers;
    }

    return (
        <div>
            <Box my={3}>
                <Typography variant="h4">Background Job Servers</Typography>
            </Box>
            {isLoading
                ? <LoadingIndicator/>
                : <Paper className={classes.paper}>
                    {servers.length < 1
                        ? <Typography variant="body1" className={classes.noItemsFound}>No servers found</Typography>
                        : <>
                            <TableContainer>
                                <Table className={classes.table} aria-label="servers overview">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell className={classes.idColumn}>Id</TableCell>
                                            <TableCell>Workers</TableCell>
                                            <TableCell>Created</TableCell>
                                            <TableCell>Last heartbeat</TableCell>
                                            <TableCell>Free memory</TableCell>
                                            <TableCell>Cpu load</TableCell>
                                            <TableCell>Running?</TableCell>
                                            {/*<TableCell>Actions</TableCell>*/}
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {servers.map(server => (
                                            <TableRow key={server.id}>
                                                <TableCell component="th" scope="row" className={classes.idColumn}>
                                                    <Link color="initial" onClick={() => handleOpen(server.id)}>
                                                        {server.id}
                                                    </Link>
                                                </TableCell>
                                                <TableCell>
                                                    {server.workerPoolSize}
                                                </TableCell>
                                                <TableCell>
                                                    <TimeAgo date={new Date(server.firstHeartbeat)}
                                                             title={new Date(server.firstHeartbeat).toString()}/>
                                                </TableCell>
                                                <TableCell>
                                                    <TimeAgo date={new Date(server.lastHeartbeat)}
                                                             title={new Date(server.lastHeartbeat).toString()}/>
                                                </TableCell>
                                                <TableCell>
                                                    {humanFileSize(server.processFreeMemory, true)}
                                                </TableCell>
                                                <TableCell>
                                                    {(server.systemCpuLoad * 100).toFixed(2)} %
                                                </TableCell>
                                                <TableCell>
                                                    {server.running
                                                        ? <CogClockwise className={classes.spin}/>
                                                        : <NotInterestedIcon/>
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
                </Paper>
            }

            {serverRef.current &&
            <Dialog onClose={handleClose} aria-labelledby="customized-dialog-title" open={open}>
                <MuiDialogTitle id="customized-dialog-title" onClose={handleClose}>
                    Server info
                    <Typography variant="body1">{serverRef.current.id}</Typography>
                </MuiDialogTitle>
                <MuiDialogContent dividers>
                    <TableContainer>
                        <Table className={classes.table} aria-label="simple table">
                            <TableBody>
                                <TableRow>
                                    <TableCell>
                                        WorkerPoolSize
                                    </TableCell>
                                    <TableCell>
                                        {serverRef.current.workerPoolSize}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        PollIntervalInSeconds
                                    </TableCell>
                                    <TableCell>
                                        {serverRef.current.pollIntervalInSeconds}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        FirstHeartbeat
                                    </TableCell>
                                    <TableCell>
                                        <TimeAgo date={new Date(serverRef.current.firstHeartbeat)}
                                                 title={new Date(serverRef.current.firstHeartbeat).toString()}/>
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        LastHeartbeat
                                    </TableCell>
                                    <TableCell>
                                        <TimeAgo date={new Date(serverRef.current.lastHeartbeat)}
                                                 title={new Date(serverRef.current.lastHeartbeat).toString()}/>
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        SystemTotalMemory
                                    </TableCell>
                                    <TableCell>
                                        {humanFileSize(serverRef.current.systemTotalMemory, true)}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        SystemFreeMemory
                                    </TableCell>
                                    <TableCell>
                                        {humanFileSize(serverRef.current.systemFreeMemory, true)}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        SystemCpuLoad
                                    </TableCell>
                                    <TableCell>
                                        {(serverRef.current.systemCpuLoad * 100).toFixed(2)} %
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        ProcessFreeMemory
                                    </TableCell>
                                    <TableCell>
                                        {humanFileSize(serverRef.current.processFreeMemory, true)}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        ProcessAllocatedMemory
                                    </TableCell>
                                    <TableCell>
                                        {humanFileSize(serverRef.current.processAllocatedMemory, true)}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        ProcessCpuLoad
                                    </TableCell>
                                    <TableCell>
                                        {(serverRef.current.processCpuLoad * 100).toFixed(2)} %
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </TableContainer>
                </MuiDialogContent>
            </Dialog>
            }
        </div>
    );
});

export default Servers;