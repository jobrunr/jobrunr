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
import VersionFooter from "../utils/version-footer";
import Grid from "@material-ui/core/Grid";
import ButtonGroup from "@material-ui/core/ButtonGroup";
import Button from "@material-ui/core/Button";

const useStyles = makeStyles(theme => ({
    table: {
        width: '100%',
    },
    root: {
        width: '100%',
        backgroundColor: theme.palette.background.paper,
    },
    backgroundJobServerActions: {
        margin: '1rem',
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
    const [servers, setServers] = React.useState([]);
    const [currentServer, setCurrentServer] = React.useState(null);
    const [showProDialog, setShowProDialog] = React.useState(false);

    const handleOpen = (server) => {
        setCurrentServer(server);
    };

    const handleClose = () => {
        setCurrentServer(null);
        setShowProDialog(false);
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
                    <Paper className={classes.paper}>
                        {servers.length < 1
                            ? <Typography variant="body1" className={classes.noItemsFound}>No servers found</Typography>
                            : <>
                                <Grid item xs={12} container>
                                    <Grid item xs={6}>
                                        <ButtonGroup className={classes.backgroundJobServerActions}>
                                            <Button variant="outlined" color="primary" onClick={() => setShowProDialog(true)}>
                                                Pause all processing
                                            </Button>
                                            <Button variant="outlined" color="primary" onClick={() => setShowProDialog(true)}>
                                                Resume all processing
                                            </Button>
                                        </ButtonGroup>
                                    </Grid>
                                </Grid>
                                <TableContainer>
                                    <Table className={classes.table} aria-label="servers overview">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell className={classes.idColumn}>Id</TableCell>
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
                                                    <TableCell component="th" scope="row" className={classes.idColumn}>
                                                        <Link color="initial" onClick={() => handleOpen(server)}>
                                                            {server.id}
                                                        </Link>
                                                    </TableCell>
                                                    <TableCell className={classes.nameColumn}>
                                                        <Link color="initial" onClick={() => handleOpen(server)}>
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
                    <VersionFooter/>
                </>
            }

            {currentServer &&
                <Dialog fullWidth="true" maxWidth="sm" scroll="paper" onClose={handleClose} aria-labelledby="customized-dialog-title" open={true}>
                    <MuiDialogTitle id="customized-dialog-title" onClose={handleClose}>
                        Server info <code>{currentServer.id}</code>
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        <TableContainer>
                            <Table className={classes.table} aria-label="simple table">
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

            {showProDialog &&
                <Dialog fullWidth={true} maxWidth="sm" scroll="paper" onClose={handleClose} aria-labelledby="customized-dialog-title" open={true}>
                    <MuiDialogTitle id="customized-dialog-title" onClose={handleClose}>
                        Get JobRunr Pro now!
                    </MuiDialogTitle>
                    <MuiDialogContent dividers>
                        Do you need to pause background job processing in JobRunr? Upgrade to JobRunr Pro today and unlock this powerful feature, along with a
                        host of other advanced capabilities to take your job management to the next level.<br/><br/>
                        <i>Unleash the full potential of your job processing</i> - upgrade to <a href={'https://www.jobrunr.io/en/get-jobrunr-pro/'}
                                                                                                 rel={'noreferrer'}
                                                                                                 target={'_blank'}>JobRunr Pro</a>!
                    </MuiDialogContent>
                </Dialog>
            }
        </div>
    );
});

export default Servers;