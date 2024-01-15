import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import ExpandMore from "@mui/icons-material/ExpandMore";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import makeStyles from '@mui/styles/makeStyles';
import {Check} from "mdi-material-ui";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import SwitchableTimeAgo from "../../utils/time-ago";

const useStyles = makeStyles(theme => ({
    primaryHeading: {
        textTransform: "none",
        lineHeight: "inherit"
    },
    secondaryHeading: {
        alignSelf: 'center',
        marginLeft: 'auto'
    },
    alert: {
        padding: 0
    },
    success: {
        color: "rgb(30, 70, 32)",
        backgroundColor: "rgb(237, 247, 237)",
        minHeight: 56
    }
}));

const getDuration = (duration) => {
    try {
        const actualDuration = duration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(duration) : duration;
        const totalSeconds = actualDuration.toFixed(2);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds - (hours * 3600)) / 60);
        const seconds = totalSeconds - (hours * 3600) - (minutes * 60);

        let result = "";
        if (hours > 0) {
            result += hours + " hours, "
        }
        if (minutes > 0) {
            result += minutes + ((minutes > 1) ? " minutes" : " minute")
        }
        if (minutes > 0 && seconds > 0) {
            result += " and "
        }
        if (seconds > 0) {
            result += seconds.toFixed(2) + " seconds"
        } else if (result === '') {
            result += "less than 10ms"
        }
        return result;
    } catch (e) {
        console.warn("Could not parse " + duration + ". If you want pretty dates in the succeeded view, your durations must be formatted as either seconds or ISO8601 duration format (e.g. PT5M33S). This is a settings in Jackson.");
        return duration + " (unsupported duration format - see console)";
    }
}

const Succeeded = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const checkIcon = <Check/>

    return (
        <Accordion>
            <AccordionSummary
                className={classes.success}
                id="succeeded-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="succeeded-panel-content"
            >
                <Alert className={classes.alert} severity="success" icon={checkIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job processing succeeded
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)}/>
                </Typography>
            </AccordionSummary>
            <AccordionDetails>
                <ul>
                    <li>Latency duration: {getDuration(jobState.latencyDuration)}</li>
                    <li>Process duration: {getDuration(jobState.processDuration)}</li>
                </ul>
            </AccordionDetails>
        </Accordion>
    )
};

export default Succeeded;