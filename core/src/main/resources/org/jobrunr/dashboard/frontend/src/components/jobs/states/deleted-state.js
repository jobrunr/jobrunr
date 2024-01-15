import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import makeStyles from '@mui/styles/makeStyles';
import {Delete} from "mdi-material-ui";
import SwitchableTimeAgo from "../../utils/time-ago";
import ExpandMore from "@mui/icons-material/ExpandMore";
import {Icon} from "@mui/material";


const useStyles = makeStyles(() => ({
    primaryHeading: {
        textTransform: "none",
        lineHeight: "inherit"
    },
    secondaryHeading: {
        alignSelf: 'center',
        marginLeft: 'auto',
        backgroundColor: 'inherit'
    },
    details: {
        padding: '24px 0 24px 24px'
    },
    alert: {
        padding: 0
    },
    info: {
        color: '#ffe4bf',
        backgroundColor: '#654b3d',
        minHeight: 56,
        '& div.MuiAlert-icon': {
            color: '#e78f73',
            backgroundColor: '#654b3d',
        },
        '& div.MuiAlert-standardInfo': {
            color: '#ffe4bf',
            backgroundColor: '#654b3d',
        }
    }
}));


const Deleted = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const deletedIcon = <Delete/>

    return (
        <Accordion>
            <AccordionSummary
                className={classes.info}
                id="deleted-panel-header"
                expandIcon={jobState.reason
                    ? <ExpandMore />
                    : <Icon />
                }
            >
                <Alert className={classes.alert} severity="info" icon={deletedIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job deleted
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </AccordionSummary>
            { jobState.reason &&
            <AccordionDetails className={classes.expansionPanel}>
                <div className={classes.details}>{jobState.reason}</div>
            </AccordionDetails>
            }
        </Accordion>
    )
};

export default Deleted;