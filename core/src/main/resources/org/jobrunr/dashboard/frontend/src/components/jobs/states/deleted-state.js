import { styled } from "@mui/material/styles"
import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import Alert from '@mui/material/Alert';
import Typography from "@mui/material/Typography";
import {Delete} from "mdi-material-ui";
import SwitchableTimeAgo from "../../utils/time-ago";
import ExpandMore from "@mui/icons-material/ExpandMore";
import {Icon} from "@mui/material";

const StyledAccordionSummary = styled(AccordionSummary)({
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
})

const Deleted = (props) => {
    const jobState = props.jobState;
    const deletedIcon = <Delete/>

    return (
        <Accordion>
            <StyledAccordionSummary
                id="deleted-panel-header"
                expandIcon={jobState.reason
                    ? <ExpandMore />
                    : <Icon />
                }
            >
                <Alert style={{padding: 0}} severity="info" icon={deletedIcon}>
                    <Typography style={{textTransform: "none", lineHeight: "inherit"}} variant="h6">
                        Job deleted
                    </Typography>
                </Alert>
                <Typography style={{marginLeft: "auto", backgroundColor: "inherit", alignSelf: "center"}}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </StyledAccordionSummary>
            { jobState.reason &&
            <AccordionDetails>
                <div style={{padding: '24px 0 24px 24px'}}>{jobState.reason}</div>
            </AccordionDetails>
            }
        </Accordion>
    )
};

export default Deleted;