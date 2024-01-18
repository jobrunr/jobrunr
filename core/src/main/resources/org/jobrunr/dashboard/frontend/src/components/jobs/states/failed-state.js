import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import ExpandMore from "@mui/icons-material/ExpandMore";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import SwitchableTimeAgo from "../../utils/time-ago";

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
    failed: {
        color: "rgb(97, 26, 21)",
        backgroundColor: "rgb(253, 236, 234)",
        minHeight: 56
    },
    expansionPanel: {
        display: "block"
    },
    stackTrace: {
        whiteSpace: "pre-wrap",
        wordWrap: "break-word",
        fontFamily: "monospace",
        fontSize: "1em"
    }
};


const Failed = (props) => {
    const jobState = props.jobState;

    return (
        <Accordion className={classes.root}>
            <AccordionSummary
                style={classes.failed}
                id="failed-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="failed-panel-content"
            >
                <Alert style={classes.alert} severity="error">
                    <Typography style={classes.primaryHeading} variant="h6">
                        Job processing failed - {jobState.message}
                    </Typography>
                </Alert>
                <Typography style={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </AccordionSummary>

            <AccordionDetails style={classes.expansionPanel}>
                <Typography variant="h6" className={classes.exceptionClass} gutterBottom>
                    {jobState.exceptionType}
                </Typography>
                <Typography component={'pre'} style={classes.stackTrace}>
                    {jobState.stackTrace}
                </Typography>
                { jobState.message.includes("not found") &&
                    <Typography style={{marginTop: '1em'}}>
                        With <a href="https://www.jobrunr.io/en/documentation/pro/" target="_blank" rel="noreferrer">JobRunr Pro</a> you can test during your build pipeline if the JobParameters of your production environment are still deserializable and use <a href="https://www.jobrunr.io/en/documentation/pro/migrations/" target="_blank" rel="noreferrer">Job Migrations</a> to keep your jobs up & running at all times.
                        Fail fast, gain time and enjoy a hassle free development workflow.
                    </Typography>
                }
            </AccordionDetails>
        </Accordion>
    )
};

export default Failed;