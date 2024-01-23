import Alert from '@mui/material/Alert';
import SwitchableTimeAgo from "../../utils/time-ago";
import Icon from "@mui/material/Icon";
import Typography from "@mui/material/Typography";
import {Schedule} from "@mui/icons-material";
import {Check, Cogs, Delete, TimerSand} from "mdi-material-ui";
import {styled} from "@mui/material/styles";
import AccordionSummary from "@mui/material/AccordionSummary";
import ExpandMore from "@mui/icons-material/ExpandMore";
import AccordionDetails from "@mui/material/AccordionDetails";
import Accordion from "@mui/material/Accordion";

const COLOR_VARIANTS = {
    deleted: {
        color: '#ffe4bf',
        backgroundColor: '#654b3d',
        '& div.MuiAlert-icon': {
            color: '#e78f73',
            backgroundColor: '#654b3d',
        },
        '& div.MuiAlert-standardInfo': {
            color: '#ffe4bf',
            backgroundColor: '#654b3d',
        }
    },
    scheduled: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "#CFD8DC",
    },
    processing: {
        color: "rgb(102, 60, 0)",
        backgroundColor: "rgb(255, 244, 229)",
    },
    failed: {
        color: "rgb(97, 26, 21)",
        backgroundColor: "rgb(253, 236, 234)",
    },
    enqueued: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "rgb(232, 244, 253)",
    },
    success: {
        color: "rgb(30, 70, 32)",
        backgroundColor: "rgb(237, 247, 237)",
    }
}

const ICON_VARIANTS = {
    deleted: Delete,
    scheduled: Schedule,
    processing: Cogs,
    enqueued: TimerSand,
    success: Check
}

const ALERT_SEVERITY_VARIANTS = {
    deleted: "info",
    scheduled: "info",
    processing: "warning",
    enqueued: "info",
    success: "success",
    failed: "error"
}

const StyledAccordionSummary = styled(AccordionSummary,
    {shouldForwardProp: prop => prop !== 'state'}
)(({state}) => ({
    minHeight: 56,
    ...COLOR_VARIANTS[state]
}));

const JobStateHeading = ({state, title, date, canExpand = true}) => {
    const AlertIcon = ICON_VARIANTS[state];
    return (
        <StyledAccordionSummary state={state} expandIcon={canExpand ? <ExpandMore/> : <Icon/>}>
            <Alert style={{padding: 0, backgroundColor: "transparent"}}
                   severity={ALERT_SEVERITY_VARIANTS[state]}
                   icon={AlertIcon && <AlertIcon/>}
            >
                <Typography style={{textTransform: "none", lineHeight: "inherit"}} variant="h6">
                    {title}
                </Typography>
            </Alert>
            <Typography style={{alignSelf: 'center', marginLeft: 'auto'}}>
                <SwitchableTimeAgo date={new Date(date)}/>
            </Typography>
        </StyledAccordionSummary>
    )
}

export const JobState = ({children, onChange, expanded, removeDetailsPadding = false, ...rest}) => (
    <Accordion onChange={onChange} expanded={expanded}>
        <JobStateHeading {...rest} />
        {children && <AccordionDetails style={removeDetailsPadding ? {padding: 0} : {padding: "8px 16px 16px"}}>
            {children}
        </AccordionDetails>}
    </Accordion>
)