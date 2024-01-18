import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import {TimerSand} from "mdi-material-ui";
import SwitchableTimeAgo from "../../utils/time-ago";
import {Icon} from "@mui/material";


const classes = {
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
    info: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "rgb(232, 244, 253)",
        minHeight: 56,
    }
};


const Enqueued = (props) => {
    const jobState = props.jobState;
    const enqueuedIcon = <TimerSand />

    return (
        <Accordion>
            <AccordionSummary
                style={classes.info}
                expandIcon={<Icon/>}
                id="enqueued-panel-header"
            >
                <Alert style={classes.alert} severity="info" icon={enqueuedIcon}>
                    <Typography style={classes.primaryHeading} variant="h6">
                        Job enqueued
                    </Typography>
                </Alert>
                <Typography style={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </AccordionSummary>
        </Accordion>
    )
};

export default Enqueued;