import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import ExpandMore from "@mui/icons-material/ExpandMore";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import makeStyles from '@mui/styles/makeStyles';
import {Schedule} from "@mui/icons-material";
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
        padding: 0,
        backgroundColor: "#CFD8DC"
    },
    scheduled: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "#CFD8DC",
        minHeight: 56
    }
}));


const Scheduled = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const scheduledIcon = <Schedule />;
    const scheduledDate = new Date(jobState.scheduledAt);
    const ScheduledMessage = () => {
        if (jobState.reason) {
            return <span>- {jobState.reason}</span>;
        }
        return <span></span>;
    };

    return (
        <Accordion>
            <AccordionSummary
                className={classes.scheduled}
                id="scheduled-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="scheduled-panel-content"
            >
                <Alert className={classes.alert} severity="info" icon={scheduledIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job scheduled <ScheduledMessage/>
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.scheduledAt)} />
                </Typography>
            </AccordionSummary>
            <AccordionDetails>
                Job scheduled at {scheduledDate.toString()}
            </AccordionDetails>
        </Accordion>
    )
};

export default Scheduled;