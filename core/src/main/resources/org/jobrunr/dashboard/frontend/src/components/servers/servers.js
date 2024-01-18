import { memo, useState, useEffect } from 'react';
import { styled, keyframes } from "@mui/material/styles";
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import TimeAgo from "react-timeago/lib";
import Box from "@mui/material/Box";
import {CogClockwise} from "mdi-material-ui";
import NotInterestedIcon from '@mui/icons-material/NotInterested';
import Dialog from '@mui/material/Dialog';
import MuiDialogTitle from '@mui/material/DialogTitle';
import MuiDialogContent from '@mui/material/DialogContent';
import {humanFileSize} from "../../utils/helper-functions";
import LoadingIndicator from "../LoadingIndicator";
import VersionFooter from "../utils/version-footer";

const spin = keyframes`
    from {
        transform: rotate(0deg)
    }
    to {
        transform: rotate(360deg)
    }
`;

const StyledCogClockwise = styled(CogClockwise)(() => ({
    animationName: spin,
    animationDuration: '5000ms',
    animationIterationCount: 'infinite',
    animationTimingFunction: 'linear'
}));

const classes = {
    table: {
        width: '100%',
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
        cursor: 'pointer'
    },
    nameColumn: {
        cursor: 'pointer'
    }
};

const Servers = memo(() => {
    const [isLoading, setIsLoading] = useState(true);
    const [servers, setServers] = useState([]);
    const [open, setOpen] = useState(false);
    const [currentServer, setCurrentServer] = useState(null);

    const handleOpen = (server) => {
        setCurrentServer(server);
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
        setCurrentServer(null);
    };


    useEffect(() => {
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
        return [...servers].sort((a, b) => a.firstHeartbeat > b.firstHeartbeat);
    }

    return (
        <div>
            <Box my={3}>
                <Typography variant="h4">Background Job Servers</Typography>
            </Box>
            {isLoading
                ? <LoadingIndicator/>
                : <>
                    <Paper style={classes.paper}>
                        {servers.length < 1
                            ? <Typography variant="body1" style={classes.noItemsFound}>No servers found</Typography>
                            : <>
                                <TableContainer>
                                    <Table style={classes.table} aria-label="servers overview">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell style={classes.idColumn}>Id</TableCell>
                                                <TableCell>Name</TableCell>
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
                                                    <TableCell component="th" scope="row" style={classes.idColumn}>
                                                        <Link onClick={() => handleOpen(server)} underline="hover">
                                                            {server.id}
                                                        </Link>
                                                    </TableCell>
                                                    <TableCell style={classes.nameColumn}>
                                                        <Link onClick={() => handleOpen(server)} underline="hover">
                                                            {server.name}
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
                                                            ? <StyledCogClockwise />
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
                    <VersionFooter/>
                </>
            }

            {currentServer &&
                <Dialog fullWidth maxWidth="sm" scroll="paper" onClose={handleClose} aria-labelledby="customized-dialog-title" open={open}>
                    <MuiDialogTitle id="customized-dialog-title" onClose={handleClose}>
                        Server info <code>{currentServer.id}</code>
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        <TableContainer>
                            <Table style={classes.table} aria-label="simple table">
                                <TableBody>
                                    <TableRow>
                                        <TableCell>
                                            Name
                                        </TableCell>
                                        <TableCell>
                                            {currentServer.name}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            WorkerPoolSize
                                        </TableCell>
                                        <TableCell>
                                            {currentServer.workerPoolSize}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            PollIntervalInSeconds
                                        </TableCell>
                                        <TableCell>
                                            {currentServer.pollIntervalInSeconds}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            FirstHeartbeat
                                        </TableCell>
                                        <TableCell>
                                            <TimeAgo date={new Date(currentServer.firstHeartbeat)}
                                                     title={new Date(currentServer.firstHeartbeat).toString()}/>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            LastHeartbeat
                                        </TableCell>
                                        <TableCell>
                                            <TimeAgo date={new Date(currentServer.lastHeartbeat)}
                                                     title={new Date(currentServer.lastHeartbeat).toString()}/>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            SystemTotalMemory
                                        </TableCell>
                                        <TableCell>
                                            {humanFileSize(currentServer.systemTotalMemory, true)}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            SystemFreeMemory
                                        </TableCell>
                                        <TableCell>
                                            {humanFileSize(currentServer.systemFreeMemory, true)}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            SystemCpuLoad
                                        </TableCell>
                                        <TableCell>
                                            {(currentServer.systemCpuLoad * 100).toFixed(2)} %
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            ProcessFreeMemory
                                        </TableCell>
                                        <TableCell>
                                            {humanFileSize(currentServer.processFreeMemory, true)}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            ProcessAllocatedMemory
                                        </TableCell>
                                        <TableCell>
                                            {humanFileSize(currentServer.processAllocatedMemory, true)}
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>
                                            ProcessCpuLoad
                                        </TableCell>
                                        <TableCell>
                                            {(currentServer.processCpuLoad * 100).toFixed(2)} %
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